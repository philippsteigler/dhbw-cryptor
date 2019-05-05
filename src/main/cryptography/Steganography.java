package main.cryptography;

import javafx.scene.control.Alert;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

/**
 * Klasse zum Verstecken und Extrahieren von Informationen in/aus Bildern.
 * Hierfür wurde ein eigenes proprietäres Verfahren entwickelt.
 *
 * Zunächst wird das eingelesene Dokument komprimiert und mit einem bekannten und sicheren Verfahren verschlüsselt.
 * Anschließend wird der Chiffretext in die einzelnen Pixel eines PNG-Bildes codiert.
 *
 * Bei diesem Verfahren werden jeweils die niedrigsten Bits der ARGB-Werte mit Informationen zur verschlüsselten
 * Datei überschrieben. Anschließend können diese Daten auf umgekehrten Weg extrahiert und zur originalen Datei
 * zusammengesetzt werden - vorausgesetzt die Entschlüsselung war erfolgreich.
 */
public class Steganography {

    /**
     * Funktion zum Verstecken eines Dokuments in einem PNG-Bild.
     *
     * Das Dokument wird mit GZIP komprimiert und anschließend mit AES verschlüsselt. Hierfür wird das Shared-Secret
     * der Zielperson übergeben und ein AES-Key abgeleitet. Danach wird der Byte-Strom mit einem Flag zur
     * Wiedererkennung vom Ende der codierten Datei gekennzeichnet.
     *
     * Abschließend wird die Endung der ursprünglichen Datei ebenfalls mit AES verschlüsselt und an die Bytefolge
     * angehängt. Dies dient der Rekonstruktion des originalen Dateiformats. Auch an diese Information wird ein Flag
     * zur Erfassung des Endes angehängt. Das Ergebnis ist ein einzelnes Byte-Array, welches nun im Bild versteckt wird.
     *
     * Das steganografische Verfahren zum codieren des Chiffretextes funktioniert wiefolgt:
     * Ein Byte des Chiffretextes wird auf exakt ein Pixel codiert. Ein Pixel besteht dabei aus vier Bytes, welche die
     * mit jeweils einem Byte die Farbkanäle Alpha, Rot, Grün und Blau (ARGB) repräsentieren. Daraus ergibt sich, dass
     * die acht Bits des Chiffre-Bytes gleichmäßig auf die vier Bytes des Pixels verteilt werden müssen:
     * Jeweils 2 Bits auf ein ARGB-Byte. Dabei werden stets die niedrigsten beiden Bits (1 und 2) eines ARGB-Wertes
     * überschrieben, sodass der Farbwert im Ausgabe-Bild maximal um 4 Einheiten abweicht.
     *
     * Wurden alle Pixel codiert, so wird dieses steganografische Verfahren ein Mal wiederholt. Dabei werden aber nicht
     * die untersten beiden Bits der ARGB-Bytes codiert, sondern die nächsthöheren: Bit 3 und 4. Um zu vermeiden, dass
     * die ARGB-Werte des originalen Bildes zu stark manipuliert werden, erfolgt nach dieser Runde keine weitere
     * Codierung der nächsthöheren Bits. Dies bildet einen Kompromiss zwischen Sichtbarkeit der Manipulation und
     * Kapazität zur Codierung einer Datei in das Bild, da somit maximal 50% der Bits überschrieben werden.
     *
     * @param document Zu versteckende Datei als File.
     * @param picture PNG-Bild, in welches die Datei eingebettet wird.
     * @param sharedSecret Mit Diffie-Hellman erzeugtes symmetrisches Geheimnis zur Erzeugung eines AES-Keys.
     * @return Manipuliertes PNG-Bild als BufferedImage.
     */
    public static BufferedImage hide(File document, File picture, byte[] sharedSecret) throws Exception {

        // Das übermittelte Dokument wird von einer Datei in eine Byte-Folge konvertiert.
        // Anschließend wird das Dokument mittels AES verschlüsselt.
        byte[] documentBytes = Files.readAllBytes(document.toPath());
        byte[] encryptedDocumentBytes = AES.encrypt(documentBytes, sharedSecret);

        byte[] endPoints = AES.encrypt(ByteBuffer.allocate(100).putInt(130).array(), sharedSecret);

        // Erstellt eine Byte-Folge als Flag zur Erkennung vom Ende der Datei
        byte[] documentEndFlag = new byte[5];
        if (endPoints != null) {
            Arrays.fill(documentEndFlag, endPoints[88]);
        }
        documentEndFlag[4] = (byte) (documentEndFlag[4] << 1);

        // Erstellt eine Byte-Folge als Flag zur Erkennung vom Ende des gesamten Chiffretextes
        byte[] chipherEndFlag = new byte[5];
        if (endPoints != null) {
            Arrays.fill(chipherEndFlag, endPoints[42]);
        }
        chipherEndFlag[4] = (byte) (chipherEndFlag[4] << 1);

        // Extrahiert den Dateinamen als Byte-Folge. Diese wird ebenfalls mit dem gleichen Key verschlüsselt.
        byte[] encryptedFileNameBytes = AES.encrypt(document.getName().getBytes(Charset.forName("UTF-8")), sharedSecret);

        // Fügt die Byte-Arrays zu einem gesamten Chiffretext zusammen.
        // Dokument (zipped, encrypted) --> Dokument-Flag --> Dateityp (encrypted) --> Ende-Flag
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(encryptedDocumentBytes);
        byteArrayOutputStream.write(documentEndFlag);
        byteArrayOutputStream.write(encryptedFileNameBytes);
        byteArrayOutputStream.write(chipherEndFlag);
        byte[] cipher = byteArrayOutputStream.toByteArray();
        byteArrayOutputStream.close();

        // Im zweiten Schritt wird der erzeugte Byte-Strom in das Bild codiert.
        // Konvertiere die Bild-Datei hierzu in ein BufferedImage, um die ARGB-Werte zu modifizieren.
        //
        // Dabei wird ein Farbraum verwendet, der neben RGB-Kanälen auch einen Alpha-Kanal besitzt und diesen somit
        // automatisch erstellt, falls das Ausgangsbild keinen besitzt.
        BufferedImage tmp = ImageIO.read(picture);
        BufferedImage img = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();

        // Deklaration einiger Hilfvariablen für den steganografischen Algorithmus.
        byte[] rgbBytes = new byte[4];
        byte insert;
        byte into;
        int width = img.getWidth();
        int height = img.getHeight();
        int rgbInt;
        int x = -1;
        int y = 0;
        byte aesMask = (byte) 0b00000011;
        byte rgbMask = (byte) 0b11111100;

        // Für jedes Byte des Chiffretextes: Bits auf ARGB-Wert eines Pixels verteilen.
        for (byte aesByte: cipher) {

            // Zunächst rückt der Lesekopf ein Pixel weiter. Zu Beginn startet er außerhalb des Bildes und rückt auf
            // das erste Pixel. Am Ende einer Zeile wird in die nächste gesprungen. Am Ende des Bildes wird einmalig
            // von Vorne angefangen, indem die nächsthöheren Bits der ARGB-Werte manipuliert werden. Dementsprechend
            // wird eine Maske definiert.
            x++;
            if (x >= width) {
                y++;
                if (y >= height) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("This picture is not big enough for this File.");
                    alert.showAndWait();
                    return null;
                }
                x = 0;
            }

            // Danach wird der ARGB-Wert des aktuellen Pixels geladen und in seine 4 Bytes aufgeteilt.
            // Alpha --> Rot --> Grün --> Blau
            rgbInt = img.getRGB(x, y);
            rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
            rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
            rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
            rgbBytes[3] = (byte)((rgbInt) & 0xff);

            // In vier Runden werden jeweils 2 Bits in ein Byte des ARGB-Wertes codiert.
            //
            // Dafür wird das Chiffre-Byte mit einer Maske so manipuliert, dass alle Bits außer den niedrigsten beiden
            // 0 sind. Das ARGB-Byte wird im Gegensatz dazu so manipuliert, dass genau die ersten beiden Bits 0 sind.
            // Zum Schluss werden die beiden maskierten Bytes miteinander ODER-Verknüpft, sodass die restlichen höheren
            // Bits erhalten bleiben, wodurch der ursprüngliche Farbwert kaum abweicht.
            //
            // Dieses Verfahren wird in vier Runden durchgeführt, wobei das Byte des Chiffretextes jeweils um 2 Bits
            // geshiftet wird. Die Masken und sind an dieser Stelle variabel, da im zweiten Durchgang die nächsthöheren
            // Bits 3 und 4 beschrieben werden, was andere Masken erfordert.
            for (int i = 0; i < 4; i++) {
                insert = (byte)(aesByte & aesMask);
                into = (byte)(rgbBytes[i] & rgbMask);
                rgbBytes[i] = (byte)(insert | into);

                aesByte = (byte)(aesByte >> 2);
            }

            // Der mit den Informationen angereicherte ARGB-Wert wird nach der Codierung in das Bild geschrieben.
            img.setRGB(x, y, ByteBuffer.wrap(rgbBytes).getInt());
        }

        // Wurden noch nicht alle Pixel manipuliert, so werden die restlichen Pixel mit zufälligen Werten beschrieben.
        if (x != width-1 && y != height-1) {

            // Dafür wird ein Byte-Array mit einer Länge gleich der Anzahl an verbleibenden Pixeln generiert und
            // anschließend mit Zufallswertden befüllt.
            byte[] randoms = new byte[(width-x-1) + ((height-y-1)*width)];
            new Random().nextBytes(randoms);

            // Die Codierung der Pixel erfolgt analog zum vorherigen Ablauf mit dem Chiffretext.
            for (byte randomByte: randoms) {
                x++;
                if (x >= width) {
                    y++;
                    if (y >= height) {
                        System.out.println("Error while encrypting: Something went wrong.");
                        break;
                    }
                    x = 0;
                }

                rgbInt = img.getRGB(x, y);
                rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
                rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
                rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
                rgbBytes[3] = (byte)((rgbInt) & 0xff);

                for (int i = 0; i < 4; i++) {
                    insert = (byte)(randomByte & aesMask);
                    into = (byte)(rgbBytes[i] & rgbMask);
                    rgbBytes[i] = (byte)(insert | into);

                    randomByte = (byte)(randomByte >> 2);
                }

                img.setRGB(x, y, ByteBuffer.wrap(rgbBytes).getInt());
            }
        }

        // Zum Schluss wird das manipulierte Bild zurückgegeben.
        return img;
    }

    /**
     * Funktion zum Extrahieren eines Dokuments, das mit Cryptor in einem PNG-Bild versteckt wurde.
     *
     * Der Vorgang läuft analog zum Verstecken ab - nur Rückwärts.
     * Als erstes wird der Chiffretext schrittweise aus den Pixeln des Bildes extrahiert. Dabei werden jeweils die
     * letzten beiden Bits der ARGB-Bytes zu einem Chiffretext-Byte zusammengesetzt. Die beim Verstecken codierten
     * Flags kennzeichnen an dieser Stelle das Ende des Dokuments und des mitgelieferten Dateinamens.
     *
     * Wurde kein Flag erfasst, so werden nach Durchlaufen aller Pixel auf der nächsthöheren Ebene die Bits 3 und 4
     * ausgewertet. Der extrahierte Chiffretext wird anschließend unter Verwendung eines geheimen Keys entschlüsselt -
     * vorausgesetzt es handelt sich um den gleichen Key wie bei der Verschlüsselung. War dies erfolgreich, so wird die
     * entschlüsselte Datei nun mit GZIP dekomprimiert.
     *
     * Das Ergebnis ist eine entschlüsselte Datei und deren urspürnglicher Dateiname mit Dateityp, sodass die originale
     * Datei vollständig wiederhergestellt werden kann. Wurde auf der zweiten Ebene auch kein Ende-Flag erfasst, so
     * bricht der Algorithmus ab, da keine versteckte Datei im PNG-Bild erfasst wurde.
     *
     * @param picture PNG-Bild, in welchem eventuell eine Datei eingebettet wurde.
     * @param sharedSecret Mit Diffie-Hellman erzeugtes symmetrisches Geheimnis zur Erzeugung eines AES-Keys.
     * @return Extrahierte Datei und deren ursprünglicher Name mit Dateityp.
     */
    public static byte[][] extract(File picture, byte[] sharedSecret) throws Exception {

        // Das übermittelte Bild wird in ein BufferedImage verwandelt, um die ARGB-Werte auszulesen.
        BufferedImage img = ImageIO.read(picture);

        byte[] endPoints = AES.encrypt(ByteBuffer.allocate(100).putInt(130).array(), sharedSecret);

        // Einige Hilfsvariablen zum Scannen des Bildes und Auslesen von Informationen.
        boolean readFileType = false;
        boolean next = true;
        int countDocumentEndFlag = 0;
        int countCipherEndFlag = 0;

        byte[] rgbBytes = new byte[4];
        byte cipherByte = 0;
        byte input;
        int width = img.getWidth();
        int height = img.getHeight();
        int rgbInt;
        int x = -1;
        int y = 0;
        byte aesMask = 0b00111111;
        byte rgbMask = 0b00000011;
        int shift = 6;

        // Extrahierte Daten werden in Outputstreams geschrieben und später entschlüsselt, dekomprimiert, etc.
        ByteArrayOutputStream outputDocument = new ByteArrayOutputStream();
        ByteArrayOutputStream outputFileType = new ByteArrayOutputStream();

        while(next) {

            // Zunächst rückt der Lesekopf ein Pixel weiter. Zu Beginn startet er außerhalb des Bildes und rückt auf
            // das erste Pixel. Am Ende einer Zeile wird in die nächste gesprungen. Am Ende des Bildes wird einmalig
            // von Vorne angefangen, indem die nächsthöheren Bits der ARGB-Werte manipuliert werden. Dementsprechend
            // wird eine Maske definiert.
            x++;
            if (x >= width) {
                y++;
                if (y >= height) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("This picture doesn't seem to contain any hidden files.");
                    alert.showAndWait();
                    return null;
                }
                x = 0;
            }

            // Danach wird der ARGB-Wert des aktuellen Pixels geladen und in seine 4 Bytes aufgeteilt.
            // Alpha --> Rot --> Grün --> Blau
            rgbInt = img.getRGB(x, y);
            rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
            rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
            rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
            rgbBytes[3] = (byte)((rgbInt) & 0xff);

            // In vier Runden werden jeweils 2 Bits aus dem Byte des ARGB-Wertes gelesen und zu einem Byte des
            // Chiffretextes zusammengesetzt.
            //
            // Dafür wird das ARGB-Byte mit einer Maske so manipuliert, dass alle Bits außer den niedrigsten beiden 0
            // sind. Danach werden diese beiden Bits in das Chiffretext-Byte geschrieben, woraufhin dieses für die
            // nächste Runde um 2 Stellen geshiftet wird und sich dieser Vorgang wiederholt. Die Maske für das
            // Chiffretext-Byte stellt sicher, dass stets 0 nachgeschoben und mit dem Chiffrewert überschrieben werden.
            for (byte b: rgbBytes) {
                input = (byte)(b & rgbMask);
                input = (byte)(input << shift);

                cipherByte = (byte)(cipherByte >> 2);
                cipherByte = (byte)(cipherByte & aesMask);
                cipherByte = (byte)(cipherByte | input);
            }

            // Nach zusammensetzen eines Chiffretext-Bytes wird dessen Wert evaluiert.
            // Es wird zwischen zwei Modi unterschieden: Dokument auslesen (bis zum Flag vom Ende der Datei) und
            // Dateiname/-typ auslesen (nach dem Ende der Datei, bis zum Flag vom Ende des Namens/Typs).
            if (readFileType) {

                // Im Modus Dateiname/-typ auslesen: Wird vier Mal der Wert 42 erfasst, so handelt es sich um das
                // Ende-Flag. Andernfalls handelt es sich um eine zufällige 42 im Dateinamen und der Algorithmus wartet
                // weiterhin auf vier Mal 42 in Folge.
                if (cipherByte == endPoints[42])
                {
                    countCipherEndFlag++;
                    outputFileType.write(cipherByte);

                } else {
                    // Wurde das vierstellige Ende-Flag des gesamten Chiffretextes erfasst, Beende den Lesevorgang.
                    if (countCipherEndFlag >= 4) {
                        next = false;
                    } else {
                        outputFileType.write(cipherByte);
                    }

                    // Wenn nach einer 42 keine weitere 42 gelesen wird, so setze den Zähler zurück.
                    if (countCipherEndFlag != 0 ) {
                        countCipherEndFlag = 0;
                    }
                }

            } else {

                // Im Modus Dokument auslesen: Wird vier Mal der Wert 88 erfasst, so handelt es sich um das
                // Ende-Flag. Andernfalls handelt es sich um eine zufällige 42 im codierten Dokument und der Algorithmus
                // wartet weiterhin auf vier Mal 88 in Folge.
                if (endPoints != null) {
                    if (cipherByte == endPoints[88]){
                        countDocumentEndFlag++;
                        outputDocument.write(cipherByte);

                    } else {
                        // Wurde das vierstellige Ende-Flag erfasst, wechsel in den Dateiname/-typ-Lesen-Modus.
                        if (countDocumentEndFlag >= 4) {
                            readFileType = true;
                        } else {
                            outputDocument.write(cipherByte);
                        }

                        // Wenn nach einer 88 keine weitere 88 gelesen wird, so setze den Zähler zurück.
                        if (countDocumentEndFlag != 0) {
                            countDocumentEndFlag = 0;
                        }
                    }
                }
            }
        }

        // Als Ergebnis liegen zwei Outputstreams vor: Dokument und dessen Dateiname mit Typ, jeweils mit Flag am Ende
        // Deshalb werden die Outputstreams in Byte-Arrays geschrieben. Danach werden die Flags abgeschnitten.
        byte[] flaggedEncryptedDocumentBytes = outputDocument.toByteArray();
        byte[] encryptedDocumentBytes = new byte[flaggedEncryptedDocumentBytes.length - 4];
        System.arraycopy(flaggedEncryptedDocumentBytes, 0, encryptedDocumentBytes, 0, encryptedDocumentBytes.length);

        byte[] flaggedEncryptedFileNameBytes = outputFileType.toByteArray();
        byte[] encryptedFileNameBytes = new byte[flaggedEncryptedFileNameBytes.length - 4];
        System.arraycopy(flaggedEncryptedFileNameBytes, 0, encryptedFileNameBytes, 0, encryptedFileNameBytes.length);

        // Nach Entfernen der Flags wird das Dokument mit dem übergebenen Shared-Secret entschlüsselt.
        byte[] documentBytes = AES.decrypt(encryptedDocumentBytes, sharedSecret);

        // Um die extrahierte Datei exportieren zu können wird zum Schluss auch der Dateiname/-typ entschlüsselt.
        byte[] fileNameBytes = AES.decrypt(encryptedFileNameBytes, sharedSecret);

        // Schlägt die Entschlüsselung fehl, so wird eine Meldung für den Anwender erzeugt. Andernfalls werden
        // Dokument und Dateiname/-typ zum Export als Byte-Arrays übermittelt.
        if (documentBytes == null && fileNameBytes == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Wrong decryption key.");
            alert.showAndWait();
            return null;
        } else {
            return new byte[][]{documentBytes, fileNameBytes};
        }

    }
}
