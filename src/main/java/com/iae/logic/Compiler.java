package com.iae.logic;

import com.iae.model.CompileResult;
import com.iae.model.Configuration;
import com.iae.model.Submission;

// Compiles a student submission according to the given configuration.
public interface Compiler {

    /**
     * Compiles the source files in {@code submission} using the compiler
     * settings from {@code config}.
    */
    CompileResult compile(Submission submission, Configuration config);
}
