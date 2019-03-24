package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Cryptor");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();

        UserAdministration userAdministration = new UserAdministration();

        int i = 0;
        userAdministration.createUser(i++, "Test", "12345");
        userAdministration.createUser(i++, "Test", "12345");
        userAdministration.createUser(i++, "Test", "12345");
        userAdministration.createUser(i++, "Test", "12345");

        userAdministration.saveUsers();

        userAdministration.setUsers();

    }

    public static void main(String[] args) {
        launch(args);
    }
}
