package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;

public class Controller {

    private File document;
    private File picture;

    @FXML Label label_picturePath;
    @FXML Label label_filePath;

    //Öffnet eine neie Scene für die Dateiauswahl
    //Speichert die Datei in die Variable document
    public void loadFile() {
        FileChooser fileChooser = new FileChooser();

        document = fileChooser.showOpenDialog(new Stage());

        label_filePath.setText(document.getPath());
    }

    //Öffnet eine neie Scene für die Bildauswahl
    //Speichert das Bild in die Variable picture
    public void loadPicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg"));

        picture = fileChooser.showOpenDialog(new Stage());

        label_picturePath.setText(picture.getPath());
    }

    //Verschlüsseln und Verstecken der Datei
    public void encrypt() throws Exception {
        FileInputStream fileInputStream = new FileInputStream(document);

        byte[] inputBytes = new byte[(int) document.length()];
    }

}
