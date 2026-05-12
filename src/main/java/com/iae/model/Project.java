package com.iae.model;

import java.util.ArrayList;
import java.util.List;
public class Project {

    private int    id;
    private String name;
    private String submissionsDirectory;
    private final List<TestCase>        testCases   = new ArrayList<>();
    private final List<Submission>      submissions = new ArrayList<>();
    private final List<EvaluationResult> results    = new ArrayList<>();

    public Project() {}

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }

    public String getName()                        { return name; }
    public void   setName(String n)                { this.name = n; }

    public String getSubmissionsDirectory()        { return submissionsDirectory; }
    public void   setSubmissionsDirectory(String d){ this.submissionsDirectory = d; }

    public List<TestCase> getTestCases()           { return testCases; }
    public void addTestCase(TestCase tc)           { testCases.add(tc); }

    public void addSubmission(Submission s)        { submissions.add(s); }
    public List<Submission> getSubmissions()       { return submissions; }

    public void addResult(EvaluationResult r)      { results.add(r); }
    public List<EvaluationResult> getResults()     { return results; }
}
