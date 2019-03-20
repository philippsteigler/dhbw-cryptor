package sample;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Steganographie {

    static void hide(byte[] encrypted, File picture) throws Exception {
        BufferedImage img = ImageIO.read(picture);
        //TODO alphakanalzeug BufferedImage img = new BufferedImage(inputImg.getWidth(), inputImg.getHeight(), BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;

        for (byte msg: encrypted) {
            byte[] RGB = ByteBuffer.allocate(4).putInt(img.getRGB(x,y)).array();

            //System.out.println("imgRGB 0: "+ Integer.toBinaryString((RGB[0]+256)%256));
            //System.out.println("imgRGB 1: "+ Integer.toBinaryString((RGB[1]+256)%256));
            //System.out.println("imgRGB 2: "+ Integer.toBinaryString((RGB[2]+256)%256));
            //System.out.println("imgRGB 3: "+ Integer.toBinaryString((RGB[3]+256)%256));


            byte[] pixelByte = RGB;

            for (int i = 0; i < pixelByte.length; i++) {
                byte insert = (byte)(msg & 0b00000011);
                byte into = (byte)(pixelByte[i] & 0b11111100);

                pixelByte[i] = (byte)(insert | into);

                //System.out.println("msg: "+ Integer.toBinaryString((msg+256)%256));
                //System.out.println("imgRGB insert: "+ Integer.toBinaryString((insert+256)%256));
                //System.out.println("imgRGB into: "+ Integer.toBinaryString((into+256)%256));
                //System.out.println("imgRGB pixlByte: "+ Integer.toBinaryString((pixelByte[i]+256)%256));

                msg = (byte)(msg >> 2);
            }

            int modifiedRGB = 0;
            for (int i=0; i<4; i++) {
                modifiedRGB <<= 8;
                modifiedRGB |= (int)(pixelByte[i] & 0xff);
            }

            //System.out.println("insert imgRGB: "+ Integer.toBinaryString((modifiedRGB)));
            //System.out.println("imgRGB vorher getRGB: "+ Integer.toBinaryString(img.getRGB(x,y)));

            img.setRGB(x, y, 0b11111100111111001111110011111100);
            img.setRGB(x, y, modifiedRGB);

            //System.out.println("imgRGB nachher: "+ Integer.toBinaryString((img.getRGB(x,y))));

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

        ImageIO.write(img, "png", new File(picture.getPath() + "Cryptor.png"));
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
            int RGB = img.getRGB(x,y);
            byte[] pixelByte = ByteBuffer.allocate(4).putInt(RGB).array();

            for (byte b: pixelByte) {
                byte input = (byte)(b & 0b00000011);
                input = (byte)(input << 6);

                msg = (byte) (msg >> 2);
                msg = (byte) (msg & 0b00111111);
                msg = (byte)(msg | input);
            }

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

        byte[] decryptedAES = AES.decrypt(cutEnd, "test");

        //TODO Dynamisches schreiben mit dateityp
        try (FileOutputStream outputStream = new FileOutputStream("C:\\Users\\roman\\Desktop\\out.txt")) {
               outputStream.write(decryptedAES);
        }
    }
}
