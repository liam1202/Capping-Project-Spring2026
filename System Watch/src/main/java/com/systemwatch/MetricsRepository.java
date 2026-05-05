package com.systemwatch;

import com.systemwatch.db.DatabaseManager;
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
        try (Connection conn = DatabaseManager.getConnection()) {
            collectAll(conn); 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Overload with specified conection
    public void collectAll(Connection conn) {
    try {
        long timestamp = System.currentTimeMillis();

        insertCpuMetrics(conn, timestamp);
        insertRamMetrics(conn, timestamp);
        insertDiskMetrics(conn, timestamp);
        insertProcessMetrics(conn, timestamp);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    // CPU INSERT
    public void insertCpuMetrics(Connection conn, long time) throws Exception {
        String sql = "INSERT INTO cpu " +
                "(timestamp, cpu_usage_percentage, interrupts, user_mode_time, kernel_mode_time, thread_count) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, time);
            ps.setDouble(2, metrics.getCpuUsage());
            ps.setLong(3, metrics.getInterrupts());
            ps.setDouble(4, metrics.getUserTime());
            ps.setDouble(5, metrics.getKernelTime());
            ps.setInt(6, metrics.getThreadCount());

            ps.executeUpdate();
        }
    }

    // RAM INSERT
    public void insertRamMetrics(Connection conn, long time) throws Exception {
        String sql = "INSERT INTO ram " +
                "(timestamp, total_memory_bytes, used_memory_bytes, cached_memory_bytes, page_faults) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, time);
            ps.setLong(2, metrics.getTotalMemory());
            ps.setLong(3, metrics.getUsedMemory());

            // OSHI does not provide true cache mapping, so estimated using available memory
            ps.setLong(4, metrics.getAvailableMemory());

            ps.setLong(5, metrics.getSwapUsed());

            ps.executeUpdate();
        }
    }

    // DISK INSERT
    public void insertDiskMetrics(Connection conn, long time) throws Exception {
        String sql = "INSERT INTO disk " +
                "(timestamp, disk_id, disk_total_bytes, disk_used_bytes, disk_free_bytes) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

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
    public void insertProcessMetrics(Connection conn, long time) throws Exception {
        String sql = "INSERT INTO " +
                "process (timestamp, pid, process_name, cpu_percent, ram_percent, disk_percent, marked_for_suspension, valid_for_tracking) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

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