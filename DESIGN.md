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
