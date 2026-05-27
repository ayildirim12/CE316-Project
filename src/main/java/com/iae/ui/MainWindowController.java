package com.iae.ui;

import com.iae.logic.CodeRunner;
import com.iae.logic.Compiler;
import com.iae.logic.DefaultCompiler;
import com.iae.logic.ConfigurationManager;
import com.iae.logic.DatabaseManager;
import com.iae.logic.DirectoryScanner;
import com.iae.logic.EvaluationEngine;
import com.iae.logic.OutputComparator;
import com.iae.logic.ProjectManager;
import com.iae.model.ComparisonResult;
import com.iae.model.Configuration;
import com.iae.model.EvaluationResult;
import com.iae.model.ExecutionResult;
import com.iae.model.Project;
import com.iae.model.Status;
import com.iae.model.TestCase;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the main application window:
 * menu bar, sidebar project tree, project-detail pane, and Run button.
 */
public class MainWindowController {

    /* ── FXML ── */
    @FXML private TreeView<String>        projectTreeView;
    @FXML private StackPane               mainContentArea;
    @FXML private Label                   statusLabel;
    @FXML private Label                   envStatusLabel;
    @FXML private Button                  runBtn;
    @FXML private Button                  lastResultsBtn;

    @FXML private Button                  projectsTab;
    @FXML private Button                  submissionsTab;
    @FXML private Button                  analyticsTab;
    @FXML private Button                  settingsTab;

    @FXML private VBox                    noProjectPane;
    @FXML private ScrollPane              projectDetailScroll;
    @FXML private Label                   projectNameLabel;
    @FXML private Label                   configBadge;
    @FXML private TextField               submissionsDirField;

    /* Test-cases table – each row is String[]{ inputArgs, expectedOutputFile } */
    @FXML private TableView<String[]>            testCasesTable;
    @FXML private TableColumn<String[], String>  tcNumCol;
    @FXML private TableColumn<String[], String>  tcInputCol;
    @FXML private TableColumn<String[], String>  tcOutputCol;
    @FXML private TableColumn<String[], String>  tcActionCol;

    /* ── UI state ── */
    private Button                activeTab            = null;
    private String                currentProjectName   = null;
    private String                currentConfigName    = null;
    private String                submissionsDir       = null;
    private Project               currentProject       = null;
    private final ObservableList<String[]> testCaseRows =
            FXCollections.observableArrayList();

    /** Stores the last results view so the user can return to it without re-running. */
    private javafx.scene.Parent   lastResultsRoot      = null;

    /* ── Init ── */

    @FXML
    public void initialize() {
        buildProjectTree();
        buildTestCasesTable();
        showNoProjectPane();
        setActiveTab(projectsTab);
    }

    /* ── Sidebar ── */

