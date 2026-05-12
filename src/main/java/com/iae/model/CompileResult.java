package com.iae.model;

public class CompileResult {

    private final boolean success;
    private final ProcessOutput rawOutput;
    private final long durationMs;

    public CompileResult(boolean success, ProcessOutput rawOutput, long durationMs) {
        this.success   = success;
        this.rawOutput = rawOutput;
        this.durationMs = durationMs;
    }
    public boolean isSuccess() { return success; }
    public ProcessOutput getRawOutput() { return rawOutput; }
    public String getStderr() {
        return rawOutput != null ? rawOutput.getStderr() : "";
    }
    public long getDurationMs() { return durationMs; }
}
