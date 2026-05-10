package com.systemwatch.db;

import com.systemwatch.model.ProcessRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
// Data Access Object (DAO) for process metrics, responsible for retrieving process records from the database
public class ProcessDao {
    // Retrieves the latest process records from the database, ordered by timestamp in descending order, limited by the specified number of records
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
 // Retrieves the current processes by joining the process table with a subquery that selects the latest timestamp for each process ID, effectively giving the most recent record for each process
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
    // Retrieves the historical records for a specific process ID, ordered by timestamp in descending order, limited by the specified number of records
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