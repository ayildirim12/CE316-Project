package com.iae.logic;

import com.iae.model.Project;
import com.iae.model.Submission;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner implements SubmissionSource {

    private final ZipExtractor zipExtractor = new ZipExtractor();

    @Override
    public List<Submission> loadSubmissions(File submissionsDirectory, Project project) {
        List<Submission> submissions = new ArrayList<>();

        if (submissionsDirectory == null || !submissionsDirectory.isDirectory()) {
            return submissions;
        }

        File[] entries = submissionsDirectory.listFiles();
        if (entries == null) return submissions;

        for (File entry : entries) {
            if (entry.isDirectory() && !entry.getName().equals(".extracted")) {
                submissions.add(new Submission(entry.getName(), project, entry, true));
            } else if (entry.isFile() && entry.getName().toLowerCase().endsWith(".zip")) {
                String studentId = entry.getName().substring(0, entry.getName().length() - 4);
                File extractBase = new File(submissionsDirectory, ".extracted");
                try {
                    zipExtractor.extract(entry, extractBase);
                    // ZIP already contains a top-level folder named after the student
                    File extractDir = new File(extractBase, studentId);
                    submissions.add(new Submission(studentId, project, extractDir, true));
                } catch (IOException e) {
                    // skip unreadable zip
                }
            }
        }

        return submissions;
    }
}