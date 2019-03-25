package sample;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;

public class UserAdministration {

    ArrayList<User> users;

    private String filename = "Users.txt";

    public UserAdministration() {
        users = new ArrayList<>();
        try {
            readUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        for (User user: users) {
            System.out.println("name: " + user.getName());
            //System.out.println("key: " + Arrays.toString(user.getKey()));
        }
    }

    public void createUser(String name) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[][] alice = DiffieHellman.alice();
        users.add(new User(1, name, alice[0], alice[1]));
    }

    public void createUser(String name, String publicKey) throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException {
        // TODO: Input Key String abfangen
        byte[][] bob = DiffieHellman.bob(publicKey.getBytes(Charset.forName("UTF-8")));
        User user = new User(1, name, bob[0], bob[1]);
        user.setSharedSecret(bob[2]);
        users.add(user);
    }

    public void readUsers() throws IOException{
        File file = new File(filename);

        if (file.exists()) {
            byte[] encoded = Files.readAllBytes(Paths.get(filename));

            String usersString =  new String(encoded, "UTF-8");

            String[] userStrings = usersString.split(";");

            for (String userString: userStrings) {
                String[] attributes = userString.split("/");
                if(attributes.length == 3) {
                    //users.add(new User(Integer.parseInt(attributes[0]), attributes[1], attributes[2].getBytes(Charset.forName("UTF-8"))));
                }
            }
        }
    }

    public void saveUsers() throws IOException {
        // TODO: save Users

        String usersString = "";

        for (User user: users) {
            //usersString = usersString + Integer.toString(user.getId()) + "/" + user.getName() + "/" + Arrays.toString(user.getKey()) + ";";
            usersString.trim();
        }

        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        fileOutputStream.write(usersString.getBytes());
        fileOutputStream.flush();
        fileOutputStream.close();
    }

    public ArrayList<String> getObservableUserNames() {
        ArrayList<String> userNames = new ArrayList<>();

        for (User user: users) {
            userNames.add(user.getName());
        }

        return userNames;
    }
}
