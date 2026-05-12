package com.iae.logic;

import com.iae.model.EvaluationResult;

import java.util.ArrayList;
import java.util.List;
public class DatabaseManager {

    private static DatabaseManager instance;

    protected DatabaseManager() {}

    /** @return singleton instance */
    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    // Persists a completed evaluation result.
    public void saveResult(EvaluationResult result) {

    }

    // Returns all evaluation results for the given project.
    public List<EvaluationResult> getResultsForProject(int projectId) {

        return new ArrayList<>();
    }
}
