package com.iae.logic;

import com.iae.model.CompileResult;
import com.iae.model.Configuration;
import com.iae.model.ProcessOutput;
import com.iae.model.Submission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultCompiler implements Compiler {

    private static final long COMPILE_TIMEOUT_MS = 30_000L;

    @Override
    public CompileResult compile(Submission submission, Configuration config) {
        File workDir = submission.getExtractedDirectory();
        File sourceFile = resolveSourceFile(submission, config);

        if (sourceFile == null) {
            ProcessOutput out = new ProcessOutput(1, "", "Source file not found", 0, false);
            return new CompileResult(false, out, 0);
        }

        List<String> command = buildCommand(config, sourceFile);
        long start = System.currentTimeMillis();

        ProcessOutput output;
        try {
            output = ProcessRunner.run(command, workDir, COMPILE_TIMEOUT_MS);
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            ProcessOutput out = new ProcessOutput(1, "", e.getMessage(), elapsed, false);
            return new CompileResult(false, out, elapsed);
        }

        boolean success = output.getExitCode() == 0 && !output.isTimedOut();
        return new CompileResult(success, output, output.getDurationMs());
    }

    private File resolveSourceFile(Submission submission, Configuration config) {
        if (config.getSourceFile() != null && !config.getSourceFile().isBlank()) {
            File candidate = new File(submission.getExtractedDirectory(), config.getSourceFile());
            if (candidate.exists()) return candidate;
        }
        if (!submission.getSourceFiles().isEmpty()) {
            return submission.getSourceFiles().get(0);
        }
        return null;
    }

    private List<String> buildCommand(Configuration config, File sourceFile) {
        List<String> cmd = new ArrayList<>();
        cmd.add(config.getCompileCommand());
        if (config.getCompileArgs() != null && !config.getCompileArgs().isBlank()) {
            for (String arg : config.getCompileArgs().trim().split("\\s+")) {
                cmd.add(arg);
            }
        }
        cmd.add(sourceFile.getAbsolutePath());
        return cmd;
    }
}
