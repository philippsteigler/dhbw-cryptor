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

/**
 * Klasse zur Verwaltung der Kontakte, mit denen Ver- und Entschlüsselt wird.
 */
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


    // Generiere eine eindeutige ID an der Kontakte identifiziert werden können.
    private int generateNewID() {
        if (users.isEmpty()) {
            return 0;
        } else {
            // Die neue ID entspricht der höchsten aktuellen ID+1.
            // Kontakte sind in der Map aufsteigend sortiert, sodass der letzte Eintrag die höchste ID hat.
            Map.Entry<Integer, User> lastEntry = users.lastEntry();
            return lastEntry.getValue().getId() + 1;
        }
    }

    // Erstellt einen neuen Kontakt.
    // Diese Methode wird von Alice (A) verwendet, da sie den Key-Exchange initialisiert und zu diesem Zeitpunkt keinen
    // Public-Key von Bob (B) zur Verfügung hat.
    User createUser(String name) throws NoSuchAlgorithmException, IOException {

        // Generiere ein Private-Public-Key Pair für die Kommunikation mit diesem Kontakt.
        byte[][] alice = DiffieHellman.alice();
        int id = generateNewID();

        // Der neue Kontakt besitzt eine ID, Namen, Private-Key und Public Key. Das Shared-Secret bleibt leer und wird
        // Später berechnet.
        User user = new User(id, name, alice[0], alice[1], new byte[1]);
        users.put(id, user);

        saveUsers();

        return user;
    }

    // Erstellt einen neuen Kontakt unter Verwendung eines Public-Keys.
    // Diese Methode wird von Bob (B) verwendet, der Alices' Public-Key zur Erzeugung korrespondieren Keys nutzt.
    User createUser(String name, byte[] publicKeyEnc) throws InvalidKeySpecException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, InvalidKeyException, IOException {

        // Generiere ein Private-Public-Key Pair + Shared-Secret für die Kommunikation mit diesem Kontakt.
        byte[][] bob = DiffieHellman.bob(publicKeyEnc);
        int id = generateNewID();

        // Der neue Kontakt besitzt eine ID, Namen, Private-Key, Public Key und Shared-Secret.
        User user =  new User(id, name, bob[0], bob[1], bob[2]);
        users.put(id, user);

        saveUsers();

        return user;
    }

    // Mit dem Public-Key von Bob wird der Key-Exchange abgeschlossen.
    void finishSetup(int id, byte[] publicKeyEnc) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, IOException {

        // Über die mitgelieferte ID wird Alice ermittelt und anschließend mit ihrem Private-Key und Bob's Public-Key,
        // um auf ihrer Seite das Shared-Secret zu berechnen.
        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            if (entry.getValue().getId() == id) {
                entry.getValue().setSharedSecret(DiffieHellman.aliceComplete(entry.getValue().getMyPrivKey(), publicKeyEnc));
                break;
            }
        }

        saveUsers();
    }

    // Löschen eines Kontaktes.
    void deleteUser(int id) throws IOException {
        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            if (entry.getValue().getId() == id) {
                users.remove(entry.getKey());
                break;
            }
        }

        saveUsers();
    }

    // Speichern der Kontakte in eine verschlüsselte Datei. Die User-Objekte werden als String codiert und in eine Datei
    // geschrieben. Der notwendige Key ist final definiert.
    private void saveUsers() throws IOException {
        StringBuilder encodedUsers = new StringBuilder();

        // Für alle User-Objekte in der User-Map..
        for (Map.Entry<Integer, User> entry : users.entrySet()) {
            User user = entry.getValue();

            // Codiere einen User als String und trenne seine Attribute mit ---. anze User werden mit ::: separiert.
            // --> ID, Name, Private-Key, Public-Key und Shared-Secret
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

        // Der codierte User-String wird mit AES verschlüsselt und ins Dateisystem geschrieben.
        byte[] encryptedUsers = AES.encrypt(encodedUsers.toString().getBytes(Charset.forName("ISO-8859-1")), CRYPTOR_AES_SECRET);

        FileOutputStream fos = new FileOutputStream(USERS_FILE);
        if (encryptedUsers != null) {
            fos.write(encryptedUsers);
        }

        fos.flush();
        fos.close();
    }

    // Einlesen der verschlüsselten Kontaktdatei und laden der Kontakte in die interne User-Map.
    // Der notwendige Key ist final definiert.
    private void readUsers() throws IOException{
        File file = new File(USERS_FILE);

        if (file.exists()) {
            // Verschlüsselte Datei einlesen und entschlüsseln.
            byte[] encrypted = Files.readAllBytes(Paths.get(USERS_FILE));
            byte[] encoded = AES.decrypt(encrypted, CRYPTOR_AES_SECRET);

            String encodedUsers = null;
            if (encoded != null) {
                encodedUsers = new String(encoded, "ISO-8859-1");
            }

            // Trenne den gesamten String in alle User-Objekte.
            String[] separatedUsers = new String[0];
            if (encodedUsers != null) {
                separatedUsers = encodedUsers.split(":::");
            }

            // Trenne alle Attribute jedes Users und erzeuge neue User-Objekte, die auf der User-Map abgelegt werden.
            // --> 0=ID, 1=Name, 2=Private-Key, 3=Public-Key, 4=Shared-Secret
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
}

