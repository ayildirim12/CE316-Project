package com.iae.model;

// Aggregate outcome of a single student submission evaluation.
public enum Status {

    /** All test cases produced correct output. */
    SUCCESS,

    /** At least one test case produced incorrect output. */
    WRONG_OUTPUT,

    /** Compilation failed; execution was skipped. */
    COMPILE_ERROR,

    /** At least one test case execution exceeded the time limit. */
    TIMEOUT,

    /** The process exited with a non-zero code or threw an unexpected exception. */
    RUNTIME_ERROR,

    /** The submission ZIP could not be extracted (final-phase only; not used in prototype). */
    ZIP_ERROR,

    /** No recognisable source file was found in the student's directory. */
    SOURCE_MISSING
}
