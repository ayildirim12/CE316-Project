package com.iae.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Font.loadFont(getClass().getResourceAsStream("/com/iae/fonts/MaterialIcons-Regular.ttf"), 14);
        Font.loadFont(getClass().getResourceAsStream("/com/iae/fonts/Inter-Regular.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/com/iae/fonts/Inter-Medium.ttf"), 13);
        Font.loadFont(getClass().getResourceAsStream("/com/iae/fonts/Inter-SemiBold.ttf"), 13);

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/iae/fxml/MainWindow.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("Integrated Assignment Environment");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
        
    }
}
