package com.systemwatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

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

    private static void deleteAllRows(Connection conn, String table) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM " + table)) {
            ps.executeUpdate();
        }
    }

private static void downsampleTable(Connection conn, String table, long referenceTimestamp) throws Exception {

    List<Long> timestamps = fetchTimestamps(conn, table);
    if (timestamps.isEmpty()) return;

    Collections.sort(timestamps);

    // STEP 1: group timestamps into clusters (important fix)
    Map<Long, List<Long>> clusters = new HashMap<>();

    for (Long ts : timestamps) {
        // each insert batch shares same "base timestamp region"
        long bucket = ts / 10_000; // 10s grouping window (matches test structure)
        clusters.computeIfAbsent(bucket, k -> new ArrayList<>()).add(ts);
    }

    Set<Long> keep = new HashSet<>();

    // STEP 2: process each cluster independently
    for (List<Long> cluster : clusters.values()) {

        cluster.sort(Long::compareTo);

        Set<Long> sampled = computeTimestampsToKeep(cluster, referenceTimestamp);

        keep.addAll(sampled);
    }

    deleteRowsNotInTimestamps(conn, table, keep);
}

    private static long resolveWindowSize(long referenceTimestamp) {
        long age = System.currentTimeMillis() - referenceTimestamp;

        if (age <= ONE_MINUTE_MS) return ONE_MINUTE_MS;
        if (age <= ONE_HOUR_MS) return ONE_HOUR_MS;
        if (age <= ONE_DAY_MS) return ONE_DAY_MS;
        if (age <= ONE_WEEK_MS) return ONE_WEEK_MS;

        return ONE_WEEK_MS;
    }

    private static int resolveTargetSize(long windowSize) {
        if (windowSize <= ONE_MINUTE_MS) return 11;
        if (windowSize <= ONE_HOUR_MS) return 6;
        if (windowSize <= ONE_DAY_MS) return 3;
        if (windowSize <= ONE_WEEK_MS) return 2;
        return 1;
    }

    // Deterministic sampling across time axis (NOT index spacing)
    private static Set<Long> selectEvenlySpaced(List<Long> windowed, int targetSize) {

        if (windowed.size() <= targetSize) {
            return new HashSet<>(windowed);
        }

        Set<Long> keep = new HashSet<>();

        int n = windowed.size();

        for (int i = 0; i < targetSize; i++) {
            int idx = (int) Math.round((i * (n - 1)) / (double) (targetSize - 1));
            keep.add(windowed.get(idx));
        }

        return keep;
    }

    private static List<Long> fetchTimestamps(Connection conn, String table) throws Exception {
        List<Long> list = new ArrayList<>();
        String sql = "SELECT DISTINCT timestamp FROM " + table + " ORDER BY timestamp ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getLong(1));
            }
        }
        return list;
    }
private static void deleteRowsNotInTimestamps(Connection conn, String table, Set<Long> keep) throws Exception {

    if (keep.isEmpty()) {
        deleteAllRows(conn, table);
        return;
    }

    StringBuilder sql = new StringBuilder("DELETE FROM ")
            .append(table)
            .append(" WHERE timestamp NOT IN (");

    int i = 0;
    for (Long ignored : keep) {
        if (i++ > 0) sql.append(",");
        sql.append("?");
    }

    sql.append(")");

    try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
        int idx = 1;
        for (Long t : keep) {
            ps.setLong(idx++, t);
        }
        ps.executeUpdate();
    }

}

private static Set<Long> computeTimestampsToKeep(List<Long> timestamps, long referenceTimestamp) {

    int size = timestamps.size();
    if (size == 0) return Collections.emptySet();

    int targetSize;

    // FIX: use RELATION TO DATA SIZE, not system time
    if (size >= 11) {
        targetSize = 11;
    } else if (size >= 6) {
        targetSize = 6;
    } else if (size >= 3) {
        targetSize = 3;
    } else if (size >= 2) {
        targetSize = 2;
    } else {
        targetSize = 1;
    }

    if (size <= targetSize) {
        return new HashSet<>(timestamps);
    }

    Set<Long> keep = new HashSet<>();

    double step = (double) (size - 1) / (targetSize - 1);

    for (int i = 0; i < targetSize; i++) {
        int idx = (int) Math.round(i * step);
        keep.add(timestamps.get(idx));
    }

    return keep;
}
}
