package main;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import main.cryptography.Steganography;
import main.users.User;
import main.users.UserAdministration;

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
import java.util.stream.Collectors;

/**
 * JavaFx Controller Klasse als Schnittstelle zwischen GUI und Programmlogik.
 * Verarbeitet Ein- und Ausgaben vom/fürs Applikationsfenster.
 */
public class Controller {

    private UserAdministration userAdministration;

    // Encrypt
    private File document;
    private File picture;
    private BufferedImage pictureBuffered;
    @FXML Label label_documentFileSize;
    @FXML Label label_documentName;
    @FXML Label label_pictureFileSize;
    @FXML Label label_pictureName;
    @FXML Label label_pictureResolutionEncryption;
    @FXML ImageView imageView_encrypt;
    @FXML ChoiceBox<User> choiseBox_encryptionUser;
    @FXML Button button_encrypt;

    // Decrypt
    private File encryptedPicture;
    @FXML Label label_encryptedPictureFileSize;
    @FXML Label label_encryptedPictureName;
    @FXML Label label_pictureResolutionDecryption;
    @FXML ImageView imageView_decrypt;
    @FXML ChoiceBox<User> choiseBox_decryptionUser;
    @FXML Button button_decrypt;

    // Contacts
    public Button button_exportPublicKey;
    public Button button_importPublicKey;
    public Button button_deleteContact;
    @FXML Label label_userName;
    @FXML Label label_setupStatus;
    @FXML Label label_importPubKey;
    @FXML Label label_exportPubKey;
    @FXML TableView<User> tableView_users;
    @FXML TableColumn<User, Integer> tableColumn_id;
    @FXML TableColumn<User, String> tableColumn_name;

    // New User
    private File publicKeyFile;
    public Button button_loadPublicKey;
    public CheckBox checkBox_publicKey;
    public TextField textField_UserName;
    @FXML Label label_publicKey;

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

    /**
     * TAB: Encrypt
     *
     * Der Tab Encrypt wird bei der Verschlüsselung von Informationen verwendet.
     * Er Läd die zu verschlüsselnde Datei und das Bild in dem diese versteckt wird.
     */

    // Ist für das Anzeigen des Encrypt-Buttons zuständig.
    //
    // Invertiert bei Aufruf den Anzeigezustand.
    // Sichtbar -> Unsichtbar
    // Unsichtbar -> Sichtbar
    private void updateEncryptButton() {
        if (document == null || picture == null || choiseBox_encryptionUser.getSelectionModel().isEmpty()) {
            button_encrypt.setDisable(true);
        } else {
            button_encrypt.setDisable(false);
        }
    }

    // Öffnet eine eine Scene für die Dateiauswahl und speichert eine Referenz auf diese Datei.
    public void loadFile() {
        FileChooser fc = new FileChooser();

        document = fc.showOpenDialog(new Stage());
        if (document != null) {
            label_documentFileSize.setText("Size: " + getFileSizeString(document.length()));
            label_documentName.setText("File: " + document.getName());
            updateEncryptButton();
        }
    }

    // Öffnet eine eine Scene für die PNG-Bildauswahl und speichert eine Referenz auf dieses PNG-Bild.
    public void loadPicture() throws IOException {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fc.setTitle("Load picture to embed document into..");

        picture = fc.showOpenDialog(new Stage());
        if (picture != null) {
            label_pictureFileSize.setText("Size: " + getFileSizeString(picture.length()));
            label_pictureName.setText("File: " + picture.getName());

            pictureBuffered = ImageIO.read(picture);
            int imgWidth = pictureBuffered.getWidth();
            int imgHeight = pictureBuffered.getHeight();
            label_pictureResolutionEncryption.setText(
                    "Information:\n\n"
                    + "Resolution of picture:\n"
                    + imgWidth + " x " + imgHeight + " (" + imgWidth*imgHeight + " Pixels)\n\n"
                    + "Maximum capacity:\n"
                    + getFileSizeString(imgWidth*imgHeight - 10)
            );

            Image image = new Image(picture.toURI().toString());
            imageView_encrypt.setImage(image);

            updateEncryptButton();
        }
    }

    // Lädt alle Kontakte die ein Shared-Secret generiert haben, da nur diese zur Ver- und Entschlüsselung zugelassen
    // sind. Diese werden in einer ChoiceBox zur Auswahl angezeigt.
    public void loadEncryptionUser() {
        ObservableList<User> userList = FXCollections.observableArrayList(userAdministration.getUsers()
                .stream().filter(user -> user.getSharedSecret().length > 1).collect(Collectors.toList()));

        choiseBox_encryptionUser.setItems(userList);
        choiseBox_encryptionUser.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user.getName();
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });

        choiseBox_encryptionUser.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateEncryptButton());
    }

    // Verschlüsseln und Verstecken der Datei.
    public void encrypt() throws Exception {
        if (document == null || picture == null || choiseBox_encryptionUser.getSelectionModel().isEmpty()) {
            return;
        }

        User user = choiseBox_encryptionUser.getSelectionModel().getSelectedItem();

        pictureBuffered = ImageIO.read(picture);
        int numberOfPixels = pictureBuffered.getHeight()*pictureBuffered.getWidth();
        long fileSize = document.length();

        if (fileSize > numberOfPixels - 10) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Use smaller file (max. " + getFileSizeString(numberOfPixels - 10) + ") or image with higher resolution (min. " + fileSize + " pixels).");
            alert.showAndWait();
            return;
        }

        BufferedImage encryptedPicture = Steganography.hide(document, picture, user.getSharedSecret());

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
                    System.out.println("Error while writing encoded image to file: " + e.toString());
                }
            }
        }

        updateEncryptButton();
    }

    /**
     * TAB: Decrypt
     *
     * Der Tab Encrypt wird bei der Entschlüsselung von Informationen verwendet.
     * Er lädt ein Bild in dem Informationen vermutet werden und versucht diese mit dem passenden Key zu entschlüsseln.
     */

    // Ist für das Anzeigen des Decrypt-Buttons zuständig.
    //
    // Invertiert bei Aufruf den Anzeigezustand.
    // Sichtbar -> Unsichtbar
    // Unsichtbar -> Sichtbar
    private void updateDecryptButton() {
        if (encryptedPicture == null || choiseBox_decryptionUser.getSelectionModel().isEmpty()) {
            button_decrypt.setDisable(true);
        } else {
            button_decrypt.setDisable(false);
        }
    }

    // Lädt das PNG-Bild in dem die verschlüsselte Nachricht vermutet wird.
    public void loadEncryptedPicture() throws IOException {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG (.png)", "*.png"));
        fc.setTitle("Load picture to extract document from..");

        encryptedPicture = fc.showOpenDialog(new Stage());
        if (encryptedPicture != null) {
            label_encryptedPictureFileSize.setText("Size: " + getFileSizeString(encryptedPicture.length()));
            label_encryptedPictureName.setText("File: " + encryptedPicture.getName());

            pictureBuffered = ImageIO.read(encryptedPicture);
            int imgWidth = pictureBuffered.getWidth();
            int imgHeight = pictureBuffered.getHeight();
            label_pictureResolutionDecryption.setText("Information:\n\nResolution of picture:\n" + imgWidth + " x " + imgHeight + " (" + imgWidth*imgHeight + " Pixels)");

            Image image = new Image(encryptedPicture.toURI().toString());
            imageView_decrypt.setImage(image);

            updateDecryptButton();
        }
    }

    // Lädt alle Kontakte die ein Shared-Secret generiert haben.
    // Diese werden in einer ChoiceBox zur Auswahl angezeigt.
    public void loadDecryptionUser() {
        ObservableList<User> userList = FXCollections.observableArrayList(userAdministration.getUsers()
                .stream().filter(user -> user.getSharedSecret().length > 1).collect(Collectors.toList()));

        choiseBox_decryptionUser.setItems(userList);
        choiseBox_decryptionUser.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user.getName();
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        });

        choiseBox_decryptionUser.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> updateDecryptButton());
    }

    // Liest die versteckte Nachricht aus einem Bild und entschlüsselt sie.
    public void decrypt() throws Exception {
        if (encryptedPicture == null || choiseBox_decryptionUser.getSelectionModel().isEmpty()) {
            return;
        }

        User user = choiseBox_decryptionUser.getSelectionModel().getSelectedItem();

        byte[][] result = Steganography.extract(encryptedPicture, user.getSharedSecret());

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
                    System.out.println("Error while writing decrypted document to file: " + e.toString());
                }
            }
        }

        updateDecryptButton();
    }

    /**
     * TAB: Contacts
     *
     * Zeigt alle angelegten Kontakte an.
     * Erlaubt das generieren und eintragen von Public-Keys, um den DHKE abzuschließen.
     */

    // Leert die Tabelle mit Usern, um diese zu aktualisieren.
    public void resetTabContacts() {
        clearSceneContacts();
        loadUsers();
    }

    // Objekte mit Infos über einen Kontakt werden ausgeblendet, solange keiner in der GUI ausgewählt wurde.
    private void clearSceneContacts() {
        label_userName.setVisible(false);
        label_exportPubKey.setVisible(false);
        button_exportPublicKey.setVisible(false);
        label_setupStatus.setVisible(false);
        label_importPubKey.setVisible(false);
        button_importPublicKey.setVisible(false);
        button_deleteContact.setVisible(false);
    }

    // Lädt alle Kontakte und zeigt diese in einer Tabelle an.
    private void loadUsers() {
        tableView_users.getItems().clear();

        ObservableList<User> userList = FXCollections.observableArrayList(userAdministration.getUsers());

        tableColumn_id.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()).asObject());
        tableColumn_name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        tableView_users.setItems(userList);
    }

    // Wird beim anklicken eines Kontakts aufgerufen.
    // Zeigt Informationen über diesen Kontakt an und bietet Möglichkeiten, den Public-Key zu exportieren und den
    // Private-Key zum Abschluss des DHKW zu importieren.
    public void selectUserInView() {
        clearSceneContacts();

        User selectedUser = tableView_users.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        label_userName.setVisible(true);
        label_userName.setText(selectedUser.getName());
        label_exportPubKey.setVisible(true);
        button_exportPublicKey.setVisible(true);

        if (selectedUser.getSharedSecret().length == 1) {
            label_setupStatus.setTextFill(Color.RED);
            label_setupStatus.setText("Setup not completed!");
            label_setupStatus.setVisible(true);

            label_importPubKey.setText("To finish key exchange and generate symmetric crypto keys please import this persons public key.");
            label_importPubKey.setVisible(true);
            button_importPublicKey.setVisible(true);
        } else {
            label_setupStatus.setTextFill(Color.GREEN);
            label_setupStatus.setText("Setup completed.");
            label_setupStatus.setVisible(true);
        }

        button_deleteContact.setVisible(true);
    }

    // Exportiert den Public-Key zum ausgewählten Kontakt.
    public void exportPublicKey() {
        User selectedUser = tableView_users.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("PUBKEY (.pubKey)", "*.pubKey");
        fc.getExtensionFilters().add(extFilter);

        fc.setInitialFileName("publicKey_for_" + selectedUser.getName().replaceAll("\\s+","") + ".pubKey");
        fc.setTitle("Save public key for this contact..");

        File file = fc.showSaveDialog(new Stage());

        if (file != null) {
            try (FileOutputStream os = new FileOutputStream(file.getPath())) {
                os.write(selectedUser.getMyPublicKey());
            } catch (IOException e) {
                System.out.println("Error while writing public key to file: " + e.toString());
            }
        }
    }

    // Importiert den Public-Key zum ausgewählten Kontakt.
    public void importPublicKey() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        User selectedUser = tableView_users.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PUBKEY (.pubKey)", "*.pubKey"));
        fc.setTitle("Load public key for this contact..");

        publicKeyFile = fc.showOpenDialog(new Stage());

        if (publicKeyFile != null) {
            byte[] publicKeyEnc = Files.readAllBytes(Paths.get(publicKeyFile.getPath()));
            userAdministration.finishSetup(selectedUser.getId(), publicKeyEnc);
        }

        resetTabContacts();
    }

    // Löscht einen Kontakt aus der Kontaktliste.
    public void deleteUser() throws IOException {
        User selectedUser = tableView_users.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            return;
        }

        userAdministration.deleteUser(selectedUser.getId());

        resetTabContacts();
    }

    /**
     * TAB: New User
     *
     * Auf diesem Tab können neue Kontakte erzeugt und Schlüssel für diese generiert bzw. hinterlegt werden.
     */

    // Setzt die Benutzereingaben zurück.
    public void resetTabNewUser() {
        textField_UserName.clear();
        checkBox_publicKey.setSelected(false);
        button_loadPublicKey.setVisible(false);

        publicKeyFile = null;
        label_publicKey.setText("");
        label_publicKey.setVisible(false);
    }

    // Zeigt das Feld zur eingabe des Public-Keys an.
    public void checkBoxState() {
        button_loadPublicKey.setVisible(!button_loadPublicKey.isVisible());
        label_publicKey.setVisible(!label_publicKey.isVisible());
    }

    // Öffnet die Dateiauswahl, um den Public-Key einzulesen.
    public void loadPublicKey() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PUBKEY (.pubKey)", "*.pubKey"));
        fc.setTitle("Load public key for this contact..");

        publicKeyFile = fc.showOpenDialog(new Stage());
        if (publicKeyFile != null) {
            label_publicKey.setText("Public Key: " + publicKeyFile.getName());
        }
    }

    // Fügt einen neuen Kontakt hinzu.
    // Es wird unterschieden zwischen einem Kontakt der den Key Exchange einleitet und einem Kontakt der ihn empfängt.
    // (siehe DiffieHellman.class)
    public void addUser() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, IOException {
        if (textField_UserName.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please enter a name.");
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
                    System.out.println("Error while writing public key to file: " + e.toString());
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
                    System.out.println("Error while writing public key to file: " + e.toString());
                }
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Please load this persons public key file.");
            alert.showAndWait();
        }
    }
}
