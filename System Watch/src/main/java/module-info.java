module com.systemwatch {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    exports com.systemwatch;
    exports com.systemwatch.db;
    opens com.systemwatch to javafx.fxml;
}