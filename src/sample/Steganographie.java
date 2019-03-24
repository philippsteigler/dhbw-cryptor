package sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class Steganographie {

    static BufferedImage hide(File document, File picture) throws Exception {

        // TODO: Variabler AES Key
        // Dokument einlesen und mit AES verschlüsseln
        FileInputStream fileInputStream = new FileInputStream(document);
        byte[] documentBytes = new byte[(int) document.length()];
        fileInputStream.read(documentBytes);
        fileInputStream.close();
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
        if (encryptedDocumentBytes != null) {
            outputStream.write(encryptedDocumentBytes);
        }
        outputStream.write(documentEndFlag);
        if (encryptedFileNameBytes != null) {
            outputStream.write(encryptedFileNameBytes);
        }
        outputStream.write(chipherEndFlag);
        byte[] cipher = outputStream.toByteArray();
        outputStream.close();

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
        byte aesMask = 0b00000011;
        byte rgbMask = (byte) 0b11111100;
        int shift = 0;
        int level = 0;

        for (byte aesByte: cipher) {
            rgbInt = img.getRGB(x, y);

            rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
            rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
            rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
            rgbBytes[3] = (byte)((rgbInt) & 0xff);

            for (int i = 0; i < 4; i++) {
                insert = (byte)(aesByte & aesMask);
                into = (byte)(rgbBytes[i] & rgbMask);
                rgbBytes[i] = (byte)((insert << shift) | into);

                aesByte = (byte)(aesByte >> 2);
            }

            img.setRGB(x, y, ByteBuffer.wrap(rgbBytes).getInt());

            x++;
            if (x >= width) {
                x = 0;
                y++;
                if (y >= height) {
                    level++;
                    if (level >= 2) {
                        System.out.println("--ERROR: FILE TOO BIG, NEED MORE PIXELS");
                        return null;
                    }

                    System.out.println("--WRITING TO 2ND LEVEL");
                    rgbMask = (byte) 0b1111110011;
                    shift = 2;
                    x = 0;
                    y = 0;
                }
            }
        }

        return img;
    }

    static byte[][] extract(File picture) throws Exception {
        BufferedImage img = ImageIO.read(picture);

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
        int x = 0;
        int y = 0;
        byte aesMask = 0b00111111;
        byte rgbMask = 0b00000011;
        int shift = 6;
        int level = 0;

        ByteArrayOutputStream outputDocument = new ByteArrayOutputStream();
        ByteArrayOutputStream outputFileType = new ByteArrayOutputStream();

        while(next) {
            rgbInt = img.getRGB(x, y);

            rgbBytes[0] = (byte)((rgbInt >> 24) & 0xff);
            rgbBytes[1] = (byte)((rgbInt >> 16) & 0xff);
            rgbBytes[2] = (byte)((rgbInt >> 8) & 0xff);
            rgbBytes[3] = (byte)((rgbInt) & 0xff);

            for (byte b: rgbBytes) {
                input = (byte)(b & rgbMask);
                input = (byte)(input << shift);

                cipherByte = (byte)(cipherByte >> 2);
                cipherByte = (byte)(cipherByte & aesMask);
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
                if (y >= height) {
                    level++;
                    if (level >= 2) {
                        return null;
                    }

                    System.out.println("--WRITING TO 2ND LEVEL");
                    rgbMask = 0b00001100;
                    shift = 4;
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

        return new byte[][]{documentBytes, fileNameBytes};
    }
}
