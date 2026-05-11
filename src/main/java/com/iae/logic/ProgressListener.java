package com.iae.logic;

// Callback interface for reporting evaluation pipeline progress to the UI layer.
public interface ProgressListener {
    // Called after each student submission is processed.

    void onProgress(int current, int total, String studentId);
    // Called once when the entire batch completes successfully (or after cancellation drains the remaining submissions).

    void onComplete();

    default void onError(String message) {}
}
