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

    private NavigableMap<Integer, User> users;
    private String filename = "Users.txt";

    UserAdministration() {
        users = new TreeMap<>();

        try {
            readUsers();
            printUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int generateNewID() {
        if (users.isEmpty()) {
            return 0;
        } else {
            Map.Entry<Integer, User> lastEntry = users.lastEntry();
            return lastEntry.getValue().getId() + 1;
        }
    }

    private void printUsers() {
        for (Map.Entry<Integer, User> entry : this.users.entrySet()) {
            String key = entry.getKey().toString();
            User value = entry.getValue();

            System.out.println("Key: " + key
                    + "\nName: " + value.getName()
                    + "\nPrivKey: " + Arrays.toString(value.getMyPrivKey())
                    + "\nPubKey; " + Arrays.toString(value.getMyPublicKey())
                    + "\nSecret: " + Arrays.toString(value.getSharedSecret())
                    + "\n"
            );
        }
    }

    void createUser(String name) throws InvalidKeyException, NoSuchAlgorithmException, IOException {
        byte[][] alice = DiffieHellman.alice();
        int id = generateNewID();

        users.put(id, new User(id, name, alice[0], alice[1], new byte[1]));
        saveUsers();
    }

    void createUser(String name, String publicKey) throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, IOException {
        // TODO: Input Key als Key-File
        byte[][] bob = DiffieHellman.bob(publicKey.getBytes(Charset.forName("UTF-8")));
        int id = generateNewID();

        users.put(id, new User(id, name, bob[0], bob[1], bob[2]));
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
                encodedUsers = new String(encoded, "UTF-8");
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
                                    attributes[2].getBytes(Charset.forName("UTF-8")),
                                    attributes[3].getBytes(Charset.forName("UTF-8")),
                                    attributes[4].getBytes(Charset.forName("UTF-8"))
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
                    .append(new String(user.getMyPrivKey(), "UTF-8"))
                    .append("---")
                    .append(new String(user.getMyPublicKey(), "UTF-8"))
                    .append("---")
                    .append(new String(user.getSharedSecret(), "UTF-8"))
                    .append(":::");
        }

        byte[] encryptedUsers = AES.encrypt(encodedUsers.toString().getBytes(Charset.forName("UTF-8")), new byte[] {
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

    public String[][] getIdAndName() {
        String[][] idAndName = new String[users.size()][2];
        int i = 0;

        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            User user = entry.getValue();

            idAndName[i][0] = Integer.toString(user.getId());
            idAndName[i][1] = user.getName();

            i++;
        }

        return idAndName;
    }
}

