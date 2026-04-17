package com.systemwatch;

import com.systemwatch.db.DatabaseManager;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {

    @Test
    void testFullDataFlow() throws Exception {

        // Creates repository which connects OS metrics collection to database
        MetricsRepository repo = new MetricsRepository();

        // Opens database connection
        try (Connection conn = DatabaseManager.getConnection()) {

            // Collects real-time system metrics
            repo.collectAll();

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
            System.out.println("Integration Test Successful!");
        }
    }
}