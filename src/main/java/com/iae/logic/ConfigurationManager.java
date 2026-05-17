package com.iae.logic;

import com.iae.model.Configuration;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Singleton that manages {@link Configuration} objects.
 *
 * <p>Persistence is delegated to {@link DatabaseManager} (CONFIGURATIONS table).
 * An in-memory write-through cache avoids redundant DB round-trips.
 */
public class ConfigurationManager {

    // ── Singleton ─────────────────────────────────────────────────────────────

    private static ConfigurationManager instance;

    public static ConfigurationManager getInstance() {
        if (instance == null) instance = new ConfigurationManager();
        return instance;
    }

    protected ConfigurationManager() {}

    // ── Cache ─────────────────────────────────────────────────────────────────

    /** Write-through cache: configId → Configuration */
    private final Map<Integer, Configuration> store = new LinkedHashMap<>();

    // ── Persist ───────────────────────────────────────────────────────────────

    /**
     * Saves (inserts or updates) a configuration and adds it to the cache.
     *
     * @param config the configuration to persist
     * @return the same config with its generated id set (if new)
     * @throws RuntimeException wrapping {@link SQLException} on DB failure
     */
    public Configuration save(Configuration config) {
        try {
            DatabaseManager.getInstance().saveConfiguration(config);  // sets config.id if new
            store.put(config.getId(), config);
            return config;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save configuration: " + e.getMessage(), e);
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Removes the configuration with the given id from the DB and cache.
     *
     * @param id the configuration id
     * @return {@code true} if the configuration existed and was removed
     */
    public boolean delete(int id) {
        try {
            DatabaseManager.getInstance().deleteConfiguration(id);
            return store.remove(id) != null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete configuration id=" + id + ": " + e.getMessage(), e);
        }
    }

    // ── Find ──────────────────────────────────────────────────────────────────

    /**
     * Finds a configuration by its numeric id.
     * Checks the cache first; falls back to loading all from the DB.
     */
    public Optional<Configuration> findById(int id) {
        if (store.containsKey(id)) return Optional.of(store.get(id));
        loadAllIntoCache();
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Finds a configuration by name (the soft-reference key used in {@link com.iae.model.Project}).
     * Used by {@link com.iae.ui.ProjectDialogController} to pre-select the right combo item.
     */
    public Optional<Configuration> findByName(String name) {
        if (name == null) return Optional.empty();
        // Check cache first
        Optional<Configuration> cached = store.values().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst();
        if (cached.isPresent()) return cached;
        // Refresh from DB and try again
        loadAllIntoCache();
        return store.values().stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst();
    }

    /**
     * Returns all configurations, refreshing the cache from the DB.
     */
    public List<Configuration> findAll() {
        loadAllIntoCache();
        return Collections.unmodifiableList(new ArrayList<>(store.values()));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Loads all configurations from the DB into the in-memory cache. */
    private void loadAllIntoCache() {
        try {
            List<Configuration> all = DatabaseManager.getInstance().getAllConfigurations();
            store.clear();
            all.forEach(c -> store.put(c.getId(), c));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load configurations: " + e.getMessage(), e);
        }
    }
}
