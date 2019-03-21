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

    private static String getFileExtension(File file) {
        String fileName = file.getName();

        if(fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".")+1);
        else return "";
    }


    static void hide(File document, File picture) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(document);
        byte[] documentBytes = new byte[(int) document.length()];

        fileInputStream.read(documentBytes);

        // TODO: Variabler AES Key
        // Das eingelesene Dokument wird mit AES-256 verschlüsselt
        byte[] encryptedDocumentBytes = AES.encrypt(documentBytes, "test");

        // Flag zur Wiedererkennung des Textendes beim Extrahieren
        byte[] documentEndFlag = new byte[4];
        Arrays.fill(documentEndFlag, (byte) 88);

        // TODO: Dateiendung verschlüsseln
        // Kodierung des Dateityps für die spätere Extraktion aus dem Bild
        byte[] fileTypeBytes = getFileExtension(document).getBytes(StandardCharsets.UTF_8);

        // Flag am Ende des Chiffretextes
        byte[] chipherEndFlag = new byte[4];
        Arrays.fill(chipherEndFlag, (byte) 42);

        // Hänge die erstellten Byte-Arrays an den Chiffretext an
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(encryptedDocumentBytes);
        outputStream.write(documentEndFlag);
        outputStream.write(fileTypeBytes);
        outputStream.write(chipherEndFlag);

        byte[] cipher = outputStream.toByteArray();

        BufferedImage tmp = ImageIO.read(picture);
        BufferedImage img = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();

        int x = 0;
        int y = 0;

        for (byte aesByte: cipher) {
            byte[] rgbBytes = ByteBuffer.allocate(4).putInt(img.getRGB(x,y)).array();

            for (int i = 0; i < rgbBytes.length; i++) {
                byte insert = (byte)(aesByte & 0b00000011);
                byte into = (byte)(rgbBytes[i] & 0b11111100);

                rgbBytes[i] = (byte)(insert | into);

                aesByte = (byte)(aesByte >> 2);
            }

            img.setRGB(x, y, ByteBuffer.wrap(rgbBytes).getInt());

            // Springe ein Pixel weiter
            // Am Ende der Zeile wird in die nächste Zeile gesprungen
            x++;
            if (x >= img.getWidth()){
                x = 0;
                y ++;
                if (y >= img.getHeight()) {
                    System.out.println("--OVERWRITING PICTURE");
                    x = 0;
                    y = 0;
                }
            }
        }

        ImageIO.write(img, "png", new File(picture.getPath().substring(0, picture.getPath().length() - 4) + "_encrypted.png"));
        System.out.println("Encrypted");
    }

    static void extract(File picture) throws Exception {
        BufferedImage img = ImageIO.read(picture);
        int countDocumentEndFlag = 0;
        int countCipherEndFlag = 0;
        boolean readFileType = false;
        int x = 0;
        int y = 0;

        ByteArrayOutputStream outputDocument = new ByteArrayOutputStream();
        ByteArrayOutputStream outputFileType = new ByteArrayOutputStream();

        byte cipherByte = 0;

        while(true) {
            byte[] rgbBytes = ByteBuffer.allocate(4).putInt(img.getRGB(x,y)).array();

            for (byte b: rgbBytes) {
                byte input = (byte)(b & 0b00000011);
                input = (byte)(input << 6);

                cipherByte = (byte)(cipherByte >> 2);
                cipherByte = (byte)(cipherByte & 0b00111111);
                cipherByte = (byte)(cipherByte | input);
            }

            if (readFileType) {
                if (cipherByte == 42) {
                    countCipherEndFlag++;
                } else {
                    if (countCipherEndFlag < 4) {
                        countCipherEndFlag = 0;
                    } else if (countCipherEndFlag == 4) {
                        break;
                    }
                }

                if (countCipherEndFlag <= 4) {
                    outputFileType.write(cipherByte);
                }
            } else {
                if (cipherByte == 88) {
                    countDocumentEndFlag++;
                } else {
                    if (countDocumentEndFlag < 4) {
                        countDocumentEndFlag = 0;
                    } else {
                        countDocumentEndFlag++;
                        countCipherEndFlag++;
                        outputFileType.write(cipherByte);
                        readFileType = true;
                    }
                }

                if (countDocumentEndFlag <= 4) {
                    outputDocument.write(cipherByte);
                }
            }

            // Springe ein Pixel weiter
            // Am Ende der Zeile wird in die nächste Zeile gesprungen
            x ++;
            if (x >= img.getWidth()){
                x = 0;
                y ++;
                if (y >= img.getHeight()) {
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

        byte[] flaggedFileTypeBytes = outputFileType.toByteArray();
        byte[] fileTypeBytes = new byte[flaggedFileTypeBytes.length - 4];
        System.arraycopy(flaggedFileTypeBytes, 0, fileTypeBytes, 0, fileTypeBytes.length);

        String fileType = new String(fileTypeBytes, StandardCharsets.UTF_8);

        // TODO: Dialog für speichern unter
        if (fileType.length() > 0) {
            try (FileOutputStream outputStream = new FileOutputStream("decrypted." + fileType)) {
                outputStream.write(documentBytes);
            }
        } else {
            try (FileOutputStream outputStream = new FileOutputStream("decrypted")) {
                outputStream.write(documentBytes);
            }
        }





        System.out.println("Decrypted");
    }
}
