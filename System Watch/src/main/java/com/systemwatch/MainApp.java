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
        DatabaseManager.resetDatabase();
        DatabaseManager.initDatabase();

        // Gets repository which connects backend OS metric gathering to database
        MetricsRepository repo = new MetricsRepository();

        // SET TO FALSE IF YOU WANT TO TEST MOCK DATA
        boolean showRealData = true;

        // SHOWS REAL DATA FROM THE OS
        if (showRealData) {
            // START DATA FIRST
            repo.collectAll();

            // Using a thread, will collect all metrics every 5 seconds
            Thread metricsThread = new Thread(() -> {
                while (true) {
                    repo.collectAll();

                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Stops when app closes
            metricsThread.setDaemon(true);
            metricsThread.start();
        } else {
            // MOCK DATA
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

        // TESTS METRICS GATHERING
        GatherMetrics metrics = new GatherMetrics();
        metrics.printResults();

        launch(args);
    }
}