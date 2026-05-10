package com.systemwatch;

import com.systemwatch.db.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

// Test class for DatabasePopulator, responsible for testing the population of demo data and the data retention granularity sampling logic
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabasePopulatorTest {

    private static final long ONE_MINUTE_MS = 60L * 1000;
    private static final long ONE_HOUR_MS = 60L * ONE_MINUTE_MS;
    private static final long ONE_DAY_MS = 24L * ONE_HOUR_MS;
    private static final long ONE_WEEK_MS = 7L * ONE_DAY_MS;

    private long now;

    // Set up a temporary directory for the database to ensure test isolation and prevent interference with any existing databases on the system
    @TempDir
    Path tempDir;

    // Set up the database before each test
    @BeforeEach
    void setupDatabase() throws Exception {
        Path databaseFile = tempDir.resolve("test-systemwatch.db");
        System.setProperty("systemwatch.db.path", databaseFile.toString());
        DatabaseManager.resetDatabase();
        DatabaseManager.initDatabase();
        now = System.currentTimeMillis();
    }

    // Clean up system properties after each test
    @AfterEach
    void cleanupProperties() throws Exception {
        System.clearProperty("systemwatch.db.path");
    }

    // Test the population of demo data and verify that records are inserted into all tables
    @Test
    void testPopulateAndVerifyData() throws Exception {
        DatabasePopulator.populateDemoData();

        try (Connection conn = DatabaseManager.getConnection()) {
            assertTrue(countRows(conn, "cpu") > 0);
            assertTrue(countRows(conn, "ram") > 0);
            assertTrue(countRows(conn, "disk") > 0);
            assertTrue(countRows(conn, "process") > 0);
        }
    }

    // Test the data retention granularity sampling logic by inserting records at different timestamps and 
    // verifying that the correct number of records are retained based on the defined retention policy
    @Test
    void testDataRetentionGranularitySampling() throws Exception {
        long recentBase = now - (30L * 1000);
        long halfBase = now - (5L * ONE_MINUTE_MS);
        long quarterBase = now - (2L * ONE_HOUR_MS);
        long eighthBase = now - (2L * ONE_DAY_MS);
        long sixteenthBase = now - (10L * ONE_DAY_MS);

        try (Connection conn = DatabaseManager.getConnection()) {
            // insert 11 records
            // expected for each granularity
            // No granularity: 11 records
            // 1 minute: 6 records (1/2 of original)
            // 1 hour: 3 records (1/4 of original)
            // 1 day: 2 records (1/8 of original)
            // 1 week: 1 record (1/16 of original)
            insertCpuRecords(conn, recentBase, 11);
            insertRamRecords(conn, recentBase, 11);
            insertDiskRecords(conn, recentBase, 11);
            insertProcessRecords(conn, recentBase, 11);

            insertCpuRecords(conn, halfBase, 11);
            insertRamRecords(conn, halfBase, 11);
            insertDiskRecords(conn, halfBase, 11);
            insertProcessRecords(conn, halfBase, 11);

            insertCpuRecords(conn, quarterBase, 11);
            insertRamRecords(conn, quarterBase, 11);
            insertDiskRecords(conn, quarterBase, 11);
            insertProcessRecords(conn, quarterBase, 11);

            insertCpuRecords(conn, eighthBase, 11);
            insertRamRecords(conn, eighthBase, 11);
            insertDiskRecords(conn, eighthBase, 11);
            insertProcessRecords(conn, eighthBase, 11);

            insertCpuRecords(conn, sixteenthBase, 11);
            insertRamRecords(conn, sixteenthBase, 11);
            insertDiskRecords(conn, sixteenthBase, 11);
            insertProcessRecords(conn, sixteenthBase, 11);
        }

        // Executes the data retention logic to downsample old records based on the defined retention policy
        try (Connection conn = DatabaseManager.getConnection()) {
            DataRetentionManager.deleteOldData(conn, now);
        }
        // Verifies that the correct number of records are retained in each table based on the defined retention policy, 
        // ensuring that more recent data is kept at higher granularity while older data is downsampled appropriately
        try (Connection conn = DatabaseManager.getConnection()) {
            verifySampledWindow(conn, "cpu", "CPU", recentBase);
            verifySampledWindow(conn, "ram", "RAM", recentBase);
            verifySampledWindow(conn, "disk", "Disk", recentBase);
            verifySampledWindow(conn, "process", "Process", recentBase);

            verifySampledWindow(conn, "cpu", "CPU", halfBase);
            verifySampledWindow(conn, "ram", "RAM", halfBase);
            verifySampledWindow(conn, "disk", "Disk", halfBase);
            verifySampledWindow(conn, "process", "Process", halfBase);

            verifySampledWindow(conn, "cpu", "CPU", quarterBase);
            verifySampledWindow(conn, "ram", "RAM", quarterBase);
            verifySampledWindow(conn, "disk", "Disk", quarterBase);
            verifySampledWindow(conn, "process", "Process", quarterBase);

            verifySampledWindow(conn, "cpu", "CPU", eighthBase);
            verifySampledWindow(conn, "ram", "RAM", eighthBase);
            verifySampledWindow(conn, "disk", "Disk", eighthBase);
            verifySampledWindow(conn, "process", "Process", eighthBase);

            verifySampledWindow(conn, "cpu", "CPU", sixteenthBase);
            verifySampledWindow(conn, "ram", "RAM", sixteenthBase);
            verifySampledWindow(conn, "disk", "Disk", sixteenthBase);
            verifySampledWindow(conn, "process", "Process", sixteenthBase);
        }
    }

    // Helper method to verify the number of records retained in a specific time window based on the defined retention policy
    private void verifySampledWindow(Connection conn, String table, String label, long base) throws Exception {
        int expectedCount;
        if (base >= now - ONE_MINUTE_MS) {
            expectedCount = 11;
        } else if (base >= now - ONE_HOUR_MS) {
            expectedCount = 6;
        } else if (base >= now - ONE_DAY_MS) {
            expectedCount = 3;
        } else if (base >= now - ONE_WEEK_MS) {
            expectedCount = 2;
        } else {
            expectedCount = 1;
        }

        long actual = countRowsBetween(conn, table, base, base + 10_500);
        assertEquals(expectedCount, actual,
                label + " table should have " + expectedCount + " rows in the sampled window.");
        System.out.println(label.toLowerCase() + " test: SUCCESS for base=" + base + " expected=" + expectedCount);
    }

    // Helper methods to count the number of records in a specific table
    private long countRows(Connection conn, String table) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    // Helper method to count the number of records in a specific time window
    private long countRowsBetween(Connection conn, String table, long minTimestamp, long maxTimestamp) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE timestamp >= ? AND timestamp <= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, minTimestamp);
            ps.setLong(2, maxTimestamp);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    // Helper method to check if a specific timestamp exists in a table, used to verify that the correct records are retained after downsampling
    private boolean timestampExists(Connection conn, String table, long timestamp) throws Exception {
        String sql = "SELECT 1 FROM " + table + " WHERE timestamp = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Helper methods to insert records into each table
    private void insertCpuRecords(Connection conn, long baseTimestamp, int count) throws Exception {
        String sql = "INSERT OR REPLACE INTO cpu (timestamp, cpu_usage_percentage, interrupts, user_mode_time, kernel_mode_time, thread_count) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                ps.setLong(1, baseTimestamp + (i * 1000));
                ps.setDouble(2, 50.0);
                ps.setLong(3, 1000);
                ps.setDouble(4, 30.0);
                ps.setDouble(5, 20.0);
                ps.setInt(6, 150);
                ps.executeUpdate();
            }
        }
    }

    // Helper methods to insert records into each table
    private void insertRamRecords(Connection conn, long baseTimestamp, int count) throws Exception {
        String sql = "INSERT OR REPLACE INTO ram (timestamp, total_memory_bytes, used_memory_bytes, cached_memory_bytes, page_faults) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                ps.setLong(1, baseTimestamp + (i * 1000));
                ps.setLong(2, 16_384L * 1024);
                ps.setLong(3, 8_192L * 1024);
                ps.setLong(4, 2_048L * 1024);
                ps.setLong(5, 120 + i);
                ps.executeUpdate();
            }
        }
    }

    // Helper methods to insert records into each table
    private void insertDiskRecords(Connection conn, long baseTimestamp, int count) throws Exception {
        String sql = "INSERT OR REPLACE INTO disk (timestamp, disk_id, disk_total_bytes, disk_used_bytes, disk_free_bytes) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                ps.setLong(1, baseTimestamp + (i * 1000));
                ps.setString(2, "disk" + i);
                ps.setLong(3, 512L * 1024 * 1024);
                ps.setLong(4, 256L * 1024 * 1024);
                ps.setLong(5, 256L * 1024 * 1024);
                ps.executeUpdate();
            }
        }
    }

    // Helper methods to insert records into each table
    private void insertProcessRecords(Connection conn, long baseTimestamp, int count) throws Exception {
        String sql = "INSERT OR REPLACE INTO process (timestamp, pid, process_name, cpu_percent, ram_percent, disk_percent, marked_for_suspension, valid_for_tracking) VALUES (?, ?, ?, ?, ?, ?, 0, 1)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < count; i++) {
                ps.setLong(1, baseTimestamp + (i * 1000));
                ps.setInt(2, 1000 + i);
                ps.setString(3, "process" + i);
                ps.setDouble(4, 1.0 + i);
                ps.setDouble(5, 2.0 + i);
                ps.setDouble(6, 0.5 + i);
                ps.executeUpdate();
            }
        }
    }
}
