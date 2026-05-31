package com.iae.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the plain domain objects in the {@code com.iae.model} package.
 * These have no external dependencies, so they are pure, fast unit tests.
 */
class ModelTest {

    // ── Project ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Project counters")
    class ProjectCounters {

        private Project withResults(Status... statuses) {
            Project p = new Project();
            for (Status s : statuses) {
                EvaluationResult r = new EvaluationResult("stu-" + s.name());
                r.setStatus(s);
                p.addResult(r);
            }
            return p;
        }

        @Test
        void successCountOnlyCountsSuccess() {
            Project p = withResults(Status.SUCCESS, Status.SUCCESS, Status.WRONG_OUTPUT);
            assertEquals(2, p.getSuccessCount());
        }

        @Test
        void failureCountOnlyCountsWrongOutput() {
            Project p = withResults(Status.WRONG_OUTPUT, Status.SUCCESS, Status.COMPILE_ERROR);
            assertEquals(1, p.getFailureCount());
        }

        @Test
        void errorCountCountsEverythingThatIsNotSuccessOrWrongOutput() {
            Project p = withResults(Status.COMPILE_ERROR, Status.RUNTIME_ERROR,
                    Status.TIMEOUT, Status.SUCCESS, Status.WRONG_OUTPUT);
            assertEquals(3, p.getErrorCount());
        }

        @Test
        void constructorWithConfigurationUsesConfigName() {
            Configuration cfg = new Configuration();
            cfg.setName("java-std");
            Project p = new Project("Lab1", cfg);
            assertEquals("Lab1", p.getName());
            assertEquals("java-std", p.getConfigurationId());
            assertNotNull(p.getCreatedAt());
        }

        @Test
        void requiresObjectThrowsOnNull() {
            assertThrows(NullPointerException.class, () -> Project.requiresObject(null));
            assertDoesNotThrow(() -> Project.requiresObject("x"));
        }
    }

    // ── EvaluationResult ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("EvaluationResult")
    class EvaluationResultTests {

        @Test
        void defaultStatusIsSourceMissing() {
            assertEquals(Status.SOURCE_MISSING, new EvaluationResult("s1").getStatus());
        }

        @Test
        void constructorRejectsNullStudentId() {
            assertThrows(NullPointerException.class, () -> new EvaluationResult(null));
        }

        @Test
        void executionResultsListIsUnmodifiable() {
            EvaluationResult r = new EvaluationResult("s1");
            r.addExecutionResult(new ExecutionResult(0, new ProcessOutput(0, "", "", 1, false), 1, false));
            assertThrows(UnsupportedOperationException.class,
                    () -> r.getExecutionResults().add(null));
        }

        @Test
        void addNullExecutionResultThrows() {
            EvaluationResult r = new EvaluationResult("s1");
            assertThrows(NullPointerException.class, () -> r.addExecutionResult(null));
            assertThrows(NullPointerException.class, () -> r.addComparisonResult(null));
        }

        @Test
        void equalsBasedOnIdAndStudentId() {
            EvaluationResult a = new EvaluationResult("s1");
            EvaluationResult b = new EvaluationResult("s1");
            a.setId(7);
            b.setId(7);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }

    // ── ProcessOutput / ExecutionResult / CompileResult / ComparisonResult ───────

    @Test
    void processOutputNeverReturnsNullStreams() {
        ProcessOutput po = new ProcessOutput(0, null, null, 5, false);
        assertEquals("", po.getStdout());
        assertEquals("", po.getStderr());
        assertEquals(0, po.getExitCode());
        assertFalse(po.isTimedOut());
    }

    @Test
    void executionResultIsSuccessOnlyWhenExitZeroAndNotTimedOut() {
        ProcessOutput ok = new ProcessOutput(0, "out", "", 5, false);
        assertTrue(new ExecutionResult(0, ok, 5, false).isSuccess());
        assertFalse(new ExecutionResult(1, ok, 5, false).isSuccess());
        assertFalse(new ExecutionResult(0, ok, 5, true).isSuccess(), "timed out is not success");
    }

    @Test
    void executionResultDelegatesStreamsToProcessOutput() {
        ExecutionResult er = new ExecutionResult(0,
                new ProcessOutput(0, "hello", "warn", 5, false), 5, false);
        assertEquals("hello", er.getStdout());
        assertEquals("warn", er.getStderr());
    }

    @Test
    void compileResultStderrIsEmptyWhenRawOutputNull() {
        assertEquals("", new CompileResult(false, null, 0).getStderr());
        CompileResult cr = new CompileResult(true,
                new ProcessOutput(0, "", "boom", 1, false), 1);
        assertEquals("boom", cr.getStderr());
        assertTrue(cr.isSuccess());
    }

    @Test
    void comparisonResultNullDiffBecomesEmptyString() {
        assertEquals("", new ComparisonResult(true, null).getDiff());
        assertTrue(new ComparisonResult(true, null).isMatch());
    }

    // ── TestCase ─────────────────────────────────────────────────────────────────

    @Test
    void testCaseFactoryRejectsNulls() {
        assertThrows(NullPointerException.class, () -> TestCase.of(null, "out.txt"));
        assertThrows(NullPointerException.class, () -> TestCase.of("args", null));
    }

    @Test
    void testCaseEqualsAndHashCode() {
        TestCase a = new TestCase(1, "5 3", "out.txt");
        TestCase b = new TestCase(1, "5 3", "out.txt");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        TestCase c = new TestCase(2, "5 3", "out.txt");
        assertNotEquals(a, c);
    }

    // ── Configuration ─────────────────────────────────────────────────────────────

    @Test
    void configurationGettersAndSettersRoundtrip() {
        Configuration c = new Configuration();
        c.setId(9);
        c.setName("C Programming");
        c.setLanguage("C");
        c.setSourceFile("main.c");
        c.setNeedsCompilation(true);
        c.setCompileCommand("gcc");
        c.setCompileArgs("-o main");
        c.setRunCommand("./main");
        c.setRunArgs("");

        assertEquals(9, c.getId());
        assertEquals("C Programming", c.getName());
        assertEquals("C", c.getLanguage());
        assertEquals("main.c", c.getSourceFile());
        assertTrue(c.isNeedsCompilation());
        assertEquals("gcc", c.getCompileCommand());
        assertEquals("-o main", c.getCompileArgs());
        assertEquals("./main", c.getRunCommand());
        assertEquals("", c.getRunArgs());
    }
}
