package com.iae.logic;

import com.iae.model.Configuration;
import com.iae.model.ExecutionResult;
import com.iae.model.Submission;
import com.iae.model.TestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultExecutor}. Uses platform-appropriate echo
 * commands so the program output is deterministic without a real student binary.
 */
class DefaultExecutorTest {

    private static final boolean WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    @TempDir
    Path workDir;

    private final DefaultExecutor executor = new DefaultExecutor(10_000);

    private Submission submission() {
        return new Submission("S1", null, workDir.toFile(), true);
    }

    /** A run configuration that echoes whatever input argument is given. */
    private Configuration echoConfig() {
        Configuration cfg = new Configuration();
        if (WINDOWS) {
            cfg.setRunCommand("cmd");
            cfg.setRunArgs("/c echo");
        } else {
            cfg.setRunCommand("echo");
            cfg.setRunArgs("");
        }
        return cfg;
    }

    @Test
    void runsProgramAndCapturesOutputFromInputArgs() {
        TestCase tc = new TestCase(1, "hello", "expected.txt");

        ExecutionResult r = executor.execute(submission(), echoConfig(), tc);

        assertTrue(r.isSuccess(), "echo should exit 0");
        assertFalse(r.isTimedOut());
        assertTrue(r.getStdout().contains("hello"),
                "the input argument should be echoed to stdout but was: " + r.getStdout());
    }

    @Test
    void unknownRunCommandIsReportedAsFailureNotAnException() {
        Configuration cfg = new Configuration();
        cfg.setRunCommand("this-program-does-not-exist-xyz");
        cfg.setRunArgs("");
        TestCase tc = new TestCase(1, "", "expected.txt");

        ExecutionResult r = executor.execute(submission(), cfg, tc);

        assertFalse(r.isSuccess(), "a missing binary must yield a non-successful result");
        assertEquals(1, r.getExitCode());
    }
}
