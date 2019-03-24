package sample;

public class User {

    private int id;

    private String name;
    private String email;

    private byte[] key;

    public User(int id, String name, byte[] key) {
        this.id = id;
        this.name = name;
        this.key = key;
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
