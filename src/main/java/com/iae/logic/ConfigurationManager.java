package com.iae.logic;

import com.iae.model.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConfigurationManager {

    private static ConfigurationManager instance;

    private final Map<Integer, Configuration> store = new LinkedHashMap<>();
    private int nextId = 1;

    protected ConfigurationManager() {}

    public static ConfigurationManager getInstance() {
        if (instance == null) instance = new ConfigurationManager();
        return instance;
    }

    public Configuration save(Configuration config) {
        if (config.getId() == 0) {
            config.setId(nextId++);
        }
        store.put(config.getId(), config);
        return config;
    }

    public Optional<Configuration> findById(int id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<Configuration> findByName(String name) {
        return store.values().stream()
                .filter(c -> c.getName() != null && c.getName().equals(name))
                .findFirst();
    }

    public List<Configuration> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    public boolean delete(int id) {
        return store.remove(id) != null;
    }
}
