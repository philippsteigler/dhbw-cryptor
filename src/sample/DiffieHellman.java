package sample;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.crypto.interfaces.*;

/**
 * Klasse zum sicheren Austausch von symmetrischen Schlüsseln zur Ver- und Entschlüsselung.
 *
 * Hierfür wird das bekannte Diffie-Hellman-Key-Exchange (DHKE) Protokoll verwendet. Bei diesem werden über einen
 * unsicheren Kanal öffentliche Informationen ausgetauscht, aus denen anschließend von beiden Kommunikationspartnern ein
 * gemeinsames Geheimnis berechnet wird. Dieses gemeinsame Geheimnis wird später für die Erzeugung von symmetrischen
 * Schlüsseln für AES verwendet.
 *
 * Das Prinzip dahinter besteht darin, dass Alice (A) den Schlüsselaustausch startet und ein Private-Public-Key-Pair
 * erzeugt. Alice übermittelt ihren Public-Key an Bob (B). Dieser erzeugt auf der anderen Seite ebenfalls ein
 * Private-Public-Key-Pair, allerdings unter Verwendung von Alices' Public-Key, da er die Parameter von Alice mit
 * einbeziehen muss. Somit können beide Kommunikationspartner abschließend ein gemeinsames Geheimnis berechnen, aus dem
 * später der AES-Key abgeleitet wird.
 */
class DiffieHellman {

    /**
     * Diese Methode initialisiert den DHKE. Es wird ein Private-Public-Key-Pair für den DHKE erzeugt und übermittelt.
     *
     * @return Menge aus Alices' Public- und Private-Key.
     */
    static byte[][] alice() throws NoSuchAlgorithmException {

        // Erzeuge einen Schlüsselgenerator für Alice im DH-Modus und initialisiere diesen.
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);

        // Erzeuge ein Private-Public-Key-Pair
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        // Konvertiere die Keys zum Speichern in Byte-Arrys und gib diese zurück.
        byte[] alicePrivKeyEnc = aliceKpair.getPrivate().getEncoded();
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();

        return new byte[][]{alicePrivKeyEnc, alicePubKeyEnc};
    }

    /**
     * Zweiter Schritt im DHKE. Bob erhält den Public-Key von Alice und erzeugt ein davon abhängiges Private-Public-
     * Key-Pair. Von diesem kann er direkt das Shared-Secret auf seiner Seite berechnen und damit mit AES verschlüsseln.
     *
     * @param alicePubKeyEnc Public-Key von Alice, als Byte-Array codiert.
     * @return Menge aus Bob's Public-Key, Private-Key und Shared-Secret.
     */
    static byte[][] bob(byte[] alicePubKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {

        // Erzeuge eine Key-Factory und Key-Specs, um den codierten Public-Key von Alice in einen Schlüssel umzuwandeln.
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);
        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

        // Extrahiere die Diffie-Hellman-Parameter, die von Alices' Public-Key abstammen.
        // So wird sichergestellt, dass beide Partner den gleichen Ausgangswert verwenden und daraus später das gleiche
        // Shared-Secret berechnen.
        DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey) alicePubKey).getParams();

        // Nun kann Bob sein Private-Public-Key-Pair erzeugen.
        // Dafür benötigt er einen Schlüsselgenerator im DH-Modus.
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamFromAlicePubKey);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();

        // Konvertiere die Keys zum Speichern in Byte-Arrys.
        byte[] bobPrivKeyEnc = bobKpair.getPrivate().getEncoded();
        byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();

        // Bob kann nun bereits das Shared-Secret berechnen (Alice benötigt  zu diesem Zeitunkt noch Bob's Public-Key).
        // Bob initialisert mit seinem Private-Key ein Key-Agreement und übergibt diesem Alices' Public-Key, um den
        // DHKE auf seiner Seite abzuschließen.
        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
        bobKeyAgree.init(bobKpair.getPrivate());
        bobKeyAgree.doPhase(alicePubKey, true);

        // Gib Bob's Private- und Public-Key zurück, sowie das Shared-Secret (Aus dem Key-Agreement ableiten).
        return new byte[][]{bobPrivKeyEnc, bobPubKeyEnc, bobKeyAgree.generateSecret()};
    }

    /**
     * Diese Methode schließt den DHKE endgültig ab. Nachdem Bob mit Alices' Public-Key sein Schlüsselpaar erzeugt hat,
     * muss nun Alice unter Verwendung von Bob's Public-Key auf ihrer Seite ebenfalls das Shared-Secret berechnen.
     *
     * @param alicePrivKeyEnc Private-Key von Alice, als Byte-Array codiert.
     * @param bobPubKeyEnc Public-Key von Bob, als Byte-Array codiert.
     * @return Shared-Secret von Alice, als Byte-Array codiert.
     */
    static byte[] aliceComplete(byte[] alicePrivKeyEnc, byte[] bobPubKeyEnc) throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {

        // Erzeuge eine Key-Factory und Key-Specs, um den codierten Private-Key von Alice in einen Schlüssel umzuwandeln.
        KeyFactory alicePrivKeyFac = KeyFactory.getInstance("DH");
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(alicePrivKeyEnc);
        PrivateKey alicePrivKey = alicePrivKeyFac.generatePrivate(pkcs8KeySpec);

        // Erzeuge eine Key-Factory und Key-Specs, um den codierten Public-Key von Bob in einen Schlüssel umzuwandeln.
        KeyFactory bobPubKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
        PublicKey bobPubKey = bobPubKeyFac.generatePublic(x509KeySpec);

        // Alice kann nun abschließend das Shared-Secret berechnen. Alice initialisert mit ihrem Private-Key genau wie
        // Bob ein Key-Agreement und übergibt diesem Bob's Public-Key, um den DHKE auf ihrer Seite abzuschließen.
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(alicePrivKey);
        aliceKeyAgree.doPhase(bobPubKey, true);

        // Gib  das Shared-Secret zurück, abgeleitet aus dem Key-Agreement.
        return aliceKeyAgree.generateSecret();
    }
}
