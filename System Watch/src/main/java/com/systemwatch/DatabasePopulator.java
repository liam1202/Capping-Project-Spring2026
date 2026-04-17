package com.systemwatch;

import com.systemwatch.db.DatabaseManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Random;

public class DatabasePopulator {

    public static void populateDemoData() throws Exception {
        populateCpuData();
        populateRamData();
        populateDiskData();
        populateProcessData();
    }

    private static void populateCpuData() throws Exception {
        String sql = "INSERT INTO cpu (timestamp, cpu_usage_percentage, interrupts, user_mode_time, kernel_mode_time, thread_count) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Random random = new Random();
            long baseTime = System.currentTimeMillis() - 12 * 60 * 1000; // 12 minutes ago

            for (int i = 0; i < 12; i++) {
                long timestamp = baseTime + i * 60 * 1000; // every minute
                double usage = 20 + random.nextDouble() * 60; // 20-80%
                long interrupts = 1000 + random.nextInt(5000);
                double userTime = usage * 0.6 + random.nextDouble() * 10;
                double kernelTime = usage * 0.4 + random.nextDouble() * 10;
                int threadCount = 100 + random.nextInt(200);

                ps.setLong(1, timestamp);
                ps.setDouble(2, usage);
                ps.setLong(3, interrupts);
                ps.setDouble(4, userTime);
                ps.setDouble(5, kernelTime);
                ps.setInt(6, threadCount);

                ps.executeUpdate();
            }
        }
    }

    private static void populateRamData() throws Exception {
        String sql = "INSERT INTO ram (timestamp, total_memory_bytes, used_memory_bytes, cached_memory_bytes, page_faults) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Random random = new Random();
            long baseTime = System.currentTimeMillis() - 12 * 60 * 1000;
            long totalRam = 16L * 1024 * 1024 * 1024; // 16GB

            for (int i = 0; i < 12; i++) {
                long timestamp = baseTime + i * 60 * 1000;
                long used = (long) (totalRam * (0.3 + random.nextDouble() * 0.5)); // 30-80%
                long cached = (long) (totalRam * (0.1 + random.nextDouble() * 0.2)); // 10-30%
                long pageFaults = 1000 + random.nextInt(5000);

                ps.setLong(1, timestamp);
                ps.setLong(2, totalRam);
                ps.setLong(3, used);
                ps.setLong(4, cached);
                ps.setLong(5, pageFaults);

                ps.executeUpdate();
            }
        }
    }

    private static void populateDiskData() throws Exception {
        String sql = "INSERT INTO disk (timestamp, disk_id, disk_total_bytes, disk_used_bytes, disk_free_bytes) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Random random = new Random();
            long baseTime = System.currentTimeMillis() - 12 * 60 * 1000;
            long totalDisk = 500L * 1024 * 1024 * 1024; // 500GB
            String[] diskIds = {"C:", "D:"};

            for (String diskId : diskIds) {
                for (int i = 0; i < 12; i++) {
                    long timestamp = baseTime + i * 60 * 1000;
                    long used = (long) (totalDisk * (0.2 + random.nextDouble() * 0.6)); // 20-80%
                    long free = totalDisk - used;

                    ps.setLong(1, timestamp);
                    ps.setString(2, diskId);
                    ps.setLong(3, totalDisk);
                    ps.setLong(4, used);
                    ps.setLong(5, free);

                    ps.executeUpdate();
                }
            }
        }
    }

    private static void populateProcessData() throws Exception {
        String sql = "INSERT INTO process (timestamp, pid, process_name, cpu_percent, ram_percent, disk_percent, marked_for_suspension, valid_for_tracking) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            Random random = new Random();
            long baseTime = System.currentTimeMillis() - 12 * 60 * 1000;
            String[] processNames = {"java.exe", "chrome.exe", "firefox.exe", "explorer.exe", "notepad.exe"};
            int[] pids = {1, 2, 3, 4, 5};

            for (int p = 0; p < processNames.length; p++) {
                for (int i = 0; i < 12; i++) {
                    long timestamp = baseTime + i * 60 * 1000;
                    double cpuPercent = random.nextDouble() * 50; // 0-50%
                    double ramPercent = random.nextDouble() * 30; // 0-30%
                    double diskPercent = random.nextDouble() * 10; // 0-10%
                    int marked = random.nextDouble() < 0.1 ? 1 : 0; // 10% chance suspended

                    ps.setLong(1, timestamp);
                    ps.setInt(2, pids[p]);
                    ps.setString(3, processNames[p]);
                    ps.setDouble(4, cpuPercent);
                    ps.setDouble(5, ramPercent);
                    ps.setDouble(6, diskPercent);
                    ps.setInt(7, marked);
                    ps.setInt(8, 1);

                    ps.executeUpdate();
                }
            }
        }
    }
}