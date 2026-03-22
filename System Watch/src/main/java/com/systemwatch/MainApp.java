package com.systemwatch;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            System.out.println("MainApp: start entered");

            MainView mainView = new MainView();
            System.out.println("MainApp: MainView created");

            Scene scene = new Scene(mainView.getRoot(), 1300, 800);
            System.out.println("MainApp: Scene created");

            stage.setTitle("System Watch");
            stage.setScene(scene);
            stage.setMinWidth(1100);
            stage.setMinHeight(700);
            stage.show();

            System.out.println("MainApp: stage shown");
        } catch (Throwable t) {
            System.err.println("Startup failure:");
            t.printStackTrace();
        }
    }

    public static void main(String[] args) {

        launch(args);
    }
}