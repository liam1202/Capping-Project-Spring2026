package com.systemwatch;

import com.systemwatch.GatherMetrics;
import com.systemwatch.db.DatabaseManager;
import com.systemwatch.model.CpuRecord;
import com.systemwatch.model.RamRecord;
import com.systemwatch.model.DiskRecord;
import com.systemwatch.model.ProcessRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;

public class MetricsRepository {
    private final GatherMetrics metrics;

    // Get Metrics information from OS
    public MetricsRepository() {
        this.metrics = new GatherMetrics();
    }

    // Populates database with OS statistics
    public void collectAll() {
        try {
            insertCpuMetrics();
            insertRamMetrics();
            insertDiskMetrics();
            insertProcessMetrics();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CPU INSERT
    public void insertCpuMetrics() throws Exception {
        String sql = "INSERT INTO cpu VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, System.currentTimeMillis());
            ps.setDouble(2, metrics.getCpuUsage());
            ps.setLong(3, metrics.getInterrupts());
            ps.setDouble(4, metrics.getUserTime());
            ps.setDouble(5, metrics.getKernelTime());
            ps.setInt(6, metrics.getThreadCount());

            ps.executeUpdate();
        }
    }

    // RAM INSERT
    public void insertRamMetrics() throws Exception {
        String sql = "INSERT INTO ram VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, System.currentTimeMillis());
            ps.setLong(2, metrics.getTotalMemory());
            ps.setLong(3, metrics.getUsedMemory());
            ps.setLong(4, metrics.getSwapUsed());
            ps.setLong(5, metrics.getSwapPagesIn());

            ps.executeUpdate();
        }
    }

    // DISK INSERT
    public void insertDiskMetrics() throws Exception {
        String sql = "INSERT INTO disk VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            long time = System.currentTimeMillis();

            for (GatherMetrics.DiskMetrics d : metrics.getDiskMetrics()) {

                ps.setLong(1, time);
                ps.setString(2, d.id);
                ps.setLong(3, d.total);
                ps.setLong(4, d.used);
                ps.setLong(5, d.free);

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // PROCESS INSERT
    public void insertProcessMetrics() throws Exception {
        String sql = "INSERT INTO process VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            long time = System.currentTimeMillis();

            for (GatherMetrics.ProcessMetrics p : metrics.getProcessMetrics()) {
                ps.setLong(1, time);
                ps.setInt(2, p.pid);
                ps.setString(3, p.name);
                ps.setDouble(4, p.cpu);
                ps.setDouble(5, p.ram);
                ps.setDouble(6, p.disk);
                ps.setInt(7, 0);
                ps.setInt(8, 1);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }
}