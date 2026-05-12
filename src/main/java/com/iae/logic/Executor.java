package com.iae.logic;

import com.iae.model.Configuration;
import com.iae.model.ExecutionResult;
import com.iae.model.Submission;
import com.iae.model.TestCase;

// Executes a compiled (or interpreted) student submission against one test case.

public interface Executor {

    // Runs the submission for the given test case.

    ExecutionResult execute(Submission submission, Configuration config, TestCase testCase);
}
