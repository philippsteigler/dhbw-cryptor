package sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class Steganographie {

    static void hide(File document, File picture) throws Exception {

        // TODO: Variabler AES Key
        // Dokument einlesen und mit AES verschlüsseln
        FileInputStream fileInputStream = new FileInputStream(document);
        byte[] documentBytes = new byte[(int) document.length()];
        fileInputStream.read(documentBytes);
        byte[] encryptedDocumentBytes = AES.encrypt(documentBytes, "test");

        // Flag zur Wiedererkennung des Textendes beim Extrahieren
        byte[] documentEndFlag = new byte[4];
        Arrays.fill(documentEndFlag, (byte) 88);

        // Dabeinamen extrahieren und verschlüsseln
        byte[] fileNameBytes = document.getName().getBytes(StandardCharsets.UTF_8);
        byte[] encryptedFileNameBytes = AES.encrypt(fileNameBytes, "test");

        // Flag am Ende des Chiffretextes
        byte[] chipherEndFlag = new byte[4];
        Arrays.fill(chipherEndFlag, (byte) 42);

        // Hänge die erstellten Byte-Arrays an den Chiffretext an
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(encryptedDocumentBytes);
        outputStream.write(documentEndFlag);
        outputStream.write(encryptedFileNameBytes);
        outputStream.write(chipherEndFlag);
        byte[] cipher = outputStream.toByteArray();

        // Erzeuge aus der eingelesenen Bilddatei ein Bild
        // Dabei wird ein Farbraum verwendet, der neben RGB-Kanälen auch einen Alpha-Kanal besitzt
        BufferedImage tmp = ImageIO.read(picture);
        BufferedImage img = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();

        // Deklaration einiger Hilfvariablen zur Kodierung der Informationen im Bild
        byte[] rgbBytes = new byte[4];
        byte insert;
        byte into;
        int width = img.getWidth();
        int height = img.getHeight();
        int rgbInt;
        int x = 0;
        int y = 0;

        for (byte aesByte: cipher) {
            rgbInt = img.getRGB(x, y);

            rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
            rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
            rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
            rgbBytes[3] = (byte)((rgbInt) & 0xff);

            for (int i = 0; i < 4; i++) {
                insert = (byte)(aesByte & 0b00000011);
                into = (byte)(rgbBytes[i] & 0b11111100);
                rgbBytes[i] = (byte)(insert | into);

                aesByte = (byte)(aesByte >> 2);
            }

            img.setRGB(x, y, ByteBuffer.wrap(rgbBytes).getInt());

            x++;
            if (x >= width) {
                x = 0;
                y++;
                if (y > height) {
                    System.out.println("--OVERWRITING PICTURE");
                    x = 0;
                    y = 0;
                }
            }
        }

        // TODO: PNG-Encoder????
        ImageIO.write(img, "png", new File(picture.getPath().substring(0, picture.getPath().length() - 4) + "_encrypted.png"));
        System.out.println("Encrypted");
    }

    // TODO: Abbruch-Bedingung falls nichts gefunden wurde!!
    static void extract(File picture) throws Exception {
        BufferedImage img = ImageIO.read(picture);

        boolean readFileType = false;
        boolean next = true;
        int countDocumentEndFlag = 0;
        int countCipherEndFlag = 0;

        byte[] rgbBytes = new byte[4];
        byte input;
        int width = img.getWidth();
        int height = img.getHeight();
        int rgbInt;
        int x = 0;
        int y = 0;

        ByteArrayOutputStream outputDocument = new ByteArrayOutputStream();
        ByteArrayOutputStream outputFileType = new ByteArrayOutputStream();

        byte cipherByte = 0;

        while(next) {
            rgbInt = img.getRGB(x, y);

            rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
            rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
            rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
            rgbBytes[3] = (byte)((rgbInt) & 0xff);

            for (byte b: rgbBytes) {
                input = (byte)(b & 0b00000011);
                input = (byte)(input << 6);

                cipherByte = (byte)(cipherByte >> 2);
                cipherByte = (byte)(cipherByte & 0b00111111);
                cipherByte = (byte)(cipherByte | input);
            }

            if (readFileType) {
                switch (cipherByte) {
                    case 42:
                        countCipherEndFlag++;
                        outputFileType.write(cipherByte);

                        if (countCipherEndFlag == 4) {
                            next = false;
                        }

                        break;
                    default:
                        if (countCipherEndFlag != 0 ) {
                            countCipherEndFlag = 0;
                        }

                        outputFileType.write(cipherByte);
                }
            } else {
                switch (cipherByte) {
                    case 88:
                        countDocumentEndFlag++;
                        outputDocument.write(cipherByte);

                        if (countDocumentEndFlag == 4) {
                            readFileType = true;
                        }

                        break;
                    default:
                        if (countDocumentEndFlag != 0) {
                            countDocumentEndFlag = 0;
                        }

                        outputDocument.write(cipherByte);
                }
            }

            x++;
            if (x >= width) {
                x = 0;
                y++;
                if (y > height) {
                    System.out.println("--OVERWRITING PICTURE");
                    x = 0;
                    y = 0;
                }
            }
        }

        byte[] flaggedEncryptedDocumentBytes = outputDocument.toByteArray();
        byte[] encryptedDocumentBytes = new byte[flaggedEncryptedDocumentBytes.length - 4];
        System.arraycopy(flaggedEncryptedDocumentBytes, 0, encryptedDocumentBytes, 0, encryptedDocumentBytes.length);

        // TODO: Variabler AES Key
        byte[] documentBytes = AES.decrypt(encryptedDocumentBytes, "test");

        byte[] flaggedEncryptedFileNameBytes = outputFileType.toByteArray();
        byte[] encryptedFileNameBytes = new byte[flaggedEncryptedFileNameBytes.length - 4];
        System.arraycopy(flaggedEncryptedFileNameBytes, 0, encryptedFileNameBytes, 0, encryptedFileNameBytes.length);

        byte[] fileNameBytes = AES.decrypt(encryptedFileNameBytes, "test");
        String fileName = new String(fileNameBytes, StandardCharsets.UTF_8);

        // TODO: Dialog für "speichern unter"
        try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
            outputStream.write(documentBytes);
        }

        System.out.println("Decrypted");
    }
}
