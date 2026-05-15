package com.iae.logic;

import com.iae.model.Project;
import com.iae.model.TestCase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles all CRUD operations for {@link Project} objects.
 *
 * <p>Persistence is delegated to {@link DatabaseManager}. An in-memory
 * {@link LinkedHashMap} acts as a write-through cache so that the rest of
 * the application can obtain objects without incurring a DB round-trip on
 * every access.
 *
 * <p>Configurations are resolved by name (soft reference) via
 * {@link ConfigurationManager} — no relational FK is involved.
 */
public class ProjectManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static ProjectManager instance;

    /** @return the single application-wide instance */
    public static ProjectManager getInstance() {
        if (instance == null) {
            instance = new ProjectManager();
        }
        return instance;
    }

    protected ProjectManager() {}

    // ── Internal cache ────────────────────────────────────────────────────────

    /** Write-through cache: projectId → Project */
    private final Map<Integer, Project> store = new LinkedHashMap<>();

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Persists a brand-new project (its test cases are also persisted).
     *
     * @param project a project with {@code id == 0}
     * @return the same project with the generated id set
     * @throws RuntimeException wrapping {@link SQLException} on DB failure
     */
    public Project createProject(Project project) {
        try {
            DatabaseManager.getInstance().saveProject(project);      // sets project.id
            persistTestCases(project);
            store.put(project.getId(), project);
            return project;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create project: " + e.getMessage(), e);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Persists changes to an existing project (also replaces its test cases).
     *
     * @param project a project with a valid, positive id
     * @throws IllegalArgumentException if the project is not found in the store
     * @throws RuntimeException wrapping {@link SQLException} on DB failure
     */
    public void updateProject(Project project) {
        if (!store.containsKey(project.getId()) && loadProject(project.getId()) == null) {
            throw new IllegalArgumentException("Project not found: id=" + project.getId());
        }
        try {
            DatabaseManager.getInstance().saveProject(project);
            persistTestCases(project);
            store.put(project.getId(), project);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update project: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience alias — inserts if {@code id == 0}, updates otherwise.
     *
     * @param project the project to persist
     */
    public void saveProject(Project project) {
        if (project.getId() == 0) {
            createProject(project);
        } else {
            updateProject(project);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Removes the project with the given id from the DB and the cache.
     *
     * @param id the project id to delete
     * @throws RuntimeException wrapping {@link SQLException} on DB failure
     */
    public void deleteProject(int id) {
        try {
            DatabaseManager.getInstance().deleteProject(id);
            store.remove(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete project id=" + id + ": " + e.getMessage(), e);
        }
    }

    /** Backward-compatible alias for {@link #deleteProject(int)}. */
    public boolean delete(int id) {
        try {
            deleteProject(id);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Loads a project by id. Checks the cache first; falls back to the DB.
     *
     * @param id the project id
     * @return the {@link Project}, or {@code null} if not found
     */
    public Project loadProject(int id) {
        if (store.containsKey(id)) return store.get(id);
        try {
            Project p = DatabaseManager.getInstance().loadProject(id);
            if (p != null) store.put(p.getId(), p);
            return p;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load project id=" + id + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns all projects from the DB, refreshing the cache.
     *
     * @return unmodifiable list of all projects
     */
    public List<Project> getAllProjects() {
        try {
            List<Project> projects = DatabaseManager.getInstance().getAllProjects();
            store.clear();
            projects.forEach(p -> store.put(p.getId(), p));
            return Collections.unmodifiableList(new ArrayList<>(store.values()));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load all projects: " + e.getMessage(), e);
        }
    }

    /**
     * Alias for {@link #getAllProjects()} — matches the spec method name.
     */
    public List<Project> loadAllProjects() {
        return getAllProjects();
    }

    // ── Optional-based finders (backward compat with existing UI code) ────────

    /** Finds a project by id in the cache, without a DB fallback. */
    public Optional<Project> findById(int id) {
        return Optional.ofNullable(loadProject(id));
    }

    /** Finds a project by exact name, scanning all loaded projects. */
    public Optional<Project> findByName(String name) {
        return getAllProjects().stream()
                .filter(p -> p.getName() != null && p.getName().equals(name))
                .findFirst();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Persists the test cases owned by a project.
     * Sets {@code projectId} on each test case before saving.
     */
    private void persistTestCases(Project project) throws SQLException {
        List<TestCase> tcs = project.getTestCases();
        if (tcs.isEmpty()) return;
        tcs.forEach(tc -> tc.setProjectId(project.getId()));
        DatabaseManager.getInstance().saveTestCasesForProject(tcs);
    }
}
