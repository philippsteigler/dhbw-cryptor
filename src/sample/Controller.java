package sample;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Observable;

public class Controller {

    public CheckBox checkBox_publicKey;
    public TextField textField_UserName;
    public Button button_loadPublicKey;

    private File document;
    private File picture;
    private File encryptedPicture;
    private File publicKeyFile;
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
    @FXML Label label_publicKey;
    @FXML Label label_publicKeyFile;
    @FXML ListView<String> listView_Users;

    @FXML TableView tableView_users;
    @FXML TableColumn tableColumn_id;
    @FXML TableColumn tableColumn_name;
    @FXML TableRow<String> tableRow_user;

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
        FileChooser fc = new FileChooser();

        document = fc.showOpenDialog(new Stage());
        if (document != null) {
            label_documentFileSize.setText(getFileSizeString(document.length()));
            label_documentName.setText(document.getName());
        }
    }

    // Öffnet eine eine Scene für die Bildauswahl
    // Speichert das Bild in die Variable picture
    public void loadPicture() throws IOException {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fc.setTitle("Load picture to embed document into..");

        picture = fc.showOpenDialog(new Stage());
        if (picture != null) {
            label_pictureFileSize.setText(getFileSizeString(picture.length()));
            label_pictureName.setText(picture.getName());

            pictureBuffered = ImageIO.read(picture);
            int imgWidth = pictureBuffered.getWidth();
            int imgHeight = pictureBuffered.getHeight();
            label_pictureResolution.setText("Info: Resolution of picture: " + imgWidth + " x " + imgHeight + " (" + imgWidth*imgHeight + " Pixels). This picture can store up to " + getFileSizeString(imgWidth*imgHeight*2) + ".");
        }
    }

    public void loadEncryptedPicture() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fc.setTitle("Load picture to extract document from..");

        encryptedPicture = fc.showOpenDialog(new Stage());
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
            alert.setContentText("Use smaller file (max. " + getFileSizeString(numberOfPixels*2) + ") or image with higher resolution (min. " + fileSize/2 + " pixels).");
            alert.showAndWait();
            return;
        }

        BufferedImage encryptedPicture = Steganographie.hide(document, picture);

        if (encryptedPicture != null) {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
            fc.setTitle("Save encrypted picture as..");
            fc.setInitialFileName(picture.getName().substring(0, picture.getName().lastIndexOf(".")) + "_encrypted");
            File file = fc.showSaveDialog(new Stage());

            if (file != null) {
                try {
                    ImageIO.write(encryptedPicture, "png", file);
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
            FileChooser fc = new FileChooser();

            if (parts.length > 1) {
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Original Format (*." + parts[parts.length-1] + ")", "*." + parts[parts.length-1]);
                fc.getExtensionFilters().add(extFilter);
            } else {
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Original Format (not extension)", "*.");
                fc.getExtensionFilters().add(extFilter);
            }

            fc.setInitialFileName(fileName);
            fc.setTitle("Save decrypted file as..");

            File file = fc.showSaveDialog(new Stage());

            if (file != null) {
                try (FileOutputStream os = new FileOutputStream(file.getPath())) {
                    if (result[0] != null) {
                        os.write(result[0]);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void listView_loadUsers() {
        tableView_users.getItems().clear();

        tableColumn_id.setCellValueFactory((Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String>>) p -> {
            String[] x = p.getValue();
            if (x != null && x.length>0) {
                return new SimpleStringProperty(x[0]);
            } else {
                return new SimpleStringProperty("<no id>");
            }
        });

        tableColumn_name.setCellValueFactory((Callback<TableColumn.CellDataFeatures<String[], String>, ObservableValue<String>>) p -> {
            String[] x = p.getValue();
            if (x != null && x.length>1) {
                return new SimpleStringProperty(x[1]);
            } else {
                return new SimpleStringProperty("<no name>");
            }
        });

        tableView_users.getItems().addAll(userAdministration.getIdAndName());
    }

    public void selectUserInView() {
        label_userName.setText("Id: " +  tableColumn_id.getCellData(tableView_users.getSelectionModel().getFocusedIndex()) +
                               " Name: " + tableColumn_name.getCellData(tableView_users.getSelectionModel().getFocusedIndex()));
    }

    public void checkBoxState() {
        button_loadPublicKey.setVisible(!button_loadPublicKey.isVisible());
        label_publicKey.setVisible(!label_publicKey.isVisible());
        label_publicKeyFile.setVisible(!label_publicKeyFile.isVisible());
    }

    public void loadPublicKey() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PUBKEY (.pubKey)", "*.pubKey"));
        fc.setTitle("Load public key for this contact..");

        publicKeyFile = fc.showOpenDialog(new Stage());
        if (publicKeyFile != null) {
            label_publicKeyFile.setText(publicKeyFile.getName());
        }
    }

    public void addUser() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, IOException {
        if (textField_UserName.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please enter a name!");
            alert.showAndWait();
            return;
        }

        if (!checkBox_publicKey.isSelected() && !textField_UserName.getText().isEmpty()) {
            User user = userAdministration.createUser(textField_UserName.getText());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("User '" + user.getName() + "' has been added. Please save the related public key and send it to " + user.getName() + ". Then get their public key and import it under 'contacts'.");
            alert.showAndWait();

            FileChooser fc = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PUBKEY (.pubKey)", "*.pubKey");
            fc.getExtensionFilters().add(extFilter);

            fc.setInitialFileName("publicKey_for_" + user.getName().replaceAll("\\s+","") + ".pubKey");
            fc.setTitle("Save public key for this contact..");

            File file = fc.showSaveDialog(new Stage());

            if (file != null) {
                try (FileOutputStream os = new FileOutputStream(file.getPath())) {
                    os.write(user.getMyPublicKey());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (checkBox_publicKey.isSelected() && !textField_UserName.getText().isEmpty() && publicKeyFile != null) {
            byte[] publicKey = Files.readAllBytes(Paths.get(publicKeyFile.getPath()));
            User user = userAdministration.createUser(textField_UserName.getText(), publicKey);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setContentText("User '" + user.getName() + "' has been added. Please save the related public key and send it to " + user.getName() + ".");
            alert.showAndWait();

            FileChooser fc = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PUBKEY (.pubKey)", "*.pubKey");
            fc.getExtensionFilters().add(extFilter);

            fc.setInitialFileName("publicKey_for_" + user.getName().replaceAll("\\s+","") + ".pubKey");
            fc.setTitle("Save public key for this contact..");

            File file = fc.showSaveDialog(new Stage());

            if (file != null) {
                try (FileOutputStream os = new FileOutputStream(file.getPath())) {
                    os.write(user.getMyPublicKey());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please load this persons public key file!");
            alert.showAndWait();
        }
    }
}
