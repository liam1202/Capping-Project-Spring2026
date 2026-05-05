package com.systemwatch;

import com.systemwatch.db.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class DatabasePopulator {

    public static void populateDemoData() throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            long timestamp = System.currentTimeMillis();

            insertCpuSample(conn, timestamp, 18.6, 879L, 12.4, 6.2, 64);
            insertRamSample(conn, timestamp, 16_384L * 1024, 7_200L * 1024, 2_400L * 1024, 230L);
            insertDiskSample(conn, timestamp, "C:", 512L * 1024 * 1024, 263L * 1024 * 1024, 249L * 1024 * 1024);
            insertDiskSample(conn, timestamp, "D:", 1_024L * 1024 * 1024, 748L * 1024 * 1024, 276L * 1024 * 1024);
            insertProcessSample(conn, timestamp, 1012, "System", 4.5, 12.3, 0.8);
            insertProcessSample(conn, timestamp, 2218, "Explorer", 2.1, 7.8, 0.3);
            insertProcessSample(conn, timestamp, 3324, "java", 6.9, 19.5, 1.4);

            conn.commit();
        }
    }

    private static void insertCpuSample(Connection conn,
                                        long timestamp,
                                        double cpuUsagePercentage,
                                        long interrupts,
                                        double userModeTime,
                                        double kernelModeTime,
                                        int threadCount) throws Exception {
        String sql = "INSERT OR REPLACE INTO cpu (timestamp, cpu_usage_percentage, interrupts, user_mode_time, kernel_mode_time, thread_count) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            ps.setDouble(2, cpuUsagePercentage);
            ps.setLong(3, interrupts);
            ps.setDouble(4, userModeTime);
            ps.setDouble(5, kernelModeTime);
            ps.setInt(6, threadCount);
            ps.executeUpdate();
        }
    }

    private static void insertRamSample(Connection conn,
                                        long timestamp,
                                        long totalMemoryBytes,
                                        long usedMemoryBytes,
                                        long cachedMemoryBytes,
                                        long pageFaults) throws Exception {
        String sql = "INSERT OR REPLACE INTO ram (timestamp, total_memory_bytes, used_memory_bytes, cached_memory_bytes, page_faults) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            ps.setLong(2, totalMemoryBytes);
            ps.setLong(3, usedMemoryBytes);
            ps.setLong(4, cachedMemoryBytes);
            ps.setLong(5, pageFaults);
            ps.executeUpdate();
        }
    }

    private static void insertDiskSample(Connection conn,
                                         long timestamp,
                                         String diskId,
                                         long totalBytes,
                                         long usedBytes,
                                         long freeBytes) throws Exception {
        String sql = "INSERT OR REPLACE INTO disk (timestamp, disk_id, disk_total_bytes, disk_used_bytes, disk_free_bytes) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            ps.setString(2, diskId);
            ps.setLong(3, totalBytes);
            ps.setLong(4, usedBytes);
            ps.setLong(5, freeBytes);
            ps.executeUpdate();
        }
    }

    private static void insertProcessSample(Connection conn,
                                            long timestamp,
                                            int pid,
                                            String processName,
                                            double cpuPercent,
                                            double ramPercent,
                                            double diskPercent) throws Exception {
        String sql = "INSERT OR REPLACE INTO process (timestamp, pid, process_name, cpu_percent, ram_percent, disk_percent, marked_for_suspension, valid_for_tracking) VALUES (?, ?, ?, ?, ?, ?, 0, 1)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, timestamp);
            ps.setInt(2, pid);
            ps.setString(3, processName);
            ps.setDouble(4, cpuPercent);
            ps.setDouble(5, ramPercent);
            ps.setDouble(6, diskPercent);
            ps.executeUpdate();
        }
    }
}