package com.iae.logic;

import com.iae.model.EvaluationResult;
import com.iae.model.Status;

import java.util.List;
import java.util.Objects;

// Provides summary statistics over the evaluation results of a project.
public class ReportManager {

    private final DatabaseManager databaseManager;
    public ReportManager(DatabaseManager databaseManager) {
        this.databaseManager = Objects.requireNonNull(databaseManager);
    }

    // Returns all evaluation results stored for the given project.
    public List<EvaluationResult> getResultsForProject(int projectId) {
        return databaseManager.getResultsForProject(projectId);
    }

    // Computes summary statistics for the given project.

    public Summary generateSummary(int projectId) {
        List<EvaluationResult> results = getResultsForProject(projectId);
        int total     = results.size();
        int passCount = 0;
        int failCount = 0;
        int errorCount = 0;

        for (EvaluationResult r : results) {
            Status s = r.getStatus();
            if (s == Status.SUCCESS) {
                passCount++;
            } else if (s == Status.WRONG_OUTPUT) {
                failCount++;
            } else {
                // COMPILE_ERROR, RUNTIME_ERROR, TIMEOUT, SOURCE_MISSING, ZIP_ERROR
                errorCount++;
            }
        }

        double passRate = total > 0 ? (double) passCount / total : 0.0;
        return new Summary(total, passCount, failCount, errorCount, passRate);
    }

    // ── Inner record ─────────────────────────────────────────────────────────

    // Immutable snapshot of project-level grading statistics.

    public record Summary(int total, int passCount, int failCount,
                          int errorCount, double passRate) {}
}
