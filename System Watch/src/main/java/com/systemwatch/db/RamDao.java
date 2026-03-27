package com.systemwatch.db;

import com.systemwatch.model.RamRecord;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RamDao {

    public List<RamRecord> getLatest(int limit) throws Exception {
        String sql = "SELECT * FROM ram ORDER BY timestamp DESC LIMIT ?";

        List<RamRecord> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                RamRecord r = new RamRecord();
                r.timestamp = rs.getLong("timestamp");
                r.total = rs.getLong("total_memory_bytes");
                r.used = rs.getLong("used_memory_bytes");
                r.cached = rs.getLong("cached_memory_bytes");
                r.pageFaults = rs.getLong("page_faults");

                list.add(r);
            }
        }

        return list;
    }
}