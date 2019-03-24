package sample;

import org.omg.IOP.Encoding;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class UserAdministration {

    ArrayList<User> users;

    String filename = "Users.txt";

    public UserAdministration() {
        users = new ArrayList<>();
        try {
            setUsers();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setUsers() throws IOException{
        byte[] encoded = Files.readAllBytes(Paths.get("Users.txt"));
        String usersString =  new String(encoded, "UTF-8");

        String[] userStrings = usersString.split("/");

        for (String userString: userStrings) {
            userString.trim();
            String[] attributes = userString.split(",");
            if(attributes.length == 3) {
                users.add(new User(Integer.parseInt(attributes[0]), attributes[1], attributes[2].getBytes(Charset.forName("UTF-8"))));
            }
        }
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
            usersString = usersString + Integer.toString(user.getId()) + "," + user.getName() + "," + user.getKey() + "/";
            usersString.trim();
        }

        PrintWriter printWriter = new PrintWriter(filename);
        printWriter.write(usersString);
        printWriter.close();
        printWriter.flush();

        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        fileOutputStream.write(usersString.getBytes());
        fileOutputStream.close();
    }
}
