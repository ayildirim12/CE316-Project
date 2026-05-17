package com.iae.logic;

import com.iae.model.ComparisonResult;
import com.iae.model.CompileResult;
import com.iae.model.Configuration;
import com.iae.model.EvaluationResult;
import com.iae.model.ExecutionResult;
import com.iae.model.Project;
import com.iae.model.Status;
import com.iae.model.Submission;
import com.iae.model.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Orchestrates the end-to-end grading pipeline for a single project run.
public class EvaluationEngine {

    private final SubmissionSource  submissionSource;
    private final Compiler          compiler;
    private final Executor          executor;
    private final OutputComparator  outputComparator;
    private final DatabaseManager   databaseManager;
    private final ProgressListener  progressListener;
    private final int               defaultTimeoutMs;

    private volatile boolean cancelled = false;
    public EvaluationEngine(SubmissionSource submissionSource,
                            Compiler compiler,
                            Executor executor,
                            OutputComparator outputComparator,
                            DatabaseManager databaseManager,
                            ProgressListener progressListener,
                            int defaultTimeoutMs) {
        this.submissionSource  = Objects.requireNonNull(submissionSource);
        this.compiler          = Objects.requireNonNull(compiler);
        this.executor          = Objects.requireNonNull(executor);
        this.outputComparator  = Objects.requireNonNull(outputComparator);
        this.databaseManager   = Objects.requireNonNull(databaseManager);
        this.progressListener  = progressListener;
        this.defaultTimeoutMs  = defaultTimeoutMs;
    }

    // Runs the grading pipeline for all submissions found under
    public List<EvaluationResult> runProject(Project project, Configuration config) {
        File submissionsDir = new File(project.getSubmissionsDirectory());
        List<Submission> submissions;
        try {
            submissions = submissionSource.loadSubmissions(submissionsDir, project);
        } catch (Exception e) {
            if (progressListener != null)
                progressListener.onError("Cannot load submissions: " + e.getMessage());
            return List.of();
        }

        File baseDir = submissionsDir.getParentFile();

        List<EvaluationResult> results = new ArrayList<>();
        int total = submissions.size();

        for (int i = 0; i < total; i++) {
            if (cancelled) break;

            Submission submission = submissions.get(i);
            String studentId = submission.getStudentId();
            EvaluationResult result = new EvaluationResult(studentId);
            long start = System.currentTimeMillis();

            boolean canExecute = evaluateCompilePhase(submission, config, result);

            if (canExecute) {
                evaluateExecutionPhase(submission, config, project.getTestCases(), result, baseDir);
            }

            result.setDurationMs(System.currentTimeMillis() - start);

            try {
                databaseManager.saveResult(result);
            } catch (Exception e) {
                result.setErrorMessage("DB save failed: " + e.getMessage());
            }

            results.add(result);

            if (progressListener != null)
                progressListener.onProgress(i + 1, total, studentId);
        }

        if (progressListener != null) progressListener.onComplete();
        return results;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    // Runs the compile phase for one student.
    private boolean evaluateCompilePhase(Submission submission,
                                         Configuration config,
                                         EvaluationResult result) {
        if (!config.isNeedsCompilation()) return true;

        try {
            CompileResult cr = compiler.compile(submission, config);
            result.setCompileResult(cr);
            if (!cr.isSuccess()) {
                result.setStatus(Status.COMPILE_ERROR);
                result.setErrorMessage(cr.getStderr());
                return false;
            }
        } catch (Exception e) {
            result.setStatus(Status.COMPILE_ERROR);
            result.setErrorMessage("Compiler exception: " + e.getMessage());
            return false;
        }

        return true;
    }

    // Runs the execute-and-compare phase for all test cases of one student.

    private void evaluateExecutionPhase(Submission submission,
                                        Configuration config,
                                        List<TestCase> testCases,
                                        EvaluationResult result,
                                        File baseDir) {
        Status worstStatus = Status.SUCCESS;

        for (TestCase tc : testCases) {
            ExecutionResult er = null;

            try {
                er = executor.execute(submission, config, tc);
                result.addExecutionResult(er);
            } catch (Exception e) {
                worstStatus = dominantStatus(worstStatus, Status.RUNTIME_ERROR);
                result.setErrorMessage("Executor exception: " + e.getMessage());
                continue;
            }

            if (er.isTimedOut()) {
                worstStatus = dominantStatus(worstStatus, Status.TIMEOUT);
                continue;
            }

            if (!er.isSuccess()) {
                worstStatus = dominantStatus(worstStatus, Status.RUNTIME_ERROR);
            }

            try {
                String expectedPath = resolveExpectedPath(tc.getExpectedOutputFilePath(), baseDir);
                ComparisonResult cr = outputComparator.compare(er.getStdout(), expectedPath);
                result.addComparisonResult(cr);
                if (!cr.isMatch()) {
                    worstStatus = dominantStatus(worstStatus, Status.WRONG_OUTPUT);
                }
            } catch (Exception e) {
                result.setErrorMessage("Comparator exception: " + e.getMessage());
            }
        }

        result.setStatus(worstStatus);
    }

    private static String resolveExpectedPath(String path, File baseDir) {
        if (path == null) return "";
        File f = new File(path);
        if (f.isAbsolute()) return path;
        return new File(baseDir, path).getAbsolutePath();
    }

    // Returns the more severe of two statuses.
    private static Status dominantStatus(Status current, Status candidate) {
        return severityOf(candidate) > severityOf(current) ? candidate : current;
    }

    private static int severityOf(Status s) {
        return switch (s) {
            case TIMEOUT       -> 4;
            case RUNTIME_ERROR -> 3;
            case WRONG_OUTPUT  -> 2;
            case SUCCESS       -> 1;
            default            -> 0;
        };
    }

    // Requests cancellation of the running batch. The engine will stop after completing the student currently in progress.

    public void requestCancel() {
        this.cancelled = true;
    }
}
