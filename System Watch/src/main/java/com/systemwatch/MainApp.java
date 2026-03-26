package com.systemwatch;
import com.systemwatch.db.DatabaseManager;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;


public class MainApp extends Application {

    @Override
public void start(Stage stage) {
    try {
        DatabaseManager.initDatabase();

        MainView mainView = new MainView();
        Scene scene = new Scene(mainView.getRoot(), 1300, 800);

        stage.setTitle("System Watch");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

    } catch (Exception e) {
        e.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("System Watch");
        alert.setHeaderText("Startup Error");
        alert.setContentText("Failed to initialize the database:\n" + e.getMessage());
        alert.showAndWait();
    }
}

    public static void main(String[] args) {
        launch(args);
    }
}
