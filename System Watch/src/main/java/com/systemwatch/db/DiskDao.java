package com.systemwatch.db;

import com.systemwatch.model.DiskRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DiskDao {

    public List<DiskRecord> getLatest(int limit) throws Exception {
        String sql = "SELECT * FROM disk ORDER BY timestamp DESC LIMIT ?";

        List<DiskRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DiskRecord r = new DiskRecord();
                r.timestamp = rs.getLong("timestamp");
                r.diskId = rs.getString("disk_id");
                r.total = rs.getLong("disk_total_bytes");
                r.used = rs.getLong("disk_used_bytes");
                r.free = rs.getLong("disk_free_bytes");

                list.add(r);
            }
        }

        return list;
    }
}