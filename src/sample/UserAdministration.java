package sample;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

class UserAdministration {

    private final String filename = System.getProperty("user.home") + "/cryptor/users.cryptor";
    private NavigableMap<Integer, User> users;

    UserAdministration() {
        users = new TreeMap<>();

        try {
            readUsers();
            usersToString();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void usersToString() {
        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            String key = entry.getKey().toString();
            User value = entry.getValue();

            System.out.println("Map-Key/ID: " + key + "/" + value.getId()
                    + "\nName: " + value.getName()
                    + "\nPrivKey: " + Arrays.toString(value.getMyPrivKey())
                    + "\nPubKey; " + Arrays.toString(value.getMyPublicKey())
                    + "\nSecret: " + Arrays.toString(value.getSharedSecret())
                    + "\n"
            );
        }
    }

    ArrayList<User> getUsers() {
        ArrayList<User> userList = new ArrayList<>();

        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            userList.add(entry.getValue());
        }

        return userList;
    }

    private int generateNewID() {
        if (users.isEmpty()) {
            return 0;
        } else {
            Map.Entry<Integer, User> lastEntry = users.lastEntry();
            return lastEntry.getValue().getId() + 1;
        }
    }

    User createUser(String name) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        byte[][] alice = DiffieHellman.alice();
        int id = generateNewID();

        User user = new User(id, name, alice[0], alice[1], new byte[1]);
        users.put(id, user);
        saveUsers();

        return user;
    }

    User createUser(String name, byte[] publicKeyEnc) throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        byte[][] bob = DiffieHellman.bob(publicKeyEnc);
        int id = generateNewID();

        User user =  new User(id, name, bob[0], bob[1], bob[2]);
        users.put(id, user);
        saveUsers();

        return user;
    }

    void finishSetup(int id, byte[] publicKeyEnc) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, IOException {
        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            if (entry.getValue().getId() == id) {
                entry.getValue().setSharedSecret(DiffieHellman.aliceComplete(entry.getValue().getMyPrivKey(), publicKeyEnc));
                break;
            }
        }

        saveUsers();
    }

    void deleteUser(int id) throws IOException {
        for (Map.Entry<Integer, User> entry : users.entrySet()) {

            if (entry.getValue().getId() == id) {
                users.remove(entry.getKey());
                break;
            }
        }

        saveUsers();
    }

    private void readUsers() throws IOException{
        File file = new File(filename);

        if (file.exists()) {
            byte[] encrypted = Files.readAllBytes(Paths.get(filename));

            byte[] encoded = AES.decrypt(encrypted, new byte[] {
                    (byte)0xe0, 0x4f,
                    (byte)0xd0, 0x20,
                    (byte)0xea, 0x3a, 0x69, 0x10,
                    (byte)0xa2,
                    (byte)0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30,
                    (byte)0x9d
            });

            String encodedUsers = null;
            if (encoded != null) {
                encodedUsers = new String(encoded, "ISO-8859-1");
            }

            String[] separatedUsers = new String[0];
            if (encodedUsers != null) {
                separatedUsers = encodedUsers.split(":::");
            }

            for (String userString: separatedUsers) {
                String[] attributes = userString.split("---");
                if (attributes.length == 5) {
                    this.users.put(Integer.parseInt(attributes[0]),
                            new User(Integer.parseInt(attributes[0]),
                                    attributes[1],
                                    attributes[2].getBytes(Charset.forName("ISO-8859-1")),
                                    attributes[3].getBytes(Charset.forName("ISO-8859-1")),
                                    attributes[4].getBytes(Charset.forName("ISO-8859-1"))
                            )
                    );
                }
            }
        }
    }

    private void saveUsers() throws IOException {
        StringBuilder encodedUsers = new StringBuilder();

        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            User user = entry.getValue();

            encodedUsers
                    .append(user.getId())
                    .append("---")
                    .append(user.getName())
                    .append("---")
                    .append(new String(user.getMyPrivKey(), "ISO-8859-1"))
                    .append("---")
                    .append(new String(user.getMyPublicKey(), "ISO-8859-1"))
                    .append("---")
                    .append(new String(user.getSharedSecret(), "ISO-8859-1"))
                    .append(":::");
        }

        byte[] encryptedUsers = AES.encrypt(encodedUsers.toString().getBytes(Charset.forName("ISO-8859-1")), new byte[] {
                (byte)0xe0, 0x4f,
                (byte)0xd0, 0x20,
                (byte)0xea, 0x3a, 0x69, 0x10,
                (byte)0xa2,
                (byte)0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30,
                (byte)0x9d
        });

        FileOutputStream fos = new FileOutputStream(filename);

        if (encryptedUsers != null) {
            fos.write(encryptedUsers);
        }

        fos.flush();
        fos.close();
    }
}

