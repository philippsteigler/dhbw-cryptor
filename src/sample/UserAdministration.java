package sample;

import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class UserAdministration {

    ArrayList<User> users;

    public UserAdministration() {
        users = new ArrayList<>();
    }

    public void setUsers() throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream = new FileInputStream("Test.txt");
        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
        this.users = (ArrayList<User>) (objectInputStream.readObject());
        objectInputStream.close();

        System.out.println(this.users);
    }

    public void createUser(int id, String name, String key) {
        MessageDigest sha;
        try {
            byte[] shaKey = key.getBytes("UTF-8");
            sha = MessageDigest.getInstance("SHA-1");
            users.add(new User(id, name, sha.digest(shaKey)));
        }
        catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void saveUsers() throws IOException {
        //TODO save Users

        String usersString = "";

        for (User user: users) {
            usersString = usersString + "(" + Integer.toString(user.getId()) + ", " + user.getName() + ", " + ByteBuffer.wrap(user.getKey()) + ")";
        }

        PrintWriter out = new PrintWriter("filename.txt");
        out.println(usersString);
    }
}
