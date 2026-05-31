package com.iae.logic;

import com.iae.model.EvaluationResult;
import com.iae.model.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReportManager} summary statistics, backed by the real
 * {@link DatabaseManager} (SQLite). This also exercises
 * {@link DatabaseManager#saveResult(EvaluationResult, int)} and
 * {@link DatabaseManager#getResultsForProject(int)}.
 */
class ReportManagerTest {

    private static final int PROJECT_ID = 1;

    private DatabaseManager db;
    private ReportManager report;

    @BeforeEach
    void setUp() throws SQLException {
        db = DatabaseManager.getInstance();
        db.reset();
        report = new ReportManager(db);
    }

    private void saveResult(String studentId, Status status) {
        EvaluationResult r = new EvaluationResult(studentId);
        r.setStatus(status);
        db.saveResult(r, PROJECT_ID);
    }

    @Test
    void summaryCountsPassFailErrorAndRate() {
        saveResult("s1", Status.SUCCESS);
        saveResult("s2", Status.WRONG_OUTPUT);
        saveResult("s3", Status.COMPILE_ERROR);
        saveResult("s4", Status.TIMEOUT);

        ReportManager.Summary summary = report.generateSummary(PROJECT_ID);

        assertEquals(4, summary.total());
        assertEquals(1, summary.passCount());
        assertEquals(1, summary.failCount());
        assertEquals(2, summary.errorCount(), "compile error + timeout count as errors");
        assertEquals(0.25, summary.passRate(), 1e-9);
    }

    @Test
    void emptyProjectHasZeroTotalAndZeroRate() {
        ReportManager.Summary summary = report.generateSummary(PROJECT_ID);
        assertEquals(0, summary.total());
        assertEquals(0.0, summary.passRate(), 1e-9);
    }

    @Test
    void allSuccessGivesFullPassRate() {
        saveResult("s1", Status.SUCCESS);
        saveResult("s2", Status.SUCCESS);

        ReportManager.Summary summary = report.generateSummary(PROJECT_ID);
        assertEquals(2, summary.passCount());
        assertEquals(1.0, summary.passRate(), 1e-9);
    }
}
