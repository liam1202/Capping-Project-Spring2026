package com.systemwatch.db;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_FILE_NAME = "systemwatch.db";
    private static final String DB_PATH_PROPERTY = "systemwatch.db.path";

    private static String buildDatabaseUrl() throws Exception {
        Path dbPath = getDatabasePath();
        Files.createDirectories(dbPath.getParent());
        return "jdbc:sqlite:" + dbPath.toAbsolutePath();
    }

    private static Path getDatabasePath() throws Exception {
        String explicitPath = System.getProperty(DB_PATH_PROPERTY);
        if (explicitPath != null && !explicitPath.isBlank()) {
            return Paths.get(explicitPath).toAbsolutePath();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            return Paths.get(appData, "SystemWatch", DB_FILE_NAME).toAbsolutePath();
        }
        return Paths.get(System.getProperty("user.home"), ".systemwatch", DB_FILE_NAME).toAbsolutePath();
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(buildDatabaseUrl());
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        } catch (Exception e) {
            throw new SQLException("Failed to create or access database path", e);
        }
    }

    public static void initDatabase() throws Exception {
        InputStream schemaStream = DatabaseManager.class
                .getClassLoader()
                .getResourceAsStream("schema.sql");

        if (schemaStream == null) {
            throw new IllegalStateException("schema.sql not found in resources");
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(schemaStream, StandardCharsets.UTF_8))) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sql.append(line).append("\n");
            }

            stmt.executeUpdate(sql.toString());
        }
    }

    public static boolean isProcessTableEmpty() throws SQLException {
        String sql = "SELECT COUNT(*) FROM process";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() && rs.getInt(1) == 0;
        }
    }
}