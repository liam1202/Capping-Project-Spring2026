package com.systemwatch;

import com.systemwatch.db.DatabaseManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testFullDataFlow() throws Exception {
        Path databaseFile = tempDir.resolve("integration-test.db");
        System.setProperty("systemwatch.db.path", databaseFile.toString());
        DatabaseManager.resetDatabase();
        DatabaseManager.initDatabase();

        // Creates repository which connects OS metrics collection to database
        MetricsRepository repo = new MetricsRepository();

        // Opens database connection
        try (Connection conn = DatabaseManager.getConnection()) {

            // Collects real-time system metrics
            repo.collectAll(conn);

            // Validate all tables received data
            String[] tables = {"cpu", "ram", "disk", "process"};

            // Iterates through each table to validate that data insertion occurred correctly
            for (String table : tables) {
                ResultSet rs = conn.createStatement()
                        .executeQuery("SELECT COUNT(*) as count FROM " + table);

                // Make sure query returned a result
                assertTrue(rs.next());

                // Make sure at least one record exists in this table
                assertTrue(rs.getInt("count") > 0,
                        table + " should have records");

                System.out.println("Insertion was successful on " + table + " Table");
            }
            

            // Closes database connection
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }

            System.out.println("Integration Test Successful!");
        }


    }

}