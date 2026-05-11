package com.iae.ui;

import com.iae.logic.ConfigurationManager;
import com.iae.logic.EvaluationEngine;
import com.iae.logic.ProjectManager;
import com.iae.logic.ReportManager;
import com.iae.model.Configuration;
import com.iae.model.EvaluationResult;
import com.iae.model.Project;
import com.iae.model.Submission;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the main application window:
 * menu bar, sidebar project tree, project-detail pane, and Run button.
 *
 * Internal data is represented as plain Strings / String[] arrays so that
 * this controller compiles and runs while teammates implement the model and
 * logic layers. Each integration point is marked with a TODO comment.
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

    /* ── UI state (no model method calls – integration via TODO) ── */
    private String                currentProjectName   = null;
    private String                currentConfigName    = null;
    private String                submissionsDir       = null;
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

        /*
         * TODO: replace demo list with real persistence once
         *       Sine's ProjectManager.getAllProjects() is implemented:
         *
         *   ProjectManager pm = new ProjectManager();
         *   for (Project p : pm.getAllProjects())
         *       root.getChildren().add(new TreeItem<>(p.getName()));
         */
        List<String> demoNames = List.of("CS101 – Homework 1", "Data Structures – Lab 4");
        for (String name : demoNames)
            root.getChildren().add(new TreeItem<>(name));

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

        /*
         * TODO: load the real Project object and resolve Configuration once
         *       Sine's ProjectManager and Ayşenaz's ConfigurationManager exist:
         *
         *   Project p = projectManager.getAllProjects().stream()
         *       .filter(x -> x.getName().equals(name)).findFirst().orElse(null);
         *   currentConfigName = configManager.getConfiguration(p.getConfigurationId()).getName();
         *   submissionsDir    = p.getSubmissionsDirectory();
         *   testCaseRows.setAll( convert(p.getTestCases()) );
         */
        currentConfigName = "Java Standard 17";
        submissionsDir    = "";
        testCaseRows.setAll(
                new String[]{"--input 5",  "expected/tc1.txt"},
                new String[]{"--input 10", "expected/tc2.txt"}
        );

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

    /* Delegate to Ayşenaz's controllers (stubs until she implements them) */
    @FXML private void handleNewConfiguration()  { /* TODO: open ConfigurationDialogController */ }
    @FXML private void handleEditConfiguration() { /* TODO: open ConfigurationDialogController */ }
    @FXML private void handleHelp()              { /* TODO: open HelpController */ }

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

        int tcCount = testCaseRows.size();

        Thread worker = new Thread(() -> {
            /*
             * TODO: replace demo simulation with real evaluation once
             *       Furkan's EvaluationEngine is implemented:
             *
             *   EvaluationEngine engine = new EvaluationEngine();
             *   List<EvaluationResult> real = engine.execute(project, config, progress);
             *   // then convert EvaluationResult list → String[][] and call initData()
             */
            String[][] demoResults = runDemoEvaluation(tcCount, progress);

            Platform.runLater(() -> {
                progress.close();
                runBtn.setDisable(false);
                setStatus("Evaluation complete – "
                        + demoResults.length + " submission(s) processed.");
                openResultsView(demoResults, tcCount);
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
            /*
             * TODO: persist via ProjectManager once Sine implements it:
             *   projectManager.updateProject(currentProject);
             */
        }
    }

    /* ── Add test case ── */

    @FXML
    private void handleAddTestCase() {
        testCaseRows.add(new String[]{"", ""});
        /*
         * TODO: persist via ProjectManager once Sine implements it.
         */
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
        TextInputDialog dlg = new TextInputDialog("New Project");
        dlg.setTitle("New Project");
        dlg.setHeaderText("Enter project name:");
        dlg.setContentText("Name:");
        dlg.showAndWait().ifPresent(name -> {
            if (name.isBlank()) return;
            /*
             * TODO: persist via ProjectManager:
             *   projectManager.createProject(new Project(...));
             */
            TreeItem<String> root = projectTreeView.getRoot();
            root.getChildren().add(new TreeItem<>(name));
            projectTreeView.getSelectionModel().selectLast();
        });
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

    /* ── Demo evaluation (pure UI simulation – no model method calls) ── */

    /**
     * Simulates evaluation for the progress dialog demo.
     * Each row format: [studentId, compileStatus, tc1..tcN, overallStatus, errorMessage]
     * Array length per row: 4 + tcCount
     *
     * TODO: Remove this method once Furkan's EvaluationEngine is integrated.
     */
    private String[][] runDemoEvaluation(int tcCount, ProgressDialog progress) {
        String[] students = {"S-10290", "S-10291", "S-10292", "S-10293", "S-10294"};
        int total   = students.length;
        int rowLen  = 4 + tcCount; // [studentId, compile, tc1..tcN, status, error]
        List<String[]> rows = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            if (progress.isCancelled()) break;
            String sid = students[i];
            progress.onProgress(i + 1, total, sid);
            try { Thread.sleep(450); } catch (InterruptedException ignored) {}

            String[] row = new String[rowLen];
            row[0] = sid;

            if (i == 3) {                              // compile failure
                row[1] = "FAILED";
                for (int t = 0; t < tcCount; t++) row[2 + t] = "—";
                row[2 + tcCount] = "COMPILE_ERROR";
                row[3 + tcCount] = "Syntax error – Main.java:12";
            } else {
                row[1] = "OK";
                boolean allPass = true;
                for (int t = 0; t < tcCount; t++) {
                    boolean pass = !(i == 2 && t == 1); // S-10292 fails TC2
                    row[2 + t] = pass ? "PASS" : "FAIL";
                    if (!pass) allPass = false;
                }
                row[2 + tcCount] = allPass ? "SUCCESS" : "WRONG_OUTPUT";
                row[3 + tcCount] = "";
            }
            progress.appendLog(sid + " → " + row[2 + tcCount]);
            rows.add(row);
        }
        progress.onComplete();
        return rows.toArray(new String[0][]);
    }

    /* ── Utilities ── */

    private void setStatus(String msg) { if (statusLabel != null) statusLabel.setText(msg); }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
