package com.iae.logic;

public interface ProgressListener {
    void onProgress(int current, int total, String studentId);
    void onComplete();
}
