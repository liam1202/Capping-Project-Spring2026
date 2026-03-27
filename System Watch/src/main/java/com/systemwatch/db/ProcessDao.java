package com.systemwatch.db;

import com.systemwatch.model.ProcessRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProcessDao {

    public List<ProcessRecord> getLatest(int limit) throws Exception {
        String sql = "SELECT * FROM process ORDER BY timestamp DESC LIMIT ?";

        List<ProcessRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ProcessRecord r = new ProcessRecord();
                r.timestamp = rs.getLong("timestamp");
                r.pid = rs.getInt("pid");
                r.name = rs.getString("process_name");
                r.cpuPercent = rs.getDouble("cpu_percent");
                r.ramPercent = rs.getDouble("ram_percent");
                r.diskPercent = rs.getDouble("disk_percent");
                r.markedForSuspension = rs.getInt("marked_for_suspension");

                list.add(r);
            }
        }

        return list;
    }

    public List<ProcessRecord> getCurrentProcesses() throws Exception {
        String sql = "SELECT p.* FROM process p INNER JOIN (SELECT pid, MAX(timestamp) as max_ts FROM process GROUP BY pid) latest ON p.pid = latest.pid AND p.timestamp = latest.max_ts";

        List<ProcessRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ProcessRecord r = new ProcessRecord();
                r.timestamp = rs.getLong("timestamp");
                r.pid = rs.getInt("pid");
                r.name = rs.getString("process_name");
                r.cpuPercent = rs.getDouble("cpu_percent");
                r.ramPercent = rs.getDouble("ram_percent");
                r.diskPercent = rs.getDouble("disk_percent");
                r.markedForSuspension = rs.getInt("marked_for_suspension");

                list.add(r);
            }
        }

        return list;
    }

    public List<ProcessRecord> getHistoryForPid(int pid, int limit) throws Exception {
        String sql = "SELECT * FROM process WHERE pid = ? ORDER BY timestamp DESC LIMIT ?";

        List<ProcessRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, pid);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                ProcessRecord r = new ProcessRecord();
                r.timestamp = rs.getLong("timestamp");
                r.pid = rs.getInt("pid");
                r.name = rs.getString("process_name");
                r.cpuPercent = rs.getDouble("cpu_percent");
                r.ramPercent = rs.getDouble("ram_percent");
                r.diskPercent = rs.getDouble("disk_percent");
                r.markedForSuspension = rs.getInt("marked_for_suspension");

                list.add(r);
            }
        }

        return list;
    }
}