package com.iae.logic;

import com.iae.model.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProjectManager {

    private static ProjectManager instance;

    private final Map<Integer, Project> store = new LinkedHashMap<>();
    private int nextId = 1;

    protected ProjectManager() {}

    public static ProjectManager getInstance() {
        if (instance == null) instance = new ProjectManager();
        return instance;
    }

    public Project save(Project project) {
        if (project.getId() == 0) {
            project.setId(nextId++);
        }
        store.put(project.getId(), project);
        return project;
    }

    public Project createProject(Project project) {
        return save(project);
    }

    public void updateProject(Project project) {
        if (!store.containsKey(project.getId())) {
            throw new IllegalArgumentException("Project not found: id=" + project.getId());
        }
        store.put(project.getId(), project);
    }

    public Optional<Project> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<Project> findByName(String name) {
        return store.values().stream()
                .filter(p -> p.getName() != null && p.getName().equals(name))
                .findFirst();
    }

    public List<Project> getAllProjects() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    public boolean delete(int id) {
        return store.remove(id) != null;
    }
}
