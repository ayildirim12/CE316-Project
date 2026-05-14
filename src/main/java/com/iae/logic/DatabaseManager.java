package com.iae.logic;

import com.iae.model.EvaluationResult;
import com.iae.model.Status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static DatabaseManager instance;

    private static final String DB_URL = "jdbc:sqlite:iae.db";

    private Connection connection;

    protected DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
            initSchema(connection);
        }
        return connection;
    }

    private void initSchema(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS results (" +
                "  id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  student_id  TEXT    NOT NULL," +
                "  status      TEXT    NOT NULL," +
                "  error_msg   TEXT," +
                "  duration_ms INTEGER" +
                ")"
            );
        }
    }

    public synchronized void saveResult(EvaluationResult result) {
        String sql = "INSERT INTO results(student_id, status, error_msg, duration_ms)" +
                     " VALUES(?,?,?,?)";
        try (PreparedStatement ps = getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, result.getStudentId());
            ps.setString(2, result.getStatus() != null
                    ? result.getStatus().name() : Status.SOURCE_MISSING.name());
            ps.setString(3, result.getErrorMessage());
            ps.setLong(4, result.getDurationMs());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) result.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("DatabaseManager.saveResult failed: " + e.getMessage());
        }
    }

    public synchronized List<EvaluationResult> getResultsForProject(int projectId) {
        List<EvaluationResult> results = new ArrayList<>();
        String sql = "SELECT id, student_id, status, error_msg, duration_ms FROM results";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                EvaluationResult r = new EvaluationResult(rs.getString("student_id"));
                r.setId(rs.getInt("id"));
                r.setStatus(Status.valueOf(rs.getString("status")));
                r.setErrorMessage(rs.getString("error_msg"));
                r.setDurationMs(rs.getLong("duration_ms"));
                results.add(r);
            }
        } catch (SQLException e) {
            System.err.println("DatabaseManager.getResultsForProject failed: " + e.getMessage());
        }
        return results;
    }
}
