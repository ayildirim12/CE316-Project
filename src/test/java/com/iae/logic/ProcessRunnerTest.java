package com.iae.logic;

import com.iae.model.ProcessOutput;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ProcessRunner}. These launch real OS processes, so the
 * commands are chosen per platform (the project targets Windows, but the tests
 * also run on Unix CI).
 */
class ProcessRunnerTest {

    private static final boolean WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    private static final File CWD = new File(".");

    /** A command that prints {@code text} followed by a newline. */
    private static List<String> echo(String text) {
        return WINDOWS ? List.of("cmd", "/c", "echo", text) : List.of("echo", text);
    }

    /** A command that exits with the given status code. */
    private static List<String> exitWith(int code) {
        return WINDOWS ? List.of("cmd", "/c", "exit " + code)
                       : List.of("sh", "-c", "exit " + code);
    }

    /** A command that blocks for several seconds. */
    private static List<String> sleepLong() {
        return WINDOWS ? List.of("cmd", "/c", "ping", "-n", "6", "127.0.0.1")
                       : List.of("sh", "-c", "sleep 6");
    }

    @Test
    void capturesStdoutAndZeroExitCode() throws IOException {
        ProcessOutput out = ProcessRunner.run(echo("hello"), CWD, 10_000);
        assertEquals(0, out.getExitCode());
        assertFalse(out.isTimedOut());
        assertTrue(out.getStdout().contains("hello"),
                "stdout should contain the echoed text but was: " + out.getStdout());
    }

    @Test
    void reportsNonZeroExitCode() throws IOException {
        ProcessOutput out = ProcessRunner.run(exitWith(3), CWD, 10_000);
        assertEquals(3, out.getExitCode());
        assertFalse(out.isTimedOut());
    }

    @Test
    void killsAndFlagsProcessThatExceedsTimeout() throws IOException {
        long start = System.currentTimeMillis();
        ProcessOutput out = ProcessRunner.run(sleepLong(), CWD, 300);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(out.isTimedOut(), "process exceeding the timeout must be flagged");
        assertTrue(elapsed < 5_000,
                "runner should return shortly after the timeout, not wait for completion");
    }

    @Test
    void throwsIOExceptionForUnknownCommand() {
        assertThrows(IOException.class,
                () -> ProcessRunner.run(List.of("this-command-does-not-exist-xyz"), CWD, 5_000));
    }
}
