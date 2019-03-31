package main.users;

public class User {

    private int id;
    private String name;
    private byte[] myPrivKey;
    private byte[] myPubKey;
    private byte[] sharedSecret;

    public User(int id, String name, byte[] myPrivKey, byte[] myPublicKey, byte[] sharedSecret) {
        this.id = id;
        this.name = name;
        this.myPrivKey = myPrivKey;
        this.myPubKey = myPublicKey;
        this.sharedSecret = sharedSecret;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    byte[] getMyPrivKey() {
        return myPrivKey;
    }

    public byte[] getMyPublicKey() {
        return myPubKey;
    }

    public byte[] getSharedSecret() {
        return sharedSecret;
    }

    void setSharedSecret(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}
