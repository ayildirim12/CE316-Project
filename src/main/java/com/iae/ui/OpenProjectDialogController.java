package com.iae.ui;

import com.iae.logic.DatabaseManager;
import com.iae.logic.ProjectManager;
import com.iae.model.Project;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.util.List;

/**
 * Controller for OpenProjectDialog.fxml.
 *
 * <p>Replaces the {@link javafx.scene.control.ChoiceDialog}-based Open Project flow
 * with a proper dialog that:
 * <ul>
 *   <li>Lists all saved projects with their last-run date (derived from the RESULTS table).</li>
 *   <li>Supports live filtering by project name.</li>
 *   <li>Has Cancel and Open buttons; Open is disabled until a row is selected.</li>
 * </ul>
 *
 * <p>Usage (same factory pattern as the other dialog controllers):
 * <pre>{@code
 *   OpenProjectDialogController dlg = OpenProjectDialogController.create(owner);
 *   dlg.showAndWait();
 *   Project selected = dlg.getResult();   // null if cancelled
 * }</pre>
 */
public class OpenProjectDialogController {

    // ── FXML injections ───────────────────────────────────────────────────────

    @FXML private TextField                      filterField;
    @FXML private TableView<Project>             projectsTable;
    @FXML private TableColumn<Project, String>   nameCol;
    @FXML private TableColumn<Project, String>   lastRunCol;
    @FXML private Button                         openBtn;

    // ── State ─────────────────────────────────────────────────────────────────

    private Stage   stage;
    private Project result;

    /** Master list — never filtered directly; FilteredList wraps it. */
    private final ObservableList<Project> allProjects = FXCollections.observableArrayList();

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Loads OpenProjectDialog.fxml and creates a resizable modal {@link Stage}.
     *
     * @param owner the parent window (may be {@code null})
     * @return a ready-to-show controller
     * @throws IOException if the FXML cannot be loaded
     */
    public static OpenProjectDialogController create(Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                OpenProjectDialogController.class.getResource(
                        "/com/iae/fxml/OpenProjectDialog.fxml"));
        Parent root = loader.load();
        OpenProjectDialogController ctrl = loader.getController();

        Stage dlg = new Stage(StageStyle.DECORATED);
        dlg.initModality(Modality.WINDOW_MODAL);
        if (owner != null) dlg.initOwner(owner);
        dlg.setTitle("Open Project");
        dlg.setResizable(true);
        dlg.setScene(new Scene(root));
        ctrl.stage = dlg;
        return ctrl;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        buildTable();
        loadProjects();
        wireFilter();
        wireSelection();
    }

    /** Opens the dialog and blocks until it is closed. */
    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    /** Returns the selected {@link Project}, or {@code null} if cancelled. */
    public Project getResult() { return result; }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleOpen() {
        result = projectsTable.getSelectionModel().getSelectedItem();
        if (stage != null) stage.close();
    }

    @FXML
    private void handleCancel() {
        result = null;
        if (stage != null) stage.close();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void buildTable() {
        // Name column
        nameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getName()));

        // Last-run column — derived from the existing RESULTS table (no schema change)
        lastRunCol.setCellValueFactory(c -> {
            int projectId = c.getValue().getId();
            String info = DatabaseManager.getInstance().getLastRunInfo(projectId);
            return new SimpleStringProperty(info);
        });

        // Double-click to open
        projectsTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && !projectsTable.getSelectionModel().isEmpty()) {
                handleOpen();
            }
        });
    }

    private void loadProjects() {
        List<Project> projects = ProjectManager.getInstance().getAllProjects();
        allProjects.setAll(projects);
    }

    private void wireFilter() {
        FilteredList<Project> filtered = new FilteredList<>(allProjects, p -> true);

        filterField.textProperty().addListener((obs, oldText, newText) -> {
            filtered.setPredicate(project -> {
                if (newText == null || newText.isBlank()) return true;
                String lower = newText.toLowerCase();
                return project.getName() != null
                        && project.getName().toLowerCase().contains(lower);
            });
        });

        projectsTable.setItems(filtered);
    }

    private void wireSelection() {
        // Open button enabled only when a row is selected
        openBtn.setDisable(true);
        projectsTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> openBtn.setDisable(newVal == null));
    }
}
