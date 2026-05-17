package com.iae.model;

import java.util.Date;
import java.util.Objects;

/**
 * Represents a single test scenario within a {@link Project}.
 *
 * <p>{@code projectId} is a relational foreign key to the PROJECTS table.
 * Use the static factory {@link #of(String, String)} to create new instances,
 * or the no-arg constructor for DB reconstruction.
 */
public class TestCase {

    // ── Fields ───────────────────────────────────────────────────────────────

    private int    id;
    private int    projectId;             // FK → PROJECTS.id
    private String name;
    private String inputArgs;             // command-line arguments for the student program
    private String expectedOutputFilePath;// path to the expected output file
    private Date   createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    /** No-arg constructor required for DB reconstruction. */
    public TestCase() {}

    /**
     * Full constructor used internally and by the DB layer.
     */
    public TestCase(int id, String inputArgs, String expectedOutputFilePath) {
        this.id                     = id;
        this.inputArgs              = inputArgs;
        this.expectedOutputFilePath = expectedOutputFilePath;
    }

    /**
     * Static factory method — the preferred way to create a new {@code TestCase}.
     *
     * @param inputArgs              command-line arguments to pass to the student program
     * @param expectedOutputFilePath path to the expected output file
     * @return a new {@code TestCase} with {@code createdAt} set to now
     */
    public static TestCase of(String inputArgs, String expectedOutputFilePath) {
        TestCase tc = new TestCase();
        tc.inputArgs              = Objects.requireNonNull(inputArgs, "inputArgs must not be null");
        tc.expectedOutputFilePath = Objects.requireNonNull(expectedOutputFilePath,
                "expectedOutputFilePath must not be null");
        tc.createdAt              = new Date();
        return tc;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int    getId()                               { return id; }
    public void   setId(int id)                         { this.id = id; }

    public int    getProjectId()                        { return projectId; }
    public void   setProjectId(int projectId)           { this.projectId = projectId; }

    public String getName()                             { return name; }
    public void   setName(String name)                  { this.name = name; }

    public String getInputArgs()                        { return inputArgs; }
    public void   setInputArgs(String s)                { this.inputArgs = s; }

    public String getExpectedOutputFilePath()           { return expectedOutputFilePath; }
    public void   setExpectedOutputFilePath(String s)   { this.expectedOutputFilePath = s; }

    public Date   getCreatedAt()                        { return createdAt; }
    public void   setCreatedAt(Date d)                  { this.createdAt = d; }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public int hashCode() {
        return Objects.hash(id, projectId, inputArgs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TestCase other)) return false;
        return id == other.id
                && projectId == other.projectId
                && Objects.equals(inputArgs, other.inputArgs)
                && Objects.equals(expectedOutputFilePath, other.expectedOutputFilePath);
    }

    @Override
    public String toString() {
        return "TestCase{"
                + "id=" + id
                + ", projectId=" + projectId
                + ", name='" + name + '\''
                + ", inputArgs='" + inputArgs + '\''
                + ", expectedOutputFilePath='" + expectedOutputFilePath + '\''
                + '}';
    }
}
