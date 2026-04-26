module com.systemwatch {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.oshi;

    requires org.slf4j;
    requires ch.qos.logback.classic;
    requires java.sql;
    requires java.desktop;

    requires javafx.swing;
    requires org.apache.pdfbox;

    opens com.systemwatch to javafx.fxml;
    exports com.systemwatch;
    exports com.systemwatch.db;
}