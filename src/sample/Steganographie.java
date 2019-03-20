package sample;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class Steganographie {

    static void hide(byte[] encrypted, File picture) throws Exception {
        BufferedImage img = ImageIO.read(picture);
        //TODO alphakanalzeug BufferedImage img = new BufferedImage(inputImg.getWidth(), inputImg.getHeight(), BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;

        for (byte msg: encrypted) {
            byte[] pixelByte = ByteBuffer.allocate(4).putInt(img.getRGB(x,y)).array();

            for (int i = 0; i < pixelByte.length; i++) {
                byte insert = (byte)(msg & 0b00000011);
                byte into = (byte)(pixelByte[i] & 0b11111100);

                pixelByte[i] = (byte)(insert | into);

                msg = (byte)(msg >> 2);
            }

            img.setRGB(x, y, ByteBuffer.wrap(pixelByte).getInt());

            //Pixel adressen Routine
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

        ImageIO.write(img, "png", new File(picture.getPath() + "Cryptor.png"));
        System.out.println("Encrypted");
    }

    static void extract(File picture) throws Exception {
        BufferedImage img;
        img = ImageIO.read(picture);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int x = 0;
        int y = 0;
        int count = 0;

        byte msg = 0;

        while(true) {
            byte[] pixelByte = ByteBuffer.allocate(4).putInt(img.getRGB(x,y)).array();

            for (byte b: pixelByte) {
                byte input = (byte)(b & 0b00000011);
                input = (byte)(input << 6);

                msg = (byte) (msg >> 2);
                msg = (byte) (msg & 0b00111111);
                msg = (byte)(msg | input);
            }

            //Abbruchbedingung für das Auslesen der Datei
            output.write(msg);

            if (msg == 88) {
                count++;

                if (count >=4) {
                  break;
                }
            }
            else {
                count = 0;
            }

            //Pixel adressen Routine
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

        byte[] outEncryptedAES = output.toByteArray();
        byte[] cutEnd = new byte[outEncryptedAES.length-4];

        for (int i = 0; i < cutEnd.length; i++) {
            cutEnd[i] = outEncryptedAES[i];
        }

        //TODO "test" durch schlüsselvariable tauschen
        byte[] decryptedAES = AES.decrypt(cutEnd, "test");

        //TODO Dynamisches schreiben mit dateityp
        try (FileOutputStream outputStream = new FileOutputStream("C:\\Users\\roman\\Desktop\\out.txt")) {
               outputStream.write(decryptedAES);
        }
        System.out.println("Decrypted");
    }
}
