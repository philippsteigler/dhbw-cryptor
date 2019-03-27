package sample;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/*
 * Klasse zur Ver- und Entschlüsselung von beliebigen Byte-Strömen, wie beispielsweise ein Dokument als Bytes.
 * Als sicheres, etabliertes Verfahren wird hier der symmetrische AES-256 Algorithmus verwendet.
 *
 * Der Schlüssel wird dafür vom gemeinsamen Shared-Secret beider Kommunikationspartner abgeleitet, welches jeweils
 * an die Methoden übergeben wird.
 */
class AES {

    /*
     * Methode zur Verschlüsselung eines beliebigen Byte-Arrays, wie beispielsweise ein Dokument als Bytes.
     * Diese Funktion erhält den Klartext und das Shared-Secret als Byte-Arrays und liefert den Chiffretext.
     */
    static byte[] encrypt(byte[] inputBytes, byte[] secret) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE,  new SecretKeySpec(secret, 0, 16, "AES"));
            return cipher.doFinal(inputBytes);
        } catch (Exception e) {
            System.out.println("Error while encrypting: " + e.toString());
        }

        return null;
    }

    /*
     * Methode zur Entschlüsselung eines beliebigen Byte-Arrays, wie beispielsweise ein Dokument als Bytes.
     * Diese Funktion erhält den Chiffretext und das Shared-Secret als Byte-Arrays und liefert den Klartext,
     * sofern der richtige Schlüssel verwendet wurde.
     */
    static byte[] decrypt(byte[] toDecrypt, byte[] secret) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret, 0, 16, "AES"));
            return cipher.doFinal(toDecrypt);
        } catch (Exception e) {
            System.out.println("Error while decrypting: " + e.toString());
        }

        return null;
    }
}