package com.systemwatch;

import com.systemwatch.db.DatabaseManager;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class MetricsRepositoryTest {

    private Connection conn;
    private MetricsRepository repo;

    @BeforeEach
    void setup() throws Exception {
        conn = DatabaseManager.getConnection();
        repo = new MetricsRepository();
    }

    // Test insertion of CPU Metrics
    @Test
    void testInsertCpuMetrics() throws Exception {
        repo.insertCpuMetrics(conn, System.currentTimeMillis());

        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM cpu");

        assertTrue(rs.next(), "CPU record should exist");
        assertTrue(rs.getDouble("cpu_usage_percentage") >= 0);

        System.out.println("Successful!");
    }

    // Test insertion of RAM Metrics
    @Test
    void testInsertRamMetrics() throws Exception {
        repo.insertRamMetrics(conn, System.currentTimeMillis());

        ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM ram");

        assertTrue(rs.next());
        assertTrue(rs.getLong("total_memory_bytes") > 0);

        System.out.println("Successful!");
    }

    // Test insertion of Disk Metrics
    @Test
    void testInsertDiskMetrics() throws Exception {
        repo.insertDiskMetrics(conn, System.currentTimeMillis());

        ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) as count FROM disk");
        assertTrue(rs.next());
        assertTrue(rs.getInt("count") > 0, "Disk records should be inserted");

        System.out.println("Successful!");
    }

    // Test insertion of Process Metrics
    @Test
    void testInsertProcessMetrics() throws Exception {
        repo.insertProcessMetrics(conn, System.currentTimeMillis());

        ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) as count FROM process");
        assertTrue(rs.next());
        assertTrue(rs.getInt("count") > 0, "Process records should be inserted");

        System.out.println("Successful!");
    }

    // Tests data integrity after insertion
    @Test
    void testDataIntegrityAfterInsert() throws Exception {
        long time = System.currentTimeMillis();

        // Inserts CPU metrics into the database using the generated timestamp
        repo.insertCpuMetrics(conn, time);

        // Retrieves the most recently inserted CPU record based on timestamp
        ResultSet rs = conn.createStatement().executeQuery("SELECT timestamp FROM cpu ORDER BY timestamp DESC LIMIT 1");

        // At least one record must exist
        assertTrue(rs.next());

        // Validates that the stored timestamp matches the inserted timestamp
        assertEquals(time, rs.getLong("timestamp"));

        System.out.println("Successful!");
    }

    @AfterEach
    void teardown() throws Exception {
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }
}