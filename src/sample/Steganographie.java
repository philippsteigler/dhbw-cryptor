package sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

class Steganographie {

    static void hide(File document, File picture) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(document);
        byte[] documentBytes = new byte[(int) document.length()];

        fileInputStream.read(documentBytes);

        // TODO "test" durch schlüsselvariable tauschen
        byte[] encryptedDocumentBytes = AES.encrypt(documentBytes, "test");

        // TODO: Flags für dateityp
        // Schlussbytes 88 88 88 88 zur Wiedererkennung beim Extrahieren anhängen
        byte[] documentEndFlag = new byte[4];
        Arrays.fill(documentEndFlag, (byte) 88);

        byte[] encryptedFile = new byte[encryptedDocumentBytes.length + documentEndFlag.length];
        System.arraycopy(encryptedDocumentBytes, 0, encryptedFile, 0 , encryptedDocumentBytes.length);
        System.arraycopy(documentEndFlag, 0, encryptedFile, encryptedDocumentBytes.length , documentEndFlag.length);

        BufferedImage tmp = ImageIO.read(picture);
        BufferedImage img = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.drawImage(tmp, 0, 0, null);
        g.dispose();

        // TODO alphakanalzeug BufferedImage img = new BufferedImage(inputImg.getWidth(), inputImg.getHeight(), BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;

        for (byte aesByte: encryptedFile) {
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
                    // TODO: nächste Ebende in RGB
                    x = 0;
                    y = 0;
                }
            }
        }

        ImageIO.write(img, "png", new File(picture.getPath() + "_Cryptor.png"));
        System.out.println("Encrypted");
    }

    static void extract(File picture) throws Exception {
        BufferedImage img = ImageIO.read(picture);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int x = 0;
        int y = 0;
        int count = 0;

        byte aesByte = 0;

        while(true) {
            byte[] rgbBytes = ByteBuffer.allocate(4).putInt(img.getRGB(x,y)).array();

            for (byte b: rgbBytes) {
                byte input = (byte)(b & 0b00000011);
                input = (byte)(input << 6);

                aesByte = (byte)(aesByte >> 2);
                aesByte = (byte)(aesByte & 0b00111111);
                aesByte = (byte)(aesByte | input);
            }

            // Füge das verschlüsslte Byte zum Chiffretext hinzu
            output.write(aesByte);

            // Abbruchbedingung für das Extrahieren sind die Schlussbytes 88 88 88 88
            if (aesByte == 88) {
                count++;

                if (count >= 4) {
                  break;
                }
            }
            else {
                count = 0;
            }

            // Springe ein Pixel weiter
            // Am Ende der Zeile wird in die nächste Zeile gesprungen
            x ++;
            if (x >= img.getWidth()){
                x = 0;
                y ++;
                if (y >= img.getHeight()) {
                    // TODO: nächste Ebende in RGB
                    x = 0;
                    y = 0;
                }
            }
        }

        byte[] encryptedFile = output.toByteArray();
        byte[] encryptedDocumentBytes = new byte[encryptedFile.length - 4];

        System.arraycopy(encryptedFile, 0, encryptedDocumentBytes, 0, encryptedDocumentBytes.length);

        //TODO "test" durch schlüsselvariable tauschen
        byte[] documentBytes = AES.decrypt(encryptedDocumentBytes, "test");

        //TODO Dynamisches schreiben mit dateityp
        try (FileOutputStream outputStream = new FileOutputStream("out.pdf")) {
               outputStream.write(documentBytes);
        }

        System.out.println("Decrypted");
    }
}
