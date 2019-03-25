package sample;

public class User {

    private int id;
    private String name;
    private byte[] myPrivKey;
    private byte[] myPublicKey;
    private byte[] sharedSecret;

    public User(int id, String name, byte[] key) {
        this.id = id;
        this.name = name;
        this.sharedSecret = key;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public byte[] getKey() {
        return sharedSecret;
    }
}
