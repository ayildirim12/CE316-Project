package com.iae.ui;

import com.iae.logic.DatabaseManager;
import com.iae.model.EvaluationResult;
import com.iae.model.Project;
import com.iae.model.Status;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmissionsViewController {

    // ── FXML ──────────────────────────────────────────────────────────────────

    @FXML private Label     dirLabel;
    @FXML private VBox      placeholderPane;
    @FXML private TableView<String[]> submissionsTable;

    @FXML private TableColumn<String[], String> colStudent;
    @FXML private TableColumn<String[], String> colFile;
    @FXML private TableColumn<String[], String> colSize;
    @FXML private TableColumn<String[], String> colExtraction;
    @FXML private TableColumn<String[], String> colEvalStatus;

    // ── State ─────────────────────────────────────────────────────────────────

    private Project currentProject;
    private final ObservableList<String[]> rows = FXCollections.observableArrayList();

    // ── Init ──────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        submissionsTable.setItems(rows);

        colStudent   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        colFile      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        colSize      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        colExtraction.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        colEvalStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));
    }

    public void initProject(Project project) {
        this.currentProject = project;
        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        rows.clear();

        if (currentProject == null
                || currentProject.getSubmissionsDirectory() == null
                || currentProject.getSubmissionsDirectory().isBlank()) {
            dirLabel.setText("No directory set");
            showPlaceholder(true);
            return;
        }

        File dir = new File(currentProject.getSubmissionsDirectory());
        dirLabel.setText(dir.getAbsolutePath());

        if (!dir.isDirectory()) {
            showPlaceholder(true);
            return;
        }

        // Build a map studentId → latest eval status from the database
        Map<String, String> evalStatusMap = buildEvalStatusMap();

        // Scan for ZIP files (and pre-extracted directories)
        File[] entries = dir.listFiles();
        if (entries != null) {
            for (File f : entries) {
                String name = f.getName();
                if (f.isFile() && name.toLowerCase().endsWith(".zip")) {
                    String studentId  = name.substring(0, name.length() - 4);
                    String sizeStr    = humanReadableSize(f.length());
                    String extraction = extractionStatus(dir, f, studentId);
                    String evalStatus = evalStatusMap.getOrDefault(studentId, "—");
                    rows.add(new String[]{ studentId, name, sizeStr, extraction, evalStatus });
                }
            }
        }

        showPlaceholder(rows.isEmpty());
    }

    /**
     * Determines the extraction status of a ZIP file.
     *
     * Order of checks:
     *   1. If the ZIP file itself is unreadable/corrupt → ERROR
     *   2. If .extracted/studentId/ exists              → EXTRACTED
     *   3. If any entry in .extracted/ starts with studentId → EXTRACTED
     *   4. Otherwise                                    → PENDING
     */
    private String extractionStatus(File submissionsDir, File zipFile, String studentId) {
        // 1. Validate the ZIP
        if (!isReadableZip(zipFile)) return "ERROR";

        File extractBase = new File(submissionsDir, ".extracted");
        if (!extractBase.isDirectory()) return "PENDING";

        // 2. Standard structure: .extracted/studentId/
        File studentDir = new File(extractBase, studentId);
        if (studentDir.isDirectory()) return "EXTRACTED";

        // 3. Flat structure: any entry whose name starts with studentId
        File[] contents = extractBase.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (f.getName().startsWith(studentId)) return "EXTRACTED";
            }
        }

        return "PENDING";
    }

    /** Returns {@code true} if the file can be opened as a valid ZIP archive. */
    private static boolean isReadableZip(File f) {
        try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(f)) {
            return zf.size() >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> buildEvalStatusMap() {
        Map<String, String> map = new HashMap<>();
        if (currentProject == null || currentProject.getId() == 0) return map;
        try {
            List<EvaluationResult> results =
                    DatabaseManager.getInstance().getResultsForProject(currentProject.getId());
            // Iterate in order; later entries overwrite earlier ones → "last" result wins
            for (EvaluationResult r : results) {
                Status s = r.getStatus();
                String label = (s != null) ? statusLabel(s) : "—";
                map.put(r.getStudentId(), label);
            }
        } catch (Exception ignored) { /* DB not available — show no status */ }
        return map;
    }

    private static String statusLabel(Status s) {
        return switch (s) {
            case SUCCESS       -> "✓ PASS";
            case WRONG_OUTPUT  -> "✗ FAIL";
            case COMPILE_ERROR -> "⚠ COMPILE ERR";
            case RUNTIME_ERROR -> "⚠ RUNTIME ERR";
            case TIMEOUT       -> "⏱ TIMEOUT";
            case SOURCE_MISSING-> "— MISSING";
            default            -> s.name();
        };
    }

    private static String humanReadableSize(long bytes) {
        if (bytes < 1024)            return bytes + " B";
        if (bytes < 1024 * 1024)     return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private void showPlaceholder(boolean show) {
        placeholderPane.setVisible(show);
        placeholderPane.setManaged(show);
        submissionsTable.setVisible(!show);
        submissionsTable.setManaged(!show);
    }
}
