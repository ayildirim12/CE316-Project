package com.iae.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.iae.logic.ConfigurationManager;
import com.iae.model.Configuration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for the Import / Export configuration dialog.
 *
 * <p>The Import tab reads a {@code .json} file, validates the required fields and
 * persists it as a <em>new</em> {@link Configuration} via {@link ConfigurationManager}.
 * The Export tab serialises a selected configuration to JSON using Gson.
 */
public class ImportExportDialogController {

    @FXML private TextField importPathField;
    @FXML private Label     importFeedbackLabel;
    @FXML private ComboBox<Configuration> exportConfigCombo;
    @FXML private TextField exportPathField;
    @FXML private Label     exportFeedbackLabel;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private Stage   stage;
    /** Set to {@code true} when an import or export succeeds, so callers can refresh. */
    private boolean changed = false;

    /** Factory — loads FXML, creates a modal Stage owned by {@code owner}. */
    public static ImportExportDialogController create(Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ImportExportDialogController.class.getResource(
                        "/com/iae/fxml/ImportExportDialog.fxml"));
        Parent root = loader.load();
        ImportExportDialogController ctrl = loader.getController();

        Stage dlg = new Stage(StageStyle.DECORATED);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.initOwner(owner);
        dlg.setTitle("Import / Export Configuration");
        dlg.setResizable(false);
        dlg.setScene(new Scene(root));
        ctrl.stage = dlg;
        return ctrl;
    }

    @FXML
    public void initialize() {
        StringConverter<Configuration> converter = new StringConverter<>() {
            @Override public String toString(Configuration c) {
                return c == null ? "" : c.getName();
            }
            @Override public Configuration fromString(String s) { return null; }
        };
        exportConfigCombo.setConverter(converter);
        exportConfigCombo.setButtonCell(new ConfigCell());
        exportConfigCombo.setCellFactory(list -> new ConfigCell());
        exportConfigCombo.getItems().setAll(ConfigurationManager.getInstance().findAll());
    }

    /** Opens the dialog and blocks until it is closed. */
    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    /** @return {@code true} if a configuration was imported or exported. */
    public boolean isChanged() { return changed; }

    /* ── Import ── */

    @FXML
    private void handleBrowseImportFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Configuration File");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        var file = fc.showOpenDialog(stage);
        if (file != null) {
            importPathField.setText(file.getAbsolutePath());
            importFeedbackLabel.setText("");
        }
    }

    @FXML
    private void handleConfirmImport() {
        String path = importPathField.getText();
        if (path == null || path.isBlank()) {
            setImportFeedback("Please choose a configuration file first.", true);
            return;
        }

        Configuration imported;
        try {
            String json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
            imported = gson.fromJson(json, Configuration.class);
        } catch (JsonSyntaxException e) {
            setImportFeedback("Invalid JSON: " + e.getMessage(), true);
            return;
        } catch (IOException e) {
            setImportFeedback("Could not read file: " + e.getMessage(), true);
            return;
        }

        String validationError = validate(imported);
        if (validationError != null) {
            setImportFeedback(validationError, true);
            return;
        }

        try {
            // Treat the import as a new configuration so an existing one is never
            // silently overwritten by a stale id from the file.
            imported.setId(0);
            ConfigurationManager.getInstance().save(imported);
        } catch (RuntimeException e) {
            setImportFeedback("Could not save configuration: " + e.getMessage(), true);
            return;
        }

        changed = true;
        showInfo("Configuration \"" + imported.getName() + "\" imported successfully.");
        stage.close();
    }

    /**
     * Validates the required fields of an imported configuration.
     *
     * @return an error message, or {@code null} if the configuration is valid
     */
    private static String validate(Configuration c) {
        if (c == null)                       return "The file does not contain a configuration.";
        if (isBlank(c.getName()))            return "Missing required field: name.";
        if (isBlank(c.getLanguage()))        return "Missing required field: language.";
        if (isBlank(c.getSourceFile()))      return "Missing required field: sourceFile.";
        if (isBlank(c.getRunCommand()))      return "Missing required field: runCommand.";
        if (c.isNeedsCompilation() && isBlank(c.getCompileCommand()))
            return "Missing required field: compileCommand (configuration requires compilation).";
        return null;
    }

    /* ── Export ── */

    @FXML
    private void handleBrowseExportPath() {
        Configuration selected = exportConfigCombo.getValue();
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Configuration");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        if (selected != null) {
            fc.setInitialFileName(safeFileName(selected.getName()) + ".json");
        }
        var file = fc.showSaveDialog(stage);
        if (file != null) {
            exportPathField.setText(file.getAbsolutePath());
            setExportFeedback("", false);
        }
    }

    @FXML
    private void handleExport() {
        Configuration selected = exportConfigCombo.getValue();
        if (selected == null) {
            setExportFeedback("Please select a configuration to export.", true);
            return;
        }
        String path = exportPathField.getText();
        if (path == null || path.isBlank()) {
            setExportFeedback("Please choose a destination file.", true);
            return;
        }

        try {
            String json = gson.toJson(selected);
            Files.writeString(Path.of(path), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            setExportFeedback("Could not write file: " + e.getMessage(), true);
            return;
        }

        changed = true;
        showInfo("Configuration \"" + selected.getName() + "\" exported successfully.");
        stage.close();
    }

    /* ── Shared ── */

    @FXML
    private void handleCancel() {
        if (stage != null) stage.close();
    }

    private void setImportFeedback(String msg, boolean error) {
        importFeedbackLabel.setText(msg);
        importFeedbackLabel.setStyle(error ? "-fx-text-fill:#c62828;" : "-fx-text-fill:#2e7d32;");
    }

    private void setExportFeedback(String msg, boolean error) {
        exportFeedbackLabel.setText(msg);
        exportFeedbackLabel.setStyle(error ? "-fx-text-fill:#c62828;" : "-fx-text-fill:#2e7d32;");
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.initOwner(stage);
        a.showAndWait();
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static String safeFileName(String name) {
        return name == null ? "configuration" : name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /** Renders a {@link Configuration} by its name in the combo box. */
    private static final class ConfigCell extends ListCell<Configuration> {
        @Override protected void updateItem(Configuration item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.getName());
        }
    }
}
