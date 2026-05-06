package com.systemwatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DataRetentionManager {

    private static final long ONE_MINUTE_MS = 60L * 1000;
    private static final long ONE_HOUR_MS = 60L * ONE_MINUTE_MS;
    private static final long ONE_DAY_MS = 24L * ONE_HOUR_MS;
    private static final long ONE_WEEK_MS = 7L * ONE_DAY_MS;

    public static void deleteOldData(Connection conn, long referenceTimestamp) throws Exception {
        if (referenceTimestamp == Long.MAX_VALUE) {
            deleteAllData(conn);
            return;
        }

        conn.setAutoCommit(false);

        downsampleTable(conn, "cpu", referenceTimestamp);
        downsampleTable(conn, "ram", referenceTimestamp);
        downsampleTable(conn, "disk", referenceTimestamp);
        downsampleTable(conn, "process", referenceTimestamp);

        conn.commit();
    }

    public static void deleteAllData(Connection conn) throws Exception {
        deleteAllRows(conn, "cpu");
        deleteAllRows(conn, "ram");
        deleteAllRows(conn, "disk");
        deleteAllRows(conn, "process");
    }

    private static void deleteAllRows(Connection conn, String tableName) throws Exception {
        String sql = "DELETE FROM " + tableName;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    private static void downsampleTable(Connection conn, String tableName, long referenceTimestamp) throws Exception {
        List<Long> timestamps = fetchTimestamps(conn, tableName);
        Set<Long> keepTimestamps = computeTimestampsToKeep(timestamps, referenceTimestamp);
        deleteRowsNotInTimestamps(conn, tableName, keepTimestamps);
    }

    private static List<Long> fetchTimestamps(Connection conn, String tableName) throws Exception {
        List<Long> timestamps = new ArrayList<>();
        String sql = "SELECT DISTINCT timestamp FROM " + tableName + " ORDER BY timestamp ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                timestamps.add(rs.getLong(1));
            }
        }

        return timestamps;
    }

    private static Set<Long> computeTimestampsToKeep(List<Long> timestamps, long referenceTimestamp) {
        long minuteBoundary = referenceTimestamp - ONE_MINUTE_MS;
        long hourBoundary = referenceTimestamp - ONE_HOUR_MS;
        long dayBoundary = referenceTimestamp - ONE_DAY_MS;
        long weekBoundary = referenceTimestamp - ONE_WEEK_MS;

        List<Long> recent = new ArrayList<>();
        List<Long> half = new ArrayList<>();
        List<Long> quarter = new ArrayList<>();
        List<Long> eighth = new ArrayList<>();
        List<Long> sixteenth = new ArrayList<>();

        for (Long timestamp : timestamps) {
            if (timestamp >= minuteBoundary) {
                recent.add(timestamp);
            } else if (timestamp >= hourBoundary) {
                half.add(timestamp);
            } else if (timestamp >= dayBoundary) {
                quarter.add(timestamp);
            } else if (timestamp >= weekBoundary) {
                eighth.add(timestamp);
            } else {
                sixteenth.add(timestamp);
            }
        }

        Set<Long> keepTimestamps = new HashSet<>();
        addKeptTimestamps(keepTimestamps, recent, 1);
        addKeptTimestamps(keepTimestamps, half, 2);
        addKeptTimestamps(keepTimestamps, quarter, 4);
        addKeptTimestamps(keepTimestamps, eighth, 8);
        addKeptTimestamps(keepTimestamps, sixteenth, 16);

        return keepTimestamps;
    }

    private static void addKeptTimestamps(Set<Long> keepTimestamps, List<Long> timestamps, int sampling) {
        for (int i = 0; i < timestamps.size(); i += sampling) {
            keepTimestamps.add(timestamps.get(i));
        }
    }

    private static void deleteRowsNotInTimestamps(Connection conn, String tableName, Set<Long> keepTimestamps) throws Exception {
        if (keepTimestamps.isEmpty()) {
            deleteAllRows(conn, tableName);
            return;
        }

        StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName).append(" WHERE timestamp NOT IN (");
        for (int i = 0; i < keepTimestamps.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int index = 1;
            for (Long timestamp : keepTimestamps) {
                ps.setLong(index++, timestamp);
            }
            ps.executeUpdate();
        }
    }
}
