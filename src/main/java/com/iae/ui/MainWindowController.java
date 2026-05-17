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
    private String                currentProjectName   = null;
    private String                currentConfigName    = null;
    private String                submissionsDir       = null;
    private Project               currentProject       = null;
    private final ObservableList<String[]> testCaseRows =
            FXCollections.observableArrayList();

    /* ── Init ── */

    @FXML
    public void initialize() {
        buildProjectTree();
        buildTestCasesTable();
        showNoProjectPane();
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
        tcInputCol.setOnEditCommit(e -> e.getRowValue()[0] = e.getNewValue());

        /* Expected Output File */
        tcOutputCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        tcOutputCol.setCellFactory(javafx.scene.control.cell.TextFieldTableCell.forTableColumn());
        tcOutputCol.setOnEditCommit(e -> e.getRowValue()[1] = e.getNewValue());

        /* Actions column – delete button */
        tcActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button del = new Button("✕");
            { del.getStyleClass().add("ghost-button");
              del.setOnAction(e -> testCaseRows.remove(getIndex())); }
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
        mainContentArea.getChildren().removeIf(n -> "resultsView".equals(n.getId()));
    }

    /* ── Menu handlers ── */

    @FXML private void handleNewProject()  { promptNewProject(); }
    @FXML private void handleOpenProject() { promptOpenProject(); }
    @FXML private void handleCloseProject() {
        currentProjectName = null;
        testCaseRows.clear();
        showNoProjectPane();
        setStatus("Project closed.");
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
                    if (saved != null) setStatus("Configuration updated: " + saved.getName());
                } catch (IOException e) {
                    showError("Could not open Configuration dialog: " + e.getMessage());
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

            Platform.runLater(() -> {
                progress.close();
                runBtn.setDisable(false);
                setStatus("Evaluation complete – " + rows.length + " submission(s) processed.");
                if (!progress.isCancelled()) openResultsView(rows, tcCount);
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
            if (currentProject != null) currentProject.addTestCase(tc);
            testCaseRows.add(new String[]{
                    nullSafe(tc.getInputArgs()),
                    nullSafe(tc.getExpectedOutputFilePath())
            });
        } catch (IOException e) {
            showError("Could not open Test Case dialog: " + e.getMessage());
        }
    }

    /* ── Nav tabs ── */

    @FXML private void showProjectsView()    { /* already showing project detail */ }
    @FXML private void showSubmissionsView() { /* deferred to final phase */ }
    @FXML private void showAnalyticsView()   { /* deferred to final phase */ }

    /* ── Load ResultsView FXML ── */

    private void openResultsView(String[][] results, int tcCount) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/iae/fxml/ResultsView.fxml"));
            Parent root = loader.load();
            ResultsViewController rvc = loader.getController();
            root.getProperties().put("mainWindowController", this);
            rvc.initData(results, tcCount);
            showResultsPane(root);
        } catch (IOException e) {
            showError("Could not load Results View: " + e.getMessage());
        }
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

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }
}
