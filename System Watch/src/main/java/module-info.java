module com.systemwatch {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.github.oshi;

    exports com.systemwatch;
    opens com.systemwatch to javafx.fxml;
}