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

    private final String USERS_FILE = System.getProperty("user.home") + "/cryptor/users.cryptor";
    private final byte[] CRYPTOR_AES_SECRET = new byte[]{
            (byte)0xf1,
            (byte)0xf0, 0x0b,
            (byte)0xcf, 0x78, 0x2f, 0x61,
            (byte)0x88, 0x66, 0x6f,
            (byte)0x93,
            (byte)0x82,
            (byte)0xba,
            (byte)0xdb, 0x55, 0x42,
            (byte)0xbe, 0x53,
            (byte)0xee, 0x50, 0x4b, 0x2a, 0x37, 0x67,
            (byte)0xbc, 0x1f, 0x76, 0x44,
            (byte)0xfe,
            (byte)0x95, 0x66,
            (byte)0xdb
    };

    private NavigableMap<Integer, User> users;

    UserAdministration() {
        users = new TreeMap<>();

        try {
            readUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
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
        File file = new File(USERS_FILE);

        if (file.exists()) {
            byte[] encrypted = Files.readAllBytes(Paths.get(USERS_FILE));
            byte[] encoded = AES.decrypt(encrypted, CRYPTOR_AES_SECRET);

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

        byte[] encryptedUsers = AES.encrypt(encodedUsers.toString().getBytes(Charset.forName("ISO-8859-1")), CRYPTOR_AES_SECRET);

        FileOutputStream fos = new FileOutputStream(USERS_FILE);
        if (encryptedUsers != null) {
            fos.write(encryptedUsers);
        }

        fos.flush();
        fos.close();
    }
}

