package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Controller {

    private File document;
    private File picture;
    private byte[][] result;

    @FXML Label label_picturePath;
    @FXML Label label_filePath;

    // Öffnet eine neie Scene für die Dateiauswahl
    // Speichert die Datei in die Variable document
    public void loadFile() {
        FileChooser fileChooser = new FileChooser();

        document = fileChooser.showOpenDialog(new Stage());
        if (document != null) {
            label_filePath.setText(document.getPath());
        }
    }

    // Öffnet eine neie Scene für die Bildauswahl
    // Speichert das Bild in die Variable picture
    public void loadPicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fileChooser.setTitle("Load picture to embed document into..");

        picture = fileChooser.showOpenDialog(new Stage());
        if (picture != null) {
            label_picturePath.setText(picture.getPath());
        }
    }

    // Verschlüsseln und Verstecken der Datei
    public void encrypt() throws Exception {
        if (document != null && picture != null) {
            Steganographie.hide(document, picture);
        }
    }

    // Liest die versteckte nachricht aus einem Bild und entschlüsselt sie
    public void decrypt() throws Exception{
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fileChooser.setTitle("Load picture to extract document from..");

        picture = fileChooser.showOpenDialog(new Stage());

        if (picture != null) {
            label_picturePath.setText(picture.getPath());
            result = Steganographie.extract(picture);
        }

        String fileName = null;
        if (result != null) {
            fileName = new String(result[1], StandardCharsets.UTF_8);
        }

        String[] parts = new String[0];
        if (fileName != null) {
            parts = fileName.split("\\.");
        }

        fileChooser = new FileChooser();

        if (parts.length > 1) {
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Original Format (*." + parts[parts.length-1] + ")", "*." + parts[parts.length-1]);
            fileChooser.getExtensionFilters().add(extFilter);
        } else {
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Original Format (not extension)", "*.");
            fileChooser.getExtensionFilters().add(extFilter);
        }

        fileChooser.setInitialFileName(fileName);
        fileChooser.setTitle("Save decrypted file as..");
        File file = fileChooser.showSaveDialog(new Stage());

        if (file != null && fileName != null) {
            try (FileOutputStream outputStream = new FileOutputStream(file.getPath())) {
                if (result[0] != null) {
                    outputStream.write(result[0]);
                    System.out.println("Decrypted");
                }
            }
        }
    }
}
