package com.morrison.ip_scanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class IpScannerApplication extends Application {
    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IpScannerApplication.class.getResource("ipScanner-view.fxml"));
        Parent root = fxmlLoader.load();
        Image icon = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icon/ipScannerLogo.png")));
        primaryStage.setTitle("IP Scanner App");
        primaryStage.getIcons().add(icon);
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}