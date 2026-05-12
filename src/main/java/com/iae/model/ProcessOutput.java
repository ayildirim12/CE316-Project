package com.iae.model;

public class ProcessOutput {

    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final long durationMs;
    private final boolean timedOut;

    public ProcessOutput(int exitCode, String stdout, String stderr,
                         long durationMs, boolean timedOut) {
        this.exitCode   = exitCode;
        this.stdout     = stdout != null ? stdout : "";
        this.stderr     = stderr != null ? stderr : "";
        this.durationMs = durationMs;
        this.timedOut   = timedOut;
    }

    public int getExitCode()     { return exitCode; }
    public String getStdout()    { return stdout; }
    public String getStderr()    { return stderr; }
    public long getDurationMs()  { return durationMs; }
    public boolean isTimedOut()  { return timedOut; }
}
