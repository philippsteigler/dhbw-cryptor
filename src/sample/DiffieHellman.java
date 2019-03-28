package sample;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

/*
 * Klasse zum sicheren Austausch von symmetrischen Keys zur Ver- und Entschlüsselung.
 *
 * Hierfür wird das bekannte Diffie-Hellman-Key-Exchange (DHKE) Protokoll verwendet. Bei diesem werden über einen unsicheren
 * Kanal öffentliche Informationen ausgetauscht, aus denen anschließend von beiden Kommunikationspartnern ein
 * gemeinsames Geheimnis berechnet wird.
 *
 * Dieses gemeinsame Geheimnis wird später für die Erzeugung von symmetrischen Schlüsseln für AES verwendet.
 */
class DiffieHellman {

    /*
     * Diese Methode initialisiert den DHKE.
     */
    static byte[][] alice() throws NoSuchAlgorithmException, InvalidKeyException {
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);

        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        byte[] alicePrivKeyEnc = aliceKpair.getPrivate().getEncoded();
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();

        return new byte[][]{alicePrivKeyEnc, alicePubKeyEnc};
    }

    static byte[] aliceComplete(byte[] alicePrivKeyEnc, byte[] bobPubKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        KeyFactory alicePrivKeyFac = KeyFactory.getInstance("DH");
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(alicePrivKeyEnc);
        PrivateKey alicePrivKey = alicePrivKeyFac.generatePrivate(pkcs8KeySpec);

        KeyFactory alicePubKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
        PublicKey bobPubKey = alicePubKeyFac.generatePublic(x509KeySpec);

        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(alicePrivKey);
        aliceKeyAgree.doPhase(bobPubKey, true);

        return aliceKeyAgree.generateSecret();
    }

    static byte[][] bob(byte[] alicePubKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);

        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);
        DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey) alicePubKey).getParams();

        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamFromAlicePubKey);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();

        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
        bobKeyAgree.init(bobKpair.getPrivate());

        byte[] bobPrivKeyEnc = bobKpair.getPrivate().getEncoded();
        byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();

        bobKeyAgree.doPhase(alicePubKey, true);

        return new byte[][]{bobPrivKeyEnc, bobPubKeyEnc, bobKeyAgree.generateSecret()};
    }
}
