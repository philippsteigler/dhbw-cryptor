package sample;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

class AES {

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