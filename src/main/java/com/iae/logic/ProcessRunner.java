package com.iae.logic;

import com.iae.model.ProcessOutput;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public final class ProcessRunner {

    private ProcessRunner() {}

    public static ProcessOutput run(List<String> command, File workingDir, long timeoutMs) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);

        long start = System.currentTimeMillis();
        Process process = pb.start();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<String> stdoutFuture = pool.submit(() -> drain(process.getInputStream()));
        Future<String> stderrFuture = pool.submit(() -> drain(process.getErrorStream()));

        boolean finished;
        try {
            finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            pool.shutdownNow();
            return new ProcessOutput(-1, "", "Interrupted", System.currentTimeMillis() - start, true);
        }

        if (!finished) {
            process.destroyForcibly();
            pool.shutdownNow();
            return new ProcessOutput(-1, "", "Process timed out after " + timeoutMs + " ms",
                    System.currentTimeMillis() - start, true);
        }

        pool.shutdown();
        String stdout = "";
        String stderr = "";
        try {
            stdout = stdoutFuture.get();
            stderr = stderrFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
        }

        return new ProcessOutput(process.exitValue(), stdout, stderr,
                System.currentTimeMillis() - start, false);
    }

    private static String drain(InputStream is) {
        try {
            return new String(is.readAllBytes());
        } catch (IOException e) {
            return "";
        }
    }
}
