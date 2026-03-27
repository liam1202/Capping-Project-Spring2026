package com.systemwatch;
import com.systemwatch.db.DatabaseManager;
import com.systemwatch.DatabasePopulator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;


public class MainApp extends Application {

    @Override
public void start(Stage stage) {
    try {
        DatabaseManager.initDatabase();

        if (DatabaseManager.isProcessTableEmpty()) {
            DatabasePopulator.populateDemoData();
        }

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

        // Create Metrics
        GatherMetrics metrics = new GatherMetrics();

        // TYLER:
        // THE FOLLOWING IS TEST CODE TO SEE IF GATHERING METRICS WORKS CORRECTLY

        System.out.println("\n--------------------------------------");
        System.out.println("BASIC METRICS:");

        // Print system uptime
        System.out.println("System Uptime: " + metrics.getUptime() + " seconds");

        // Gets processor information
        System.out.println("Processor Information: " + metrics.getProcessorInfo());

        // Gets memory information
        System.out.println("Total Memory: " + metrics.getTotalMemory());
        System.out.println("Available Memory: " + metrics.getAvailableMemory());

        System.out.println("Disk Models: " + metrics.getDiskModels());

        System.out.println("--------------------------------------\n");

        launch(args);
    }
}