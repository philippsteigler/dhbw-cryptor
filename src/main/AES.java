package main;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Klasse zur Ver- und Entschlüsselung von beliebigen Byte-Strömen, wie beispielsweise ein Dokument als Bytes.
 * Als sicheres, etabliertes Verfahren wird hier der symmetrische AES-256 Algorithmus verwendet.
 *
 * Der Schlüssel wird dafür vom gemeinsamen Shared-Secret beider Kommunikationspartner abgeleitet, welches ebenfalls
 * an die Methoden übergeben wird.
 */
class AES {

    /**
     * Methode zur Verschlüsselung eines beliebigen Byte-Arrays, wie beispielsweise ein Dokument als Bytes.
     * Diese Funktion erhält den Klartext und das Shared-Secret als Byte-Arrays und liefert den Chiffretext.
     *
     * @param clearBytes Klartext, als Byte-Array codiert.
     * @param secret Gemeinsames Geheimnis zwischen Alice und Bob, von dem der AES-Key abgeleitet wird.
     * @return Chiffretext, als Byte-Array codiert.
     */
    static byte[] encrypt(byte[] clearBytes, byte[] secret) {
        try {
            // Erzeuge eine Cipher-Instanz vom Typ AES im ECB-Modus und initialisiere diese mit dem Shared-Secret.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            IvParameterSpec initVector = new IvParameterSpec(secret, 16, 16);
            cipher.init(Cipher.ENCRYPT_MODE,  new SecretKeySpec(secret, secret.length - 32, 16, "AES"), initVector);

            // Führe die Verschlüsselung mit der Cipher-Instanz durch.
            return cipher.doFinal(clearBytes);
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }

        return null;
    }

    /**
     * Methode zur Entschlüsselung eines beliebigen Byte-Arrays, wie beispielsweise ein Dokument als Bytes.
     * Diese Funktion erhält den Chiffretext und das Shared-Secret als Byte-Arrays und liefert den Klartext,
     * sofern der richtige Schlüssel verwendet wurde.
     *
     * @param chiffreBytes Chiffretext, als Byte-Array codiert.
     * @param secret Gemeinsames Geheimnis zwischen Alice und Bob, von dem der AES-Key abgeleitet wird.
     * @return Klartext, als Byte-Array codiert.
     */
    static byte[] decrypt(byte[] chiffreBytes, byte[] secret) {
        try {
            // Erzeuge eine Cipher-Instanz vom Typ AES im ECB-Modus und initialisiere diese mit dem Shared-Secret.
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            IvParameterSpec initVector = new IvParameterSpec(secret, 16, 16);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, secret.length - 32, 16, "AES"), initVector);

            // Führe die Entschlüsselung mit der Cipher-Instanz durch.
            return cipher.doFinal(chiffreBytes);
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }

        return null;
    }
}