package com.systemwatch.db;

import com.systemwatch.model.CpuRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CpuDao {

    public List<CpuRecord> getLatest(int limit) throws Exception {
        String sql = "SELECT * FROM cpu ORDER BY timestamp DESC LIMIT ?";

        List<CpuRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CpuRecord r = new CpuRecord();
                r.timestamp = rs.getLong("timestamp");
                r.usage = rs.getDouble("cpu_usage_percentage");
                r.interrupts = rs.getLong("interrupts");
                r.userTime = rs.getDouble("user_mode_time");
                r.kernelTime = rs.getDouble("kernel_mode_time");
                r.threadCount = rs.getInt("thread_count");

                list.add(r);
            }
        }

        return list;
    }
}