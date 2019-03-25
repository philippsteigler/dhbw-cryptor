package sample;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

public class Controller {

    private File document;
    private File picture;
    private File encryptedPicture;
    private BufferedImage pictureBuffered;
    private UserAdministration userAdministration;

    @FXML Label label_documentFileSize;
    @FXML Label label_documentName;
    @FXML Label label_pictureFileSize;
    @FXML Label label_pictureName;
    @FXML Label label_encryptedPictureFileSize;
    @FXML Label label_encryptedPictureName;
    @FXML Label label_pictureResolution;
    @FXML Label label_userName;
    @FXML ListView<String> listView_Users;

    public Controller() {
        userAdministration = new UserAdministration();
    }

    private static String getFileSizeString(long size) {
        DecimalFormat df = new DecimalFormat("0.00");

        double sizeKb = 1000;
        double sizeMb = sizeKb * sizeKb;
        double sizeGb = sizeMb * sizeKb;
        double sizeTerra = sizeGb * sizeKb;

        if (size < sizeMb) {
            return df.format(size / sizeKb) + " KB";
        } else if (size < sizeGb) {
            return df.format(size / sizeMb) + " MB";
        } else if (size < sizeTerra) {
            return df.format(size / sizeGb) + " GB";
        }

        return "";
    }

    // Öffnet eine enie Scene für die Dateiauswahl
    // Speichert die Datei in die Variable document
    public void loadFile() {
        FileChooser fileChooser = new FileChooser();

        document = fileChooser.showOpenDialog(new Stage());
        if (document != null) {
            label_documentFileSize.setText(getFileSizeString(document.length()));
            label_documentName.setText(document.getName());
        }
    }

    // Öffnet eine eine Scene für die Bildauswahl
    // Speichert das Bild in die Variable picture
    public void loadPicture() throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fileChooser.setTitle("Load picture to embed document into..");

        picture = fileChooser.showOpenDialog(new Stage());
        if (picture != null) {
            label_pictureFileSize.setText(getFileSizeString(picture.length()));
            label_pictureName.setText(picture.getName());

            pictureBuffered = ImageIO.read(picture);
            int imgWidth = pictureBuffered.getWidth();
            int imgHeight = pictureBuffered.getHeight();
            label_pictureResolution.setText("Info: Resolution of Picture: " + imgWidth + " x " + imgHeight + " (" + imgWidth*imgHeight + " Pixels). This Picture can store up to " + getFileSizeString(imgWidth*imgHeight*2) + ".");
        }
    }

    public void loadEncryptedPicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fileChooser.setTitle("Load picture to extract document from..");

        encryptedPicture = fileChooser.showOpenDialog(new Stage());
        if (encryptedPicture != null) {
            label_encryptedPictureFileSize.setText(getFileSizeString(encryptedPicture.length()));
            label_encryptedPictureName.setText(encryptedPicture.getName());
        }
    }

    // Verschlüsseln und Verstecken der Datei
    public void encrypt() throws Exception {
        if (document == null || picture == null) {
            return;
        }

        pictureBuffered = ImageIO.read(picture);
        int numberOfPixels = pictureBuffered.getHeight()*pictureBuffered.getWidth();
        long fileSize = document.length();

        if (fileSize > numberOfPixels * 2) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Use smaller File (max. " + getFileSizeString(numberOfPixels*2) + ") or Image with higher Resolution (min. " + fileSize/2 + " Pixels).");
            alert.showAndWait();
            return;
        }

        BufferedImage encryptedPicture = Steganographie.hide(document, picture);

        if (encryptedPicture != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
            fileChooser.setTitle("Save encrypted picture as..");
            fileChooser.setInitialFileName(picture.getName().substring(0, picture.getName().lastIndexOf(".")) + "_encrypted");
            File file = fileChooser.showSaveDialog(new Stage());

            if (file != null) {
                try {
                    ImageIO.write(encryptedPicture, "png", file);
                    System.out.println("Encrypted");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Liest die versteckte nachricht aus einem Bild und entschlüsselt sie
    public void decrypt() throws Exception {
        if (encryptedPicture == null) {
            return;
        }

        byte[][] result = Steganographie.extract(encryptedPicture);

        if (result == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("This picture doesn't seem to contain any hidden files!");
            alert.showAndWait();
        }

        String fileName = null;
        if (result != null && result[1] != null) {
            fileName = new String(result[1], StandardCharsets.UTF_8);
        }

        String[] parts = new String[0];
        if (fileName != null) {
            parts = fileName.split("\\.");
        }

        if (result != null && result[1] != null) {
            FileChooser fileChooser = new FileChooser();

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

            try (FileOutputStream outputStream = new FileOutputStream(file.getPath())) {
                if (result[0] != null) {
                    outputStream.write(result[0]);
                    System.out.println("Decrypted");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadUsers() {
        ObservableList<String> observableList = FXCollections.observableArrayList();

        observableList.addAll(userAdministration.getObservableUserNames());

        listView_Users.setItems(observableList);
    }

    public void selectUserItem() {
        label_userName.setText("Name: " + listView_Users.getSelectionModel().getSelectedItem());
    }
}
