package sample;

public class User {

    private int id;

    private String name;
    private String email;

    private byte[] privateKey;
    private byte[] sharedKey;

    public User(int id, String name, byte[] privateKey) {
        this.id = id;
        this.name = name;
        this.privateKey = new byte[privateKey.length];
        this.privateKey = privateKey;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getKey() {
        return key;
    }
}
