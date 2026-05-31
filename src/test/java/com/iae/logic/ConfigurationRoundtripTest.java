package com.iae.logic;

import com.iae.model.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Save/load/delete roundtrip tests for configuration persistence, covering both
 * {@link DatabaseManager} (CONFIGURATIONS table) and the {@link ConfigurationManager}
 * write-through cache on top of it.
 */
class ConfigurationRoundtripTest {

    private ConfigurationManager cm;

    @BeforeEach
    void setUp() throws SQLException {
        DatabaseManager.getInstance().reset();   // clean tables
        cm = ConfigurationManager.getInstance();
        cm.findAll();                            // clear & reload the in-memory cache (now empty)
    }

    private Configuration sample(String name) {
        Configuration c = new Configuration();
        c.setName(name);
        c.setLanguage("C");
        c.setSourceFile("main.c");
        c.setNeedsCompilation(true);
        c.setCompileCommand("gcc");
        c.setCompileArgs("-o main");
        c.setRunCommand("./main");
        c.setRunArgs("");
        return c;
    }

    @Test
    @DisplayName("Configuration fields survive a save/load roundtrip via DatabaseManager")
    void databaseRoundtrip() throws SQLException {
        Configuration original = sample("C Programming");
        DatabaseManager.getInstance().saveConfiguration(original);
        assertTrue(original.getId() > 0, "insert should assign a positive id");

        List<Configuration> all = DatabaseManager.getInstance().getAllConfigurations();
        assertEquals(1, all.size());

        Configuration loaded = all.get(0);
        assertEquals("C Programming", loaded.getName());
        assertEquals("C", loaded.getLanguage());
        assertEquals("main.c", loaded.getSourceFile());
        assertTrue(loaded.isNeedsCompilation());
        assertEquals("gcc", loaded.getCompileCommand());
        assertEquals("-o main", loaded.getCompileArgs());
        assertEquals("./main", loaded.getRunCommand());
    }

    @Test
    @DisplayName("ConfigurationManager.save then findById / findByName returns it")
    void managerSaveAndFind() {
        Configuration saved = cm.save(sample("Java 17"));

        Optional<Configuration> byId = cm.findById(saved.getId());
        assertTrue(byId.isPresent());
        assertEquals("Java 17", byId.get().getName());

        Optional<Configuration> byName = cm.findByName("Java 17");
        assertTrue(byName.isPresent());
        assertEquals(saved.getId(), byName.get().getId());
    }

    @Test
    @DisplayName("findAll returns every saved configuration")
    void findAllReturnsAll() {
        cm.save(sample("Alpha"));
        cm.save(sample("Beta"));

        List<Configuration> all = cm.findAll();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(c -> "Alpha".equals(c.getName())));
        assertTrue(all.stream().anyMatch(c -> "Beta".equals(c.getName())));
    }

    @Test
    @DisplayName("Updating an existing configuration does not create a duplicate row")
    void updateDoesNotDuplicate() throws SQLException {
        Configuration c = cm.save(sample("Original"));
        int id = c.getId();

        c.setName("Renamed");
        cm.save(c);   // id != 0 → update

        List<Configuration> all = DatabaseManager.getInstance().getAllConfigurations();
        assertEquals(1, all.size(), "update must not insert a second row");
        assertEquals(id, all.get(0).getId());
        assertEquals("Renamed", all.get(0).getName());
    }

    @Test
    @DisplayName("delete removes the configuration from DB and cache")
    void deleteRemoves() {
        Configuration c = cm.save(sample("To Delete"));
        int id = c.getId();

        assertTrue(cm.delete(id));
        assertTrue(cm.findAll().isEmpty(), "configuration list should be empty after delete");
        assertTrue(cm.findById(id).isEmpty());
    }
}
