module com.systemwatch {
    requires javafx.controls;
    requires javafx.fxml;
    //requires com.github.oshi;
    requires java.sql;
    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires java.desktop;

    exports com.systemwatch;
    exports com.systemwatch.db;
    opens com.systemwatch to javafx.fxml;
}