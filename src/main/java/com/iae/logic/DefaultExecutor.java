package com.iae.logic;

import com.iae.model.Configuration;
import com.iae.model.ExecutionResult;
import com.iae.model.ProcessOutput;
import com.iae.model.Submission;
import com.iae.model.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DefaultExecutor implements Executor {

    private final long timeoutMs;

    public DefaultExecutor(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public ExecutionResult execute(Submission submission, Configuration config, TestCase testCase) {
        File workDir = submission.getExtractedDirectory();
        List<String> command = buildCommand(config, testCase);
        long start = System.currentTimeMillis();

        ProcessOutput output;
        try {
            output = ProcessRunner.run(command, workDir, timeoutMs);
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            ProcessOutput out = new ProcessOutput(1, "", e.getMessage(), elapsed, false);
            return new ExecutionResult(1, out, elapsed, false);
        }

        return new ExecutionResult(
                output.getExitCode(),
                output,
                output.getDurationMs(),
                output.isTimedOut()
        );
    }

    private List<String> buildCommand(Configuration config, TestCase testCase) {
        List<String> cmd = new ArrayList<>();
        cmd.add(config.getRunCommand());
        if (config.getRunArgs() != null && !config.getRunArgs().isBlank()) {
            for (String arg : config.getRunArgs().trim().split("\\s+")) {
                cmd.add(arg);
            }
        }
        if (testCase.getInputArgs() != null && !testCase.getInputArgs().isBlank()) {
            for (String arg : testCase.getInputArgs().trim().split("\\s+")) {
                cmd.add(arg);
            }
        }
        return cmd;
    }
}
