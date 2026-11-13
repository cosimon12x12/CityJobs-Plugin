package com.cityjobs.database;

import com.cityjobs.CityJobsPlugin;
import com.cityjobs.model.JobType;
import com.cityjobs.model.PlayerJobData;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DatabaseManager {

    private final CityJobsPlugin plugin;
    private final String connectionString;

    public DatabaseManager(CityJobsPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        File dbFile = new File(dataFolder, "cityjobs.db");
        this.connectionString = "jdbc:sqlite:" + dbFile.getAbsolutePath();
    }

    public void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            
            String createTableSQL = "CREATE TABLE IF NOT EXISTS player_jobs (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "job VARCHAR(32), " +
                    "join_timestamp BIGINT, " +
                    "xp DOUBLE, " +
                    "job_level INT, " +
                    "resources_collected INT, " +
                    "money_earned DOUBLE" +
                    ");";
            stmt.execute(createTableSQL);
            plugin.getLogger().info("Database tables verified successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not initialize SQLite database: " + e.getMessage());
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(connectionString);
    }

    public PlayerJobData loadJobDataSync(UUID uuid) {
        PlayerJobData data = new PlayerJobData(uuid);
        String sql = "SELECT * FROM player_jobs WHERE uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String jobStr = rs.getString("job");
                    if (jobStr != null && !jobStr.isBlank()) {
                        JobType.fromString(jobStr).ifPresent(data::setJobType);
                    }
                    data.setJoinTimestamp(rs.getLong("join_timestamp"));
                    data.setXp(rs.getDouble("xp"));
                    data.setLevel(rs.getInt("job_level"));
                    data.setResourcesCollected(rs.getInt("resources_collected"));
                    data.setMoneyEarned(rs.getDouble("money_earned"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load job data for " + uuid + ": " + e.getMessage());
        }
        return data;
    }

    public CompletableFuture<Void> saveJobDataAsync(PlayerJobData data) {
        return CompletableFuture.runAsync(() -> saveJobDataSync(data));
    }

    public void saveJobDataSync(PlayerJobData data) {
        String sql = "INSERT INTO player_jobs (uuid, job, join_timestamp, xp, job_level, resources_collected, money_earned) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(uuid) DO UPDATE SET " +
                     "job = excluded.job, " +
                     "join_timestamp = excluded.join_timestamp, " +
                     "xp = excluded.xp, " +
                     "job_level = excluded.job_level, " +
                     "resources_collected = excluded.resources_collected, " +
                     "money_earned = excluded.money_earned;";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, data.getUuid().toString());
            pstmt.setString(2, data.hasJob() ? data.getJobType().name() : null);
            pstmt.setLong(3, data.getJoinTimestamp());
            pstmt.setDouble(4, data.getXp());
            pstmt.setInt(5, data.getLevel());
            pstmt.setInt(6, data.getResourcesCollected());
            pstmt.setDouble(7, data.getMoneyEarned());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save job data for " + data.getUuid() + ": " + e.getMessage());
        }
    }
}