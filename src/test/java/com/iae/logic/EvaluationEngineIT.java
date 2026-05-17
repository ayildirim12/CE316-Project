package com.iae.logic;

import com.iae.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end smoke test for {@link EvaluationEngine}.
 *
 * <p>All external dependencies (Compiler, Executor, OutputComparator,
 * DatabaseManager, SubmissionSource) are replaced by in-test fakes that are
 * keyed on student ID.  This verifies the pipeline logic independently of
 * whether teammate implementations are ready.
 *
 * <p>Four edge-case students are exercised:
 * <ul>
 *   <li>{@code correct_student}      → {@link Status#SUCCESS}</li>
 *   <li>{@code wrong_output_student} → {@link Status#WRONG_OUTPUT}</li>
 *   <li>{@code broken_syntax_student}→ {@link Status#COMPILE_ERROR}</li>
 *   <li>{@code infinite_loop_student}→ {@link Status#TIMEOUT}</li>
 * </ul>
 */
class EvaluationEngineIT {

    private static final String CORRECT       = "correct_student";
    private static final String WRONG_OUTPUT  = "wrong_output_student";
    private static final String BROKEN_SYNTAX = "broken_syntax_student";
    private static final String INFINITE_LOOP = "infinite_loop_student";

    private Project       project;
    private Configuration config;
    private FakeDatabase  fakeDb;
    private EvaluationEngine engine;

    @BeforeEach
    void setUp() {
        config = new Configuration();
        config.setNeedsCompilation(true);

        project = new Project();
        project.setId(1);
        project.setSubmissionsDirectory("src/test/resources/demo/submissions");

        TestCase tc1 = new TestCase(1, "", "src/test/resources/demo/expected/tc1.txt");
        TestCase tc2 = new TestCase(2, "", "src/test/resources/demo/expected/tc2.txt");
        project.addTestCase(tc1);
        project.addTestCase(tc2);

        fakeDb = new FakeDatabase();

        engine = new EvaluationEngine(
                new FakeSubmissionSource(),
                new FakeCompiler(),
                new FakeExecutor(),
                new FakeOutputComparator(),
                fakeDb,
                null,
                5000
        );
    }

    @Test
    void correctStudentMapsToSuccess() {
        List<EvaluationResult> results = engine.runProject(project, config);
        EvaluationResult r = findResult(results, CORRECT);
        assertEquals(Status.SUCCESS, r.getStatus(),
                "correct_student should be SUCCESS");
    }

    @Test
    void wrongOutputStudentMapsToWrongOutput() {
        List<EvaluationResult> results = engine.runProject(project, config);
        EvaluationResult r = findResult(results, WRONG_OUTPUT);
        assertEquals(Status.WRONG_OUTPUT, r.getStatus(),
                "wrong_output_student should be WRONG_OUTPUT");
    }

    @Test
    void brokenSyntaxStudentMapsToCompileError() {
        List<EvaluationResult> results = engine.runProject(project, config);
        EvaluationResult r = findResult(results, BROKEN_SYNTAX);
        assertEquals(Status.COMPILE_ERROR, r.getStatus(),
                "broken_syntax_student should be COMPILE_ERROR");
        assertTrue(r.getExecutionResults().isEmpty(),
                "no execution should happen after compile failure");
    }

    @Test
    void infiniteLoopStudentMapsToTimeout() {
        List<EvaluationResult> results = engine.runProject(project, config);
        EvaluationResult r = findResult(results, INFINITE_LOOP);
        assertEquals(Status.TIMEOUT, r.getStatus(),
                "infinite_loop_student should be TIMEOUT");
    }

    @Test
    void allFourStudentsAreEvaluated() {
        List<EvaluationResult> results = engine.runProject(project, config);
        assertEquals(4, results.size(), "all 4 students must be evaluated");
    }

    @Test
    void summaryCountsAreCorrect() {
        engine.runProject(project, config);
        ReportManager rm = new ReportManager(fakeDb);
        ReportManager.Summary summary = rm.generateSummary(project.getId());

        assertEquals(4, summary.total());
        assertEquals(1, summary.passCount(),  "1 pass (correct_student)");
        assertEquals(1, summary.failCount(),  "1 fail (wrong_output_student)");
        assertEquals(2, summary.errorCount(), "2 errors (compile + timeout)");
        assertEquals(0.25, summary.passRate(), 1e-9);
    }

    @Test
    void cancellationStopsBatch() {
        engine.requestCancel();
        List<EvaluationResult> results = engine.runProject(project, config);
        assertEquals(0, results.size(), "cancelled before first student — no results");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private EvaluationResult findResult(List<EvaluationResult> results, String studentId) {
        return results.stream()
                .filter(r -> studentId.equals(r.getStudentId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No result for student: " + studentId));
    }

    // ── In-test fakes ─────────────────────────────────────────────────────────

    /** Returns four pre-built Submission objects (one per edge-case student). */
    private static class FakeSubmissionSource implements SubmissionSource {
        @Override
        public List<Submission> loadSubmissions(File submissionsDirectory, Project project) {
            List<Submission> list = new ArrayList<>();
            for (String id : List.of(CORRECT, WRONG_OUTPUT, BROKEN_SYNTAX, INFINITE_LOOP)) {
                Submission s = new Submission(id, null, null);
                s.setExtractedDirectory(new File(submissionsDirectory, id));
                // mark as having a source file (SOURCE_MISSING path not exercised here)
                s.getSourceFiles().add(new File(s.getExtractedDirectory(), "main.c"));
                list.add(s);
            }
            return list;
        }
    }

    /** Compile succeeds for all students except broken_syntax_student. */
    private static class FakeCompiler implements Compiler {
        @Override
        public CompileResult compile(Submission submission, Configuration config) {
            boolean ok = !BROKEN_SYNTAX.equals(submission.getStudentId());
            ProcessOutput po = new ProcessOutput(ok ? 0 : 1, "",
                    ok ? "" : "error: expected ')' before 'return'", 100, false);
            return new CompileResult(ok, po, 100);
        }
    }

    /**
     * Execution returns normal output for correct/wrong_output, timeout for infinite_loop.
     * broken_syntax never reaches execution.
     */
    private static class FakeExecutor implements Executor {
        private static final Map<String, String> STDOUT = Map.of(
                CORRECT,      "Hello, World!\n",
                WRONG_OUTPUT, "Goodbye, World!\n"
        );

        @Override
        public ExecutionResult execute(Submission submission, Configuration config,
                                       TestCase testCase) {
            String sid = submission.getStudentId();
            if (INFINITE_LOOP.equals(sid)) {
                ProcessOutput po = new ProcessOutput(137, "", "", 5000, true);
                return new ExecutionResult(137, po, 5000, true);
            }
            String out = STDOUT.getOrDefault(sid, "");
            ProcessOutput po = new ProcessOutput(0, out, "", 80, false);
            return new ExecutionResult(0, po, 80, false);
        }
    }

    /** Matches only when student output equals "Hello, World!\n". */
    private static class FakeOutputComparator extends OutputComparator {
        @Override
        public ComparisonResult compare(String actualOutput, String expectedOutputFilePath) {
            boolean match = "Hello, World!\n".equals(actualOutput);
            return new ComparisonResult(match, match ? "" : "Expected 'Hello, World!' got '" + actualOutput.trim() + "'");
        }
    }

    /** In-memory DatabaseManager that stores results indexed by project id. */
    private static class FakeDatabase extends DatabaseManager {
        private final List<EvaluationResult> store = new ArrayList<>();

        @Override
        public void saveResult(EvaluationResult result) {
            store.add(result);
        }

        @Override
        public List<EvaluationResult> getResultsForProject(int projectId) {
            return new ArrayList<>(store);
        }
    }
}
