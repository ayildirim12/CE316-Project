package com.iae.logic;

import com.iae.model.Project;
import com.iae.model.Submission;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryScanner implements SubmissionSource {

    @Override
    public List<Submission> loadSubmissions(File submissionsDirectory, Project project) {
        List<Submission> submissions = new ArrayList<>();

        if (submissionsDirectory == null || !submissionsDirectory.isDirectory()) {
            return submissions; // return empty list, don't crash
        }

        File[] subdirs = submissionsDirectory.listFiles(File::isDirectory);
        if (subdirs == null) return submissions;

        for (File dir : subdirs) {
            Submission s = new Submission(dir.getName(), project, dir);
            // dir.getName() is the student ID (e.g. "20210001")
            submissions.add(s);
        }

        return submissions;
    }
}