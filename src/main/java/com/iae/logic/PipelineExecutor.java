package com.iae.logic;

import com.iae.model.Configuration;
import com.iae.model.EvaluationResult;
import com.iae.model.Project;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PipelineExecutor {

    private final EvaluationEngine engine;
    private final ExecutorService threadPool;
    private Future<List<EvaluationResult>> runningTask;

    public PipelineExecutor(EvaluationEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.threadPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "pipeline-executor");
            t.setDaemon(true);
            return t;
        });
    }

    public Future<List<EvaluationResult>> submit(Project project, Configuration config) {
        if (isRunning()) {
            throw new IllegalStateException("A pipeline run is already in progress");
        }
        runningTask = threadPool.submit(() -> engine.runProject(project, config));
        return runningTask;
    }

    public void cancel() {
        engine.requestCancel();
        if (runningTask != null) {
            runningTask.cancel(true);
        }
    }

    public boolean isRunning() {
        return runningTask != null && !runningTask.isDone();
    }

    public void shutdown() {
        threadPool.shutdown();
    }
}
