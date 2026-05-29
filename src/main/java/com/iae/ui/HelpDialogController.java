package com.iae.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;

/**
 * Controller for HelpDialog.fxml — the resizable user-manual modal.
 *
 * <p>Uses the same factory pattern as {@link ConfigurationDialogController}:
 * call {@link #create(Window)} to obtain a controller with a ready-to-show Stage,
 * then call {@link #showAndWait()}.
 */
public class HelpDialogController {

    /** Two-element String array: [action, shortcut] */
    @FXML private TableView<String[]>             shortcutsTable;
    @FXML private TableColumn<String[], String>   actionCol;
    @FXML private TableColumn<String[], String>   shortcutCol;

    private Stage stage;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Loads HelpDialog.fxml and creates a resizable modal {@link Stage}
     * owned by {@code owner}.
     *
     * @param owner the parent window (may be {@code null})
     * @return a ready-to-show controller
     * @throws IOException if the FXML cannot be loaded
     */
    public static HelpDialogController create(Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                HelpDialogController.class.getResource(
                        "/com/iae/fxml/HelpDialog.fxml"));
        Parent root = loader.load();
        HelpDialogController ctrl = loader.getController();

        Stage dlg = new Stage(StageStyle.DECORATED);
        dlg.initModality(Modality.WINDOW_MODAL);
        if (owner != null) dlg.initOwner(owner);
        dlg.setTitle("IAE – User Manual");
        dlg.setResizable(true);
        dlg.setScene(new Scene(root));
        ctrl.stage = dlg;
        return ctrl;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        buildShortcutsTable();
    }

    /** Opens the dialog and blocks until it is closed. */
    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleClose() {
        if (stage != null) stage.close();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void buildShortcutsTable() {
        if (shortcutsTable == null) return;

        actionCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        shortcutCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));

        ObservableList<String[]> rows = FXCollections.observableArrayList(
            new String[]{"Run Evaluation",        "F9"},
            new String[]{"New Project",            "Ctrl + N"},
            new String[]{"Open Project",           "Ctrl + O"},
            new String[]{"Save Project",           "Ctrl + S"},
            new String[]{"Close Project",          "(Menu only)"},
            new String[]{"New Configuration",      "(Menu only)"},
            new String[]{"Exit Application",       "Alt + F4"}
        );
        shortcutsTable.setItems(rows);
    }
}
