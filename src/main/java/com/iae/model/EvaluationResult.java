package com.iae.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Holds the complete grading outcome for a single student submission.
public class EvaluationResult {

    private int                      id;
    private final String             studentId;
    private Status                   status;
    private CompileResult            compileResult;
    private final List<ExecutionResult>  executionResults  = new ArrayList<>();
    private final List<ComparisonResult> comparisonResults = new ArrayList<>();
    private String                   errorMessage;
    private long                     durationMs;

    // Creates a new result for the given student.
    public EvaluationResult(String studentId) {
        this.studentId = Objects.requireNonNull(studentId, "studentId must not be null");
        this.status    = Status.SOURCE_MISSING;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public CompileResult getCompileResult() { return compileResult; }
    public void setCompileResult(CompileResult compileResult) {
        this.compileResult = compileResult;
    }
    public List<ExecutionResult> getExecutionResults() {
        return Collections.unmodifiableList(executionResults);
    }
    public List<ComparisonResult> getComparisonResults() {
        return Collections.unmodifiableList(comparisonResults);
    }

    // Appends an execution result for one test case.
    public void addExecutionResult(ExecutionResult result) {
        executionResults.add(Objects.requireNonNull(result, "executionResult must not be null"));
    }

    // Appends a comparison result for one test case.
    public void addComparisonResult(ComparisonResult result) {
        comparisonResults.add(Objects.requireNonNull(result, "comparisonResult must not be null"));
    }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    // ── equals / hashCode / toString ────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationResult other)) return false;
        return id == other.id && Objects.equals(studentId, other.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, studentId);
    }

    @Override
    public String toString() {
        return "EvaluationResult{"
                + "id=" + id
                + ", studentId='" + studentId + '\''
                + ", status=" + status
                + ", durationMs=" + durationMs
                + '}';
    }
}
