package com.iae.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Central domain entity representing a lecturer's grading project.
 *
 * <p>The link to a {@link Configuration} is a <em>soft reference</em>:
 * {@code configurationId} stores the JSON filename (without extension) from
 * the {@code configs/} directory — there is no relational foreign key.
 */
public class Project {

    // ── Fields ───────────────────────────────────────────────────────────────

    private int    id;
    private String name;
    private String configurationId;       // soft ref → JSON filename (no extension)
    private String submissionsDirectory;
    private Date   createdAt;
    private Date   lastRunAt;

    private final List<TestCase>         testCases   = new ArrayList<>();
    private final List<Submission>       submissions = new ArrayList<>();
    private final List<EvaluationResult> results     = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────────────────

    /** No-arg constructor required for DB reconstruction. */
    public Project() {}

    /**
     * Preferred constructor for new projects.
     *
     * @param name the human-readable project name
     * @param cfg  the configuration whose {@code name} becomes the soft reference ID
     */
    public Project(String name, Configuration cfg) {
        this.name            = Objects.requireNonNull(name, "name must not be null");
        this.configurationId = (cfg != null) ? cfg.getName() : null;
        this.createdAt       = new Date();
    }

    // ── Core getters / setters ────────────────────────────────────────────────

    public int    getId()                           { return id; }
    public void   setId(int id)                     { this.id = id; }

    public String getName()                         { return name; }
    public void   setName(String n)                 { this.name = n; }

    public String getConfigurationId()              { return configurationId; }
    public void   setConfigurationId(String cfgId)  { this.configurationId = cfgId; }

    public String getSubmissionsDirectory()         { return submissionsDirectory; }
    public void   setSubmissionsDirectory(String d) { this.submissionsDirectory = d; }

    public Date   getCreatedAt()                    { return createdAt; }
    public void   setCreatedAt(Date d)              { this.createdAt = d; }

    public Date   getLastRunAt()                    { return lastRunAt; }
    public void   setLastRunAt(Date d)              { this.lastRunAt = d; }

    // ── Test cases ────────────────────────────────────────────────────────────

    public List<TestCase> getTestCases()            { return testCases; }
    public void addTestCase(TestCase tc)            { testCases.add(tc); }

    // ── Submissions ───────────────────────────────────────────────────────────

    public List<Submission> getSubmissions()        { return submissions; }
    public void addSubmission(Submission s)         { submissions.add(s); }

    // ── Results ───────────────────────────────────────────────────────────────

    public List<EvaluationResult> getResults()      { return results; }
    public void addResult(EvaluationResult r)       { results.add(r); }

    // ── Convenience counters ──────────────────────────────────────────────────

    /**
     * Returns the number of results whose {@link Status} is {@code SUCCESS}.
     */
    public long getSuccessCount() {
        return results.stream()
                .filter(r -> r.getStatus() == Status.SUCCESS)
                .count();
    }

    /**
     * Returns the number of results whose {@link Status} is {@code WRONG_OUTPUT}.
     */
    public long getFailureCount() {
        return results.stream()
                .filter(r -> r.getStatus() == Status.WRONG_OUTPUT)
                .count();
    }

    /**
     * Returns the number of results with an error status
     * ({@code COMPILE_ERROR}, {@code RUNTIME_ERROR}, {@code TIMEOUT},
     *  {@code ZIP_ERROR}, or {@code SOURCE_MISSING}).
     */
    public long getErrorCount() {
        return results.stream()
                .filter(r -> r.getStatus() != null
                        && r.getStatus() != Status.SUCCESS
                        && r.getStatus() != Status.WRONG_OUTPUT)
                .count();
    }

    /**
     * Throws {@link NullPointerException} if {@code obj} is null.
     * Convenience guard used internally and by the logic layer.
     *
     * @param obj the object to check
     * @throws NullPointerException if obj is null
     */
    public static void requiresObject(Object obj) {
        Objects.requireNonNull(obj, "Required object must not be null");
    }

    // ── Object overrides ─────────────────────────────────────────────────────

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project other)) return false;
        return id == other.id && Objects.equals(name, other.name);
    }

    @Override
    public String toString() {
        return "Project{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", configurationId='" + configurationId + '\''
                + ", submissionsDirectory='" + submissionsDirectory + '\''
                + ", testCases=" + testCases.size()
                + ", createdAt=" + createdAt
                + '}';
    }
}