    private void buildProjectTree() {
        TreeItem<String> root = new TreeItem<>("Projects");
        root.setExpanded(true);
        projectTreeView.setRoot(root);
        projectTreeView.setShowRoot(false);

        for (Project p : ProjectManager.getInstance().getAllProjects())
            root.getChildren().add(new TreeItem<>(p.getName()));

        projectTreeView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.isLeaf())
                        loadProjectByName(newVal.getValue());
                });
    }

    /* ── Test-cases table ── */

    private void buildTestCasesTable() {
        testCasesTable.setEditable(true);
        testCasesTable.setItems(testCaseRows);

        /* # column – row index */
        tcNumCol.setCellValueFactory(c -> {
            int idx = testCasesTable.getItems().indexOf(c.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });

        /* Input Arguments */
        tcInputCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        tcInputCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        tcInputCol.setOnEditCommit(e -> {
            e.getRowValue()[0] = e.getNewValue();
            int idx = testCasesTable.getItems().indexOf(e.getRowValue());
            if (currentProject != null && idx >= 0 && idx < currentProject.getTestCases().size()) {
                currentProject.getTestCases().get(idx).setInputArgs(e.getNewValue());
                saveCurrentProject();
            }
        });

        /* Expected Output File */
        tcOutputCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        tcOutputCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        tcOutputCol.setOnEditCommit(e -> {
            e.getRowValue()[1] = e.getNewValue();
            int idx = testCasesTable.getItems().indexOf(e.getRowValue());
            if (currentProject != null && idx >= 0 && idx < currentProject.getTestCases().size()) {
                currentProject.getTestCases().get(idx).setExpectedOutputFilePath(e.getNewValue());
                saveCurrentProject();
            }
        });

        /* Actions column – delete button */
        tcActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("✕");
            { del.getStyleClass().add("ghost-button");
              del.setOnAction(e -> {
                  int idx = getIndex();
                  testCaseRows.remove(idx);
                  if (currentProject != null && idx < currentProject.getTestCases().size()) {
                      currentProject.getTestCases().remove(idx);
                      saveCurrentProject();
                  }
              }); }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : del);
                setText(null);
            }
        });
    }

    /* ── Load project by name ── */

    private void loadProjectByName(String name) {
        currentProjectName = name;

        Project p = ProjectManager.getInstance().findByName(name).orElse(null);
        if (p != null) {
            currentProject = p;
            submissionsDir = nullSafe(p.getSubmissionsDirectory());
            currentConfigName = ConfigurationManager.getInstance()
                    .findByName(p.getConfigurationId())
                    .map(Configuration::getName)
                    .orElse("(none)");
            testCaseRows.setAll(p.getTestCases().stream()
                    .map(tc -> new String[]{
                            nullSafe(tc.getInputArgs()),
                            nullSafe(tc.getExpectedOutputFilePath())})
                    .toList());
        } else {
            currentProject    = null;
            currentConfigName = "(none)";
            submissionsDir    = "";
            testCaseRows.clear();
        }

        lastResultsRoot = null;   // clear stale results when switching projects
        if (lastResultsBtn != null) { lastResultsBtn.setVisible(false); lastResultsBtn.setManaged(false); }
        projectNameLabel.setText(currentProjectName);
        configBadge.setText(currentConfigName);
        submissionsDirField.setText(submissionsDir);
        runBtn.setDisable(false);
        showProjectDetailPane();
        setStatus("Loaded: " + name);
    }

    /* ── Visibility helpers ── */

    private void showNoProjectPane() {
        noProjectPane.setVisible(true);
        noProjectPane.setManaged(true);
        projectDetailScroll.setVisible(false);
        projectDetailScroll.setManaged(false);
        runBtn.setDisable(true);
        removeResultsPane();
    }

    private void showProjectDetailPane() {
        noProjectPane.setVisible(false);
        noProjectPane.setManaged(false);
        projectDetailScroll.setVisible(true);
        projectDetailScroll.setManaged(true);
        removeResultsPane();
    }

    private void showResultsPane(Parent root) {
        root.setId("resultsView");
        noProjectPane.setVisible(false);
        noProjectPane.setManaged(false);
        projectDetailScroll.setVisible(false);
        projectDetailScroll.setManaged(false);
        removeResultsPane();
        mainContentArea.getChildren().add(root);
    }

    private void removeResultsPane() {
        clearContentArea();
    }

    private void clearContentArea() {
        mainContentArea.getChildren()
                .retainAll(noProjectPane, projectDetailScroll);
    }

    /* ── Menu handlers ── */

    @FXML private void handleNewProject()  { promptNewProject(); }
    @FXML private void handleOpenProject() { promptOpenProject(); }
    @FXML private void handleEditProject() {
        List<Project> all = ProjectManager.getInstance().getAllProjects();
        if (all.isEmpty()) {
            showError("No projects found. Create one first.");
            return;
        }
        List<String> names = all.stream().map(Project::getName).toList();
        ChoiceDialog<String> pick = new ChoiceDialog<>(
                currentProjectName != null ? currentProjectName : names.get(0), names);
        pick.setTitle("Edit Project");
        pick.setHeaderText("Select a project to edit:");
        pick.setContentText("Project:");
        pick.showAndWait().ifPresent(chosen -> {
            ProjectManager.getInstance().findByName(chosen).ifPresent(p -> {
                try {
                    Window owner = mainContentArea.getScene().getWindow();
                    ProjectDialogController dlg = ProjectDialogController.create(owner);
                    dlg.setProject(p);
                    dlg.showAndWait();
                    Project saved = dlg.getResult();
                    if (saved != null) {
                        // Sync the tree item to the new name
                        for (TreeItem<String> item : projectTreeView.getRoot().getChildren()) {
                            if (chosen.equals(item.getValue())) {
                                item.setValue(saved.getName());
                                break;
                            }
                        }
                        if (chosen.equals(currentProjectName)) loadProjectByName(saved.getName());
                        setStatus("Project updated: " + saved.getName());
                    }
                } catch (IOException e) {
                    showError("Could not open Project dialog: " + e.getMessage());
                }
            });
        });
    }

    @FXML private void handleSaveProject() {
        if (currentProject == null) {
            setStatus("No project to save.");
            return;
        }
        try {
            ProjectManager.getInstance().saveProject(currentProject);
            setStatus("Project saved: " + currentProject.getName());
        } catch (RuntimeException e) {
            showError("Could not save project: " + e.getMessage());
        }
    }

    @FXML private void handleCloseProject() {
        currentProjectName = null;
        testCaseRows.clear();
        showNoProjectPane();
        setStatus("Project closed.");
    }

    @FXML private void handleDeleteProject() {
        List<Project> all = ProjectManager.getInstance().getAllProjects();
        if (all.isEmpty()) {
            showError("No projects found.");
            return;
        }
        List<String> names = all.stream().map(Project::getName).toList();
        ChoiceDialog<String> pick = new ChoiceDialog<>(names.get(0), names);
        pick.setTitle("Delete Project");
        pick.setHeaderText("Select a project to delete:");
        pick.setContentText("Project:");
        pick.showAndWait().ifPresent(chosen -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete \"" + chosen + "\"?\nAll test cases and results will also be deleted.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Delete Project");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    ProjectManager.getInstance().findByName(chosen).ifPresent(p -> {
                        ProjectManager.getInstance().deleteProject(p.getId());
                        projectTreeView.getRoot().getChildren()
                                .removeIf(item -> chosen.equals(item.getValue()));
                        if (chosen.equals(currentProjectName)) {
                            currentProjectName = null;
                            currentProject = null;
                            testCaseRows.clear();
                            showNoProjectPane();
                        }
                        setStatus("Project deleted: " + chosen);
                    });
                }
            });
        });
    }
    @FXML private void handleExit() { Platform.exit(); }

    @FXML private void handleNewConfiguration() {
        try {
            Window owner = mainContentArea.getScene().getWindow();
            ConfigurationDialogController dlg = ConfigurationDialogController.create(owner);
            dlg.showAndWait();
            Configuration saved = dlg.getResult();
            if (saved != null) setStatus("Configuration saved: " + saved.getName());
        } catch (IOException e) {
            showError("Could not open Configuration dialog: " + e.getMessage());
        }
    }

    @FXML private void handleEditConfiguration() {
        List<Configuration> all = ConfigurationManager.getInstance().findAll();
        if (all.isEmpty()) {
            showError("No configurations found. Create one first.");
            return;
        }
        List<String> names = all.stream().map(Configuration::getName).toList();
        ChoiceDialog<String> pick = new ChoiceDialog<>(names.get(0), names);
        pick.setTitle("Edit Configuration");
        pick.setHeaderText("Select a configuration to edit:");
        pick.setContentText("Configuration:");
        pick.showAndWait().ifPresent(chosen -> {
            ConfigurationManager.getInstance().findByName(chosen).ifPresent(config -> {
                try {
                    Window owner = mainContentArea.getScene().getWindow();
                    ConfigurationDialogController dlg = ConfigurationDialogController.create(owner);
                    dlg.setConfiguration(config);
                    dlg.showAndWait();
                    Configuration saved = dlg.getResult();
                    if (saved != null) {
                        // ── Rename guard ──────────────────────────────────────
                        // If the configuration name changed, find every project
                        // that references the old name and update it atomically.
                        String newName = saved.getName();
                        if (!chosen.equals(newName)) {
                            List<Project> affected = ProjectManager.getInstance()
                                    .getAllProjects().stream()
                                    .filter(p -> chosen.equals(p.getConfigurationId()))
                                    .toList();
                            if (!affected.isEmpty()) {
                                for (Project p : affected) {
                                    p.setConfigurationId(newName);
                                    ProjectManager.getInstance().saveProject(p);
                                }
                                // Refresh badge if the active project was affected
                                if (currentProject != null
                                        && chosen.equals(currentProject.getConfigurationId())) {
                                    currentProject.setConfigurationId(newName);
                                    configBadge.setText(newName);
                                }
                                setStatus("Configuration renamed: projects updated ("
                                        + affected.size() + " affected).");
                            } else {
                                setStatus("Configuration updated: " + newName);
                            }
                        } else {
                            setStatus("Configuration updated: " + newName);
                        }
                        // ─────────────────────────────────────────────────────
                    }
                } catch (IOException e) {
                    showError("Could not open Configuration dialog: " + e.getMessage());
                }
            });
        });
    }

    @FXML private void handleDeleteConfiguration() {
        List<Configuration> all = ConfigurationManager.getInstance().findAll();
        if (all.isEmpty()) {
            showError("No configurations found.");
            return;
        }
        List<String> names = all.stream().map(Configuration::getName).toList();
        ChoiceDialog<String> pick = new ChoiceDialog<>(names.get(0), names);
        pick.setTitle("Delete Configuration");
        pick.setHeaderText("Select a configuration to delete:");
        pick.setContentText("Configuration:");
        pick.showAndWait().ifPresent(chosen -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to delete \"" + chosen + "\"?\nThis action cannot be undone.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText("Delete Configuration");
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    ConfigurationManager.getInstance().findByName(chosen).ifPresent(config -> {
                        ConfigurationManager.getInstance().delete(config.getId());
                        setStatus("Configuration deleted: " + chosen);
                    });
                }
            });
        });
    }

    @FXML private void handleHelp()              { /* deferred to final phase */ }

    @FXML private void handleAbout() {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                "Integrated Assignment Environment\nCE 316 – Spring 2026", ButtonType.OK);
        a.setHeaderText("About IAE");
        a.showAndWait();
    }

    /* ── Run button ── */

    @FXML
    public void handleRun(ActionEvent event) {
        if (currentProjectName == null) return;
        if (submissionsDir == null || submissionsDir.isBlank()) {
            showError("Please set a submissions directory before running.");
            return;
        }
        if (currentProject == null) {
            showError("No project loaded.");
            return;
        }

        String cfgId = currentProject.getConfigurationId();
        Configuration config = (cfgId != null)
                ? ConfigurationManager.getInstance().findByName(cfgId).orElse(null)
                : null;
        if (config == null) {
            showError("No configuration is assigned to this project.\nPlease edit the project and assign one first.");
            return;
        }

        currentProject.setSubmissionsDirectory(submissionsDir);

        Stage owner = (Stage) mainContentArea.getScene().getWindow();
        ProgressDialog progress;
        try {
            progress = ProgressDialog.create(owner);
        } catch (IOException e) {
            showError("Could not open progress dialog: " + e.getMessage());
            return;
        }
        progress.show();
        runBtn.setDisable(true);
        setStatus("Running evaluation…");

        int tcCount = currentProject.getTestCases().size();
        Project projectSnapshot = currentProject;
        Configuration configSnapshot = config;

        Thread worker = new Thread(() -> {
            EvaluationEngine engine = new EvaluationEngine(
                    new DirectoryScanner(),
                    new DefaultCompiler(),
                    new CodeRunner(),
                    new OutputComparator(),
                    DatabaseManager.getInstance(),
                    progress,
                    10_000
            );

            List<EvaluationResult> evalResults;
            try {
                evalResults = engine.runProject(projectSnapshot, configSnapshot);
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    progress.close();
                    runBtn.setDisable(false);
                    showError("Evaluation error: " + ex.getMessage());
                    setStatus("Evaluation failed.");
                });
                return;
            }

            String[][] rows = toDisplayRows(evalResults, tcCount);
            List<EvaluationResult> finalEvalResults = evalResults;

            Platform.runLater(() -> {
                progress.close();
                runBtn.setDisable(false);
                setStatus("Evaluation complete – " + rows.length + " submission(s) processed.");
                if (!progress.isCancelled()) {
                    openResultsView(rows, tcCount, finalEvalResults);
                    lastResultsBtn.setVisible(true);
                    lastResultsBtn.setManaged(true);
                }
            });
        });
        worker.setDaemon(true);
        worker.start();
    }

    /* ── Browse submissions directory ── */

    @FXML
    private void handleBrowseSubmissions() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Submissions Directory");
        File dir = dc.showDialog(mainContentArea.getScene().getWindow());
        if (dir != null) {
            submissionsDir = dir.getAbsolutePath();
            submissionsDirField.setText(submissionsDir);
            if (currentProject != null) {
                currentProject.setSubmissionsDirectory(submissionsDir);
                try {
                    ProjectManager.getInstance().saveProject(currentProject);
                } catch (RuntimeException e) {
                    // Not fatal — directory is updated in memory for this session
                }
            }
        }
    }

    /* ── Add test case ── */

    @FXML
    private void handleAddTestCase() {
        try {
            Window owner = mainContentArea.getScene().getWindow();
            TestCaseDialogController dlg = TestCaseDialogController.create(owner);
            dlg.showAndWait();
            TestCase tc = dlg.getResult();
            if (tc == null) return;
            if (currentProject != null) {
                currentProject.addTestCase(tc);
                saveCurrentProject();
            }
            testCaseRows.add(new String[]{
                    nullSafe(tc.getInputArgs()),
                    nullSafe(tc.getExpectedOutputFilePath())
            });
        } catch (IOException e) {
            showError("Could not open Test Case dialog: " + e.getMessage());
        }
    }

    /* ── Nav tabs ── */

    @FXML private void showProjectsView() {
        setActiveTab(projectsTab);
        clearContentArea();
        if (currentProject != null) {
            showProjectDetailPane();
        } else {
            showNoProjectPane();
        }
    }

    @FXML private void showSubmissionsView() {
        setActiveTab(submissionsTab);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/SubmissionsView.fxml"));
            javafx.scene.Parent root = loader.load();
            SubmissionsViewController ctrl = loader.getController();
            ctrl.initProject(currentProject);
            root.setId("submissionsView");
            clearContentArea();
            noProjectPane.setVisible(false);
            noProjectPane.setManaged(false);
            projectDetailScroll.setVisible(false);
            projectDetailScroll.setManaged(false);
            mainContentArea.getChildren().add(root);
        } catch (java.io.IOException e) {
            showError("Could not load Submissions View: " + e.getMessage());
        }
    }

    @FXML private void showAnalyticsView()   { showComingSoon("Analytics View", analyticsTab); }
    @FXML private void showSettingsView()    { showComingSoon("Settings", settingsTab); }

    private void showComingSoon(String feature, Button tab) {
        setActiveTab(tab);
        Alert a = new Alert(Alert.AlertType.INFORMATION,
                feature + " will be available in the next version of IAE.",
                ButtonType.OK);
        a.setHeaderText("Coming in Next Version");
        a.showAndWait();
        setActiveTab(projectsTab);
    }

    private void setActiveTab(Button tab) {
        if (activeTab != null) activeTab.getStyleClass().remove("sidebar-tab-active");
        activeTab = tab;
        if (tab != null && !tab.getStyleClass().contains("sidebar-tab-active")) {
            tab.getStyleClass().add("sidebar-tab-active");
        }
    }

    /* ── Load ResultsView FXML ── */

    private void openResultsView(String[][] results, int tcCount, List<EvaluationResult> evalResults) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/ResultsView.fxml"));
            Parent root = loader.load();
            ResultsViewController rvc = loader.getController();
            root.getProperties().put("mainWindowController", this);
            rvc.initData(results, tcCount, evalResults);
            lastResultsRoot = root;
            showResultsPane(root);
        } catch (IOException e) {
            showError("Could not load Results View: " + e.getMessage());
        }
    }

    /** Re-displays the most recent results view without re-running the pipeline. */
    @FXML
    public void handleViewLastResults() {
        if (lastResultsRoot == null) return;
        showResultsPane(lastResultsRoot);
        setActiveTab(null);
    }

    /* ── Simple dialogs for new / open project ── */

    private void promptNewProject() {
        try {
            Window owner = mainContentArea.getScene().getWindow();
            ProjectDialogController dlg = ProjectDialogController.create(owner);
            dlg.showAndWait();
            Project created = dlg.getResult();
            if (created == null) return;
            TreeItem<String> root = projectTreeView.getRoot();
            root.getChildren().add(new TreeItem<>(created.getName()));
            projectTreeView.getSelectionModel().selectLast();
            setStatus("Project created: " + created.getName());
        } catch (IOException e) {
            showError("Could not open New Project dialog: " + e.getMessage());
        }
    }

    private void promptOpenProject() {
        TreeItem<String> root = projectTreeView.getRoot();
        if (root.getChildren().isEmpty()) {
            showError("No projects found. Create a new project first.");
            return;
        }
        List<String> names = new ArrayList<>();
        for (TreeItem<String> item : root.getChildren()) names.add(item.getValue());
        ChoiceDialog<String> dlg = new ChoiceDialog<>(names.get(0), names);
        dlg.setTitle("Open Project");
        dlg.setHeaderText("Select a project:");
        dlg.setContentText("Project:");
        dlg.showAndWait().ifPresent(this::loadProjectByName);
    }

    /* ── Convert EvaluationResult list → display rows ── */

    /**
     * Row format: [studentId, compileStatus, tc1..tcN, overallStatus, errorMessage]
     * Array length per row: 4 + tcCount
     */
    private String[][] toDisplayRows(List<EvaluationResult> results, int tcCount) {
        String[][] rows = new String[results.size()][];
        for (int i = 0; i < results.size(); i++) {
            EvaluationResult r = results.get(i);
            Status st = r.getStatus();
            String[] row = new String[4 + tcCount];
            row[0] = r.getStudentId();

            boolean compileOk = st != Status.COMPILE_ERROR && st != Status.SOURCE_MISSING;
            row[1] = compileOk ? "OK" : "FAILED";

            List<ComparisonResult> comps = r.getComparisonResults();
            List<ExecutionResult>  execs = r.getExecutionResults();
            for (int t = 0; t < tcCount; t++) {
                if (!compileOk) {
                    row[2 + t] = "—";
                } else if (t < comps.size()) {
                    row[2 + t] = comps.get(t).isMatch() ? "PASS" : "FAIL";
                } else if (t < execs.size() && execs.get(t).isTimedOut()) {
                    row[2 + t] = "TIMEOUT";
                } else {
                    row[2 + t] = "—";
                }
            }

            row[2 + tcCount] = st != null ? st.name() : "UNKNOWN";
            row[3 + tcCount] = r.getErrorMessage() != null ? r.getErrorMessage() : "";
            rows[i] = row;
        }
        return rows;
    }

    /* ── Utilities ── */

    private void saveCurrentProject() {
        if (currentProject == null) return;
        try {
            ProjectManager.getInstance().saveProject(currentProject);
        } catch (RuntimeException e) {
            setStatus("Warning: could not save project – " + e.getMessage());
        }
    }

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }
}
