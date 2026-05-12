package com.iae.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
public class Submission {

    private final String studentId;
    private final Project project;
    private final File zipFile;
    private File extractedDirectory;
    private final List<File> sourceFiles = new ArrayList<>();

    public Submission(String studentId, Project project, File zipFile) {
        this.studentId = studentId;
        this.project   = project;
        this.zipFile   = zipFile;
    }

    public String getStudentId()             { return studentId; }
    public Project getProject()              { return project; }
    public File getZipFile()                 { return zipFile; }
    public File getExtractedDirectory()      { return extractedDirectory; }
    public void setExtractedDirectory(File d){ this.extractedDirectory = d; }
    public List<File> getSourceFiles()       { return sourceFiles; }
}
