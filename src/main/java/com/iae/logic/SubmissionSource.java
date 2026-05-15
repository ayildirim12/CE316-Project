package com.iae.logic;

import com.iae.model.Submission;
import com.iae.model.Project;

import java.io.File;
import java.util.List;

// Strategy interface for loading student submissions from a directory.

public interface SubmissionSource {

    List<Submission> loadSubmissions(File submissionsDirectory, Project project);
}
