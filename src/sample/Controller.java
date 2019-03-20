package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class Controller {

    private File document;
    private File picture;

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));

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
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png"));

        picture = fileChooser.showOpenDialog(new Stage());

        if (picture != null) {
            label_picturePath.setText(picture.getPath());
            Steganographie.extract(picture);
        }
    }
}
