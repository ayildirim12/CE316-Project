package com.iae.logic;

import com.iae.model.Configuration;
import com.iae.model.ExecutionResult;
import com.iae.model.ProcessOutput;
import com.iae.model.Submission;
import com.iae.model.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CodeRunner implements Executor {

    private static final int DEFAULT_TIMEOUT_MS = 10_000;

    private final int timeoutMs;

    public CodeRunner() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public CodeRunner(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public ExecutionResult execute(Submission submission, Configuration config, TestCase testCase) {
        List<String> command = buildCommand(config, testCase);
        long start = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(submission.getExtractedDirectory());

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - start;
            ProcessOutput output = new ProcessOutput(-1, "", e.getMessage(), duration, false);
            return new ExecutionResult(-1, output, duration, false);
        }

        // Read stdout and stderr in separate threads to prevent buffer deadlock.
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = pool.submit(() -> new String(process.getInputStream().readAllBytes()));
        Future<String> stderrFuture = pool.submit(() -> new String(process.getErrorStream().readAllBytes()));
        pool.shutdown();

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            long duration = System.currentTimeMillis() - start;
            ProcessOutput output = new ProcessOutput(-1, "", "Interrupted", duration, false);
            return new ExecutionResult(-1, output, duration, false);
        }

        if (!finished) {
            process.destroyForcibly();
            long duration = System.currentTimeMillis() - start;
            ProcessOutput output = new ProcessOutput(-1, "", "Time limit exceeded", duration, true);
            return new ExecutionResult(-1, output, duration, true);
        }

        long duration = System.currentTimeMillis() - start;
        int exitCode = process.exitValue();
        String stdout = getOrEmpty(stdoutFuture);
        String stderr = getOrEmpty(stderrFuture);

        ProcessOutput output = new ProcessOutput(exitCode, stdout, stderr, duration, false);
        return new ExecutionResult(exitCode, output, duration, false);
    }

    private List<String> buildCommand(Configuration config, TestCase testCase) {
        List<String> command = new ArrayList<>();
        command.add(config.getRunCommand());
        if (config.getRunArgs() != null && !config.getRunArgs().isBlank()) {
            command.addAll(Arrays.asList(config.getRunArgs().split("\\s+")));
        }
        if (testCase.getInputArgs() != null && !testCase.getInputArgs().isBlank()) {
            command.addAll(Arrays.asList(testCase.getInputArgs().split("\\s+")));
        }
        return command;
    }

    private String getOrEmpty(Future<String> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            return "";
        }
    }
}
