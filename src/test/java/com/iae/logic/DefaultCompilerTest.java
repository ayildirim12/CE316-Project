package com.iae.logic;

import com.iae.model.CompileResult;
import com.iae.model.Configuration;
import com.iae.model.Submission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DefaultCompiler}. The "no-op compiler" path uses a
 * platform-appropriate command that always succeeds, so no real toolchain
 * (gcc/javac) needs to be installed.
 */
class DefaultCompilerTest {

    private static final boolean WINDOWS =
            System.getProperty("os.name").toLowerCase().contains("win");

    @TempDir
    Path workDir;

    private final DefaultCompiler compiler = new DefaultCompiler();

    private Submission submission() {
        return new Submission("S1", null, workDir.toFile(), true);
    }

    /** Configures a compile command that just echoes (always exits 0). */
    private void useNoOpCompiler(Configuration cfg) {
        if (WINDOWS) {
            cfg.setCompileCommand("cmd");
            cfg.setCompileArgs("/c echo");
        } else {
            cfg.setCompileCommand("echo");
            cfg.setCompileArgs("");
        }
    }

    @Test
    void missingSourceFileFailsWithClearMessage() {
        Configuration cfg = new Configuration();
        cfg.setNeedsCompilation(true);
        cfg.setSourceFile("nonexistent.c");
        useNoOpCompiler(cfg);

        CompileResult r = compiler.compile(submission(), cfg);

        assertFalse(r.isSuccess());
        assertTrue(r.getStderr().contains("Source file not found"),
                "stderr should explain the missing source file");
    }

    @Test
    void compilesSuccessfullyWhenSourcePresentAndCommandSucceeds() throws IOException {
        Files.writeString(workDir.resolve("main.c"), "int main(){return 0;}");

        Configuration cfg = new Configuration();
        cfg.setNeedsCompilation(true);
        cfg.setSourceFile("main.c");
        useNoOpCompiler(cfg);

        CompileResult r = compiler.compile(submission(), cfg);

        assertTrue(r.isSuccess(), "a present source + succeeding command should compile OK");
    }

    @Test
    void reportsFailureWhenCompilerCommandDoesNotExist() throws IOException {
        Files.writeString(workDir.resolve("main.c"), "int main(){return 0;}");

        Configuration cfg = new Configuration();
        cfg.setNeedsCompilation(true);
        cfg.setSourceFile("main.c");
        cfg.setCompileCommand("this-compiler-does-not-exist-xyz");
        cfg.setCompileArgs("");

        CompileResult r = compiler.compile(submission(), cfg);

        assertFalse(r.isSuccess(), "a missing compiler binary must yield a failed compile");
    }
}
