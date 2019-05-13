package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.util.logging.FileHandler;

public class Cryptor extends Application {

    private final String CRYPTOR_HOME = System.getProperty("user.home") + "/cryptor";

    @Override
    public void start(Stage primaryStage) throws Exception {
        setupEnv();

        Parent root = FXMLLoader.load(getClass().getResource("View.fxml"));
        primaryStage.setTitle("cryptor");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void setupEnv() {
        File cryptorPath = new File(CRYPTOR_HOME);

        if (!cryptorPath.exists()) {
            try {
                cryptorPath.mkdir();
            } catch (Exception e) {
                System.out.println("Error while setting up environment: " + e.toString());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
