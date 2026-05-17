package com.iae.logic;

import com.iae.model.Project;
import com.iae.model.TestCase;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Save/load roundtrip test for the persistence layer.
 *
 * <p>Each test method starts with a clean database ({@code iae_test.db} is
 * implicitly determined by the working directory during Maven test execution).
 * We call {@link DatabaseManager#reset()} in {@code @BeforeEach} to guarantee
 * a clean slate regardless of execution order.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProjectRoundtripTest {

    private ProjectManager pm;

    @BeforeEach
    void setUp() throws SQLException {
        // Reset the singleton cache so each test starts from scratch
        DatabaseManager.getInstance().reset();
        pm = ProjectManager.getInstance();
        // Clear the in-memory cache by re-creating the instance reference
        // (calling getAllProjects() with an empty DB achieves the same effect)
        pm.getAllProjects();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1 — basic roundtrip: name, configurationId, submissionsDirectory
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Project fields survive a save/load roundtrip")
    void projectFieldsRoundtrip() throws SQLException {
        // Arrange
        Project original = new Project();
        original.setName("CE101 – Lab 1");
        original.setConfigurationId("java17-standard");
        original.setSubmissionsDirectory("/submissions/lab1");

        // Act — persist
        pm.createProject(original);
        int savedId = original.getId();
        assertTrue(savedId > 0, "Generated id must be positive");

        // Act — reload from DB (force DB path by querying directly)
        Project loaded = DatabaseManager.getInstance().loadProject(savedId);

        // Assert
        assertNotNull(loaded, "Loaded project must not be null");
        assertEquals(savedId,                  loaded.getId(),                   "id");
        assertEquals("CE101 – Lab 1",          loaded.getName(),                 "name");
        assertEquals("java17-standard",        loaded.getConfigurationId(),      "configurationId");
        assertEquals("/submissions/lab1",      loaded.getSubmissionsDirectory(), "submissionsDirectory");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2 — test cases survive the roundtrip
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("TestCase fields survive a save/load roundtrip")
    void testCasesRoundtrip() throws SQLException {
        // Arrange — project with two test cases
        Project original = new Project();
        original.setName("Data Structures – HW2");
        original.setConfigurationId("cpp17-std");
        original.setSubmissionsDirectory("/submissions/hw2");

        TestCase tc1 = TestCase.of("--input 5",  "expected/tc1.txt");
        tc1.setName("Small input");
        TestCase tc2 = TestCase.of("--input 100", "expected/tc2.txt");
        tc2.setName("Large input");

        original.addTestCase(tc1);
        original.addTestCase(tc2);

        // Act — persist
        pm.createProject(original);
        int savedId = original.getId();

        // Act — reload
        Project loaded = DatabaseManager.getInstance().loadProject(savedId);

        // Assert — project
        assertNotNull(loaded);

        // Assert — test cases
        List<TestCase> loadedTcs = loaded.getTestCases();
        assertEquals(2, loadedTcs.size(), "Should have 2 test cases");

        TestCase l1 = loadedTcs.get(0);
        assertEquals("--input 5",        l1.getInputArgs(),              "TC1 inputArgs");
        assertEquals("expected/tc1.txt", l1.getExpectedOutputFilePath(), "TC1 expectedOutputFilePath");
        assertEquals("Small input",      l1.getName(),                   "TC1 name");
        assertEquals(savedId,            l1.getProjectId(),              "TC1 projectId FK");

        TestCase l2 = loadedTcs.get(1);
        assertEquals("--input 100",      l2.getInputArgs(),              "TC2 inputArgs");
        assertEquals("expected/tc2.txt", l2.getExpectedOutputFilePath(), "TC2 expectedOutputFilePath");
        assertEquals("Large input",      l2.getName(),                   "TC2 name");
        assertEquals(savedId,            l2.getProjectId(),              "TC2 projectId FK");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3 — getAllProjects returns all persisted projects
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("getAllProjects returns all persisted projects")
    void getAllProjectsReturnsAll() {
        Project p1 = new Project();
        p1.setName("Alpha");
        p1.setConfigurationId("cfg-a");

        Project p2 = new Project();
        p2.setName("Beta");
        p2.setConfigurationId("cfg-b");

        pm.createProject(p1);
        pm.createProject(p2);

        List<Project> all = pm.getAllProjects();
        assertEquals(2, all.size(), "Should load exactly 2 projects");

        assertTrue(all.stream().anyMatch(p -> "Alpha".equals(p.getName())), "Alpha must be present");
        assertTrue(all.stream().anyMatch(p -> "Beta".equals(p.getName())),  "Beta must be present");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4 — update project persists changes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("updateProject persists field changes")
    void updateProjectPersistsChanges() throws SQLException {
        Project p = new Project();
        p.setName("Original Name");
        p.setConfigurationId("cfg-orig");
        pm.createProject(p);

        p.setName("Updated Name");
        p.setConfigurationId("cfg-new");
        pm.updateProject(p);

        Project reloaded = DatabaseManager.getInstance().loadProject(p.getId());
        assertNotNull(reloaded);
        assertEquals("Updated Name", reloaded.getName(),             "name after update");
        assertEquals("cfg-new",      reloaded.getConfigurationId(), "configurationId after update");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5 — deleteProject removes the row
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("deleteProject removes the project from the DB")
    void deleteProjectRemovesRow() throws SQLException {
        Project p = new Project();
        p.setName("To Be Deleted");
        p.setConfigurationId("cfg-del");
        pm.createProject(p);
        int id = p.getId();

        pm.deleteProject(id);

        Project reloaded = DatabaseManager.getInstance().loadProject(id);
        assertNull(reloaded, "Project should be null after deletion");
    }
}
