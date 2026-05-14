# IAE — Integrated Assignment Evaluator
## Design Document

---

## 1. Introduction

### 1.1 Purpose

IAE (Integrated Assignment Evaluator) is a desktop application that automates the grading of student programming assignments. Given a set of student submissions and a set of test cases, IAE compiles each submission (when required), runs it against every test case, compares the actual output with the expected output, and produces a structured evaluation report.

The primary goal is to reduce the manual effort instructors spend on repetitive compile-run-compare cycles, and to produce consistent, reproducible grades across all submissions in a batch.

### 1.2 Scope

**In scope:**

- Loading student submissions from a directory of ZIP archives, one archive per student.
- Supporting compiled languages (e.g. Java, C) and interpreted languages (e.g. Python) through a configurable run/compile command model.
- Executing each submission against one or more test cases defined by command-line arguments and an expected output file.
- Comparing actual standard output against expected output (exact match).
- Persisting evaluation results to a local SQLite database.
- Displaying results in a JavaFX desktop interface with per-student status, per-test-case pass/fail, and project-level summary statistics.

**Out of scope:**

- Network-based submission collection or LMS integration.
- Plagiarism detection.
- Interactive or stdin-driven programs (test cases are argument-driven).
- Concurrent evaluation of multiple projects at the same time.

### 1.3 Deployment Context

IAE runs as a standalone desktop application on the instructor's machine. It requires:

- Java 17 or later.
- JavaFX 21 runtime (bundled via Maven dependencies).
- Read access to the file system directory containing student ZIP archives.
- Write access to the working directory for the SQLite database file (`iae.db`).

No server, network connection, or external service is needed. All state (projects, configurations, evaluation results) is stored locally in the SQLite database and in the in-memory project/configuration stores for the duration of the session.

The application is packaged as a single executable JAR via the Maven Shade plugin and is launched through the `Launcher` entry point.

### 1.4 Team and Course Context

This project is developed as the term project for **CE 316 — Software Engineering, Spring 2026**. The codebase is divided across three layers — Model, Logic, and UI — each assigned to a different team member, with integration carried out on shared branches.

---

## 2. Structural Design — Model Layer

The model layer (`com.iae.model`) contains plain data-holding classes with no business logic. All classes are in the same package and have no dependencies outside of the Java standard library.

---

### 2.1 Class Overview

| Class | Role |
|---|---|
| `Configuration` | Language profile: how to compile and run a submission |
| `Project` | A grading project: test cases, submissions, and results |
| `TestCase` | One test case: input arguments and expected output file |
| `Submission` | One student's submission: ZIP file and extracted source |
| `EvaluationResult` | Complete grading outcome for one student |
| `Status` | Enum: the possible outcomes of an evaluation |
| `CompileResult` | Outcome of the compilation step |
| `ExecutionResult` | Outcome of running the program against one test case |
| `ComparisonResult` | Outcome of comparing actual output with expected output |
| `ProcessOutput` | Raw stdout/stderr/exit-code captured from an OS process |

---

### 2.2 Configuration

Holds the language profile that tells the pipeline how to compile and run a student submission. A single `Configuration` can be reused across multiple `Project` instances.

**Fields**

| Field | Type | Description |
|---|---|---|
| `id` | `int` | Auto-assigned primary key |
| `name` | `String` | Human-readable label (e.g. "Java 17 Standard") |
| `language` | `String` | Language identifier (e.g. "java", "c", "python") |
| `sourceFile` | `String` | Expected source file name inside the submission (e.g. "Main.java") |
| `compileCommand` | `String` | Compiler executable (e.g. "javac") |
| `compileArgs` | `String` | Additional compiler flags (space-separated) |
| `runCommand` | `String` | Runtime executable (e.g. "java") |
| `runArgs` | `String` | Additional runtime flags (space-separated) |
| `needsCompilation` | `boolean` | `false` for interpreted languages; skips the compile phase |

**Key design note:** `compileArgs` and `runArgs` are stored as a single space-separated string and split at execution time. This keeps the model simple while supporting multi-flag configurations.

---

### 2.3 Project

Represents one grading assignment. Owns the list of test cases and accumulates submissions and evaluation results during a pipeline run.

**Fields**

| Field | Type | Description |
|---|---|---|
| `id` | `int` | Auto-assigned primary key |
| `name` | `String` | Human-readable project name (e.g. "CS101 – Homework 1") |
| `submissionsDirectory` | `String` | Absolute path to the directory of student ZIP archives |
| `testCases` | `List<TestCase>` | Ordered list of test cases; populated by the instructor |
| `submissions` | `List<Submission>` | Loaded at pipeline start by `SubmissionSource` |
| `results` | `List<EvaluationResult>` | Populated by `EvaluationEngine` after the run |

**Methods**

| Method | Description |
|---|---|
| `addTestCase(TestCase)` | Appends a test case to the project's list |
| `addSubmission(Submission)` | Appends a loaded submission |
| `addResult(EvaluationResult)` | Appends a completed evaluation result |
| `getTestCases()` | Returns the live `List<TestCase>` |
| `getSubmissions()` | Returns the live `List<Submission>` |
| `getResults()` | Returns the live `List<EvaluationResult>` |

---

### 2.4 TestCase

A single test scenario. The pipeline runs the student's program with `inputArgs` and compares standard output against the file at `expectedOutputFilePath`.

**Fields**

| Field | Type | Description |
|---|---|---|
| `id` | `int` | 1-based index within the project |
| `inputArgs` | `String` | Command-line arguments passed to the student's program |
| `expectedOutputFilePath` | `String` | Path to the file containing the expected standard output |

---

### 2.5 Submission

Represents one student's submission artefact. Created by `SubmissionSource` from a ZIP file; the extracted directory and source-file list are populated before the compile/run phases.

**Fields**

| Field | Type | Description |
|---|---|---|
| `studentId` | `String` | Identifier derived from the ZIP file name |
| `project` | `Project` | Back-reference to the owning project |
| `zipFile` | `File` | The original ZIP archive |
| `extractedDirectory` | `File` | Directory where the ZIP was extracted; set after extraction |
| `sourceFiles` | `List<File>` | Source files discovered inside the extracted directory |

All fields except `extractedDirectory` are set at construction time and are immutable.

---

### 2.6 EvaluationResult

The complete grading record for one student. Populated incrementally by `EvaluationEngine` as each pipeline phase completes.

**Fields**

| Field | Type | Description |
|---|---|---|
| `id` | `int` | DB-assigned primary key (set by `DatabaseManager.saveResult`) |
| `studentId` | `String` | Immutable; set at construction |
| `status` | `Status` | Final aggregate outcome; defaults to `SOURCE_MISSING` |
| `compileResult` | `CompileResult` | Populated if the compile phase ran |
| `executionResults` | `List<ExecutionResult>` | One entry per test case that was executed |
| `comparisonResults` | `List<ComparisonResult>` | One entry per test case that was compared |
| `errorMessage` | `String` | Human-readable error detail for non-SUCCESS outcomes |
| `durationMs` | `long` | Wall-clock time for the entire evaluation of this student |

**Methods**

| Method | Description |
|---|---|
| `addExecutionResult(ExecutionResult)` | Appends to the execution results list (null-safe) |
| `addComparisonResult(ComparisonResult)` | Appends to the comparison results list (null-safe) |
| `getExecutionResults()` | Returns an unmodifiable view |
| `getComparisonResults()` | Returns an unmodifiable view |

---

### 2.7 Status

Enum representing the aggregate outcome of a student's evaluation. Used by `EvaluationEngine` to summarise the worst-case result across all test cases.

| Value | Meaning |
|---|---|
| `SUCCESS` | All test cases produced correct output |
| `WRONG_OUTPUT` | At least one test case produced incorrect output |
| `COMPILE_ERROR` | Compilation failed; execution was skipped |
| `TIMEOUT` | At least one test case exceeded the time limit |
| `RUNTIME_ERROR` | Process exited with a non-zero code or threw an exception |
| `ZIP_ERROR` | The submission ZIP could not be extracted |
| `SOURCE_MISSING` | No recognisable source file was found in the submission |

Severity ordering used by `EvaluationEngine` (highest → lowest): `TIMEOUT > RUNTIME_ERROR > WRONG_OUTPUT > SUCCESS`.

---

### 2.8 Supporting Value Types

#### CompileResult

Immutable snapshot of a compilation attempt.

| Field | Type | Description |
|---|---|---|
| `success` | `boolean` | `true` if the compiler exited with code 0 |
| `rawOutput` | `ProcessOutput` | Full stdout/stderr from the compiler process |
| `durationMs` | `long` | Time taken by the compiler |

Convenience method `getStderr()` delegates to `rawOutput.getStderr()`.

#### ExecutionResult

Immutable snapshot of one test-case execution.

| Field | Type | Description |
|---|---|---|
| `exitCode` | `int` | OS process exit code |
| `rawOutput` | `ProcessOutput` | Full stdout/stderr from the student's program |
| `durationMs` | `long` | Wall-clock time for this execution |
| `timedOut` | `boolean` | `true` if the process was killed for exceeding the limit |

`isSuccess()` returns `true` only when `exitCode == 0` and `timedOut == false`.

#### ComparisonResult

Immutable outcome of one output comparison.

| Field | Type | Description |
|---|---|---|
| `match` | `boolean` | `true` if actual output equals expected output |
| `diffText` | `String` | Human-readable diff or mismatch description; empty string on match |

#### ProcessOutput

Raw data captured from any OS process (compiler or student program). Shared by both `CompileResult` and `ExecutionResult`.

| Field | Type | Description |
|---|---|---|
| `exitCode` | `int` | Process exit code |
| `stdout` | `String` | Captured standard output; never null |
| `stderr` | `String` | Captured standard error; never null |
| `durationMs` | `long` | Wall-clock time |
| `timedOut` | `boolean` | `true` if the process was killed by the timeout watchdog |

---

### 2.9 Class Relationships

```
Configuration           Project
───────────             ───────────────────────────────────
id, name, language      id, name, submissionsDirectory
compileCommand          ├── List<TestCase>
runCommand              │     └── id, inputArgs,
needsCompilation        │         expectedOutputFilePath
                        ├── List<Submission>
                        │     └── studentId, zipFile,
                        │         extractedDirectory,
                        │         List<File> sourceFiles
                        └── List<EvaluationResult>
                              └── studentId, Status
                                  CompileResult
                                  List<ExecutionResult>
                                    └── ProcessOutput
                                  List<ComparisonResult>
```

`Configuration` and `Project` are independent root objects managed by `ConfigurationManager` and `ProjectManager` respectively. A `Project` references its `Configuration` by the ID stored in `ConfigurationManager`; the link is resolved at pipeline start time by the UI layer.

---
