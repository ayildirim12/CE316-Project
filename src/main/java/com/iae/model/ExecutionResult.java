package com.iae.model;

public class ExecutionResult {

    private final int exitCode;
    private final ProcessOutput rawOutput;
    private final long durationMs;
    private final boolean timedOut;

    public ExecutionResult(int exitCode, ProcessOutput rawOutput,
                           long durationMs, boolean timedOut) {
        this.exitCode   = exitCode;
        this.rawOutput  = rawOutput;
        this.durationMs = durationMs;
        this.timedOut   = timedOut;
    }
    public boolean isSuccess()   { return exitCode == 0 && !timedOut; }
    public boolean isTimedOut()  { return timedOut; }
    public ProcessOutput getRawOutput() { return rawOutput; }
    public String getStdout() {
        return rawOutput != null ? rawOutput.getStdout() : "";
    }
    public String getStderr() {
        return rawOutput != null ? rawOutput.getStderr() : "";
    }
    public long getDurationMs() { return durationMs; }
    public int getExitCode()    { return exitCode; }
}
