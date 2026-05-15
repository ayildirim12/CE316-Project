package com.iae.ui;

import com.iae.logic.ConfigurationManager;
import com.iae.model.Configuration;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;

public class ConfigurationDialogController {

    @FXML private Label    dialogTitle;
    @FXML private TextField nameField;
    @FXML private TextField languageField;
    @FXML private TextField sourceFileField;
    @FXML private CheckBox  needsCompilationCheck;
    @FXML private VBox      compileSection;
    @FXML private TextField compileCommandField;
    @FXML private TextField compileArgsField;
    @FXML private TextField runCommandField;
    @FXML private TextField runArgsField;

    private Stage         stage;
    private Configuration result;
    private Configuration editTarget;

    /** Factory — loads FXML, creates a modal Stage owned by {@code owner}. */
    public static ConfigurationDialogController create(Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ConfigurationDialogController.class.getResource(
                        "/com/iae/fxml/ConfigurationDialog.fxml"));
        Parent root = loader.load();
        ConfigurationDialogController ctrl = loader.getController();

        Stage dlg = new Stage(StageStyle.DECORATED);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.initOwner(owner);
        dlg.setTitle("Configuration");
        dlg.setResizable(false);
        dlg.setScene(new Scene(root));
        ctrl.stage = dlg;
        return ctrl;
    }

    @FXML
    public void initialize() {
        updateCompileSection();
    }

    /** Populate the form fields when editing an existing configuration. */
    public void setConfiguration(Configuration config) {
        editTarget = config;
        dialogTitle.setText("Edit Configuration");
        nameField.setText(config.getName());
        languageField.setText(nullSafe(config.getLanguage()));
        sourceFileField.setText(nullSafe(config.getSourceFile()));
        needsCompilationCheck.setSelected(config.isNeedsCompilation());
        compileCommandField.setText(nullSafe(config.getCompileCommand()));
        compileArgsField.setText(nullSafe(config.getCompileArgs()));
        runCommandField.setText(nullSafe(config.getRunCommand()));
        runArgsField.setText(nullSafe(config.getRunArgs()));
        updateCompileSection();
    }

    /** Returns the saved/updated Configuration, or {@code null} if cancelled. */
    public Configuration getResult() { return result; }

    /** Opens the dialog and blocks until it is closed. */
    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    @FXML
    private void handleNeedsCompilationToggle() {
        updateCompileSection();
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Configuration name is required.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        Configuration config = (editTarget != null) ? editTarget : new Configuration();
        config.setName(name);
        config.setLanguage(languageField.getText().trim());
        config.setSourceFile(sourceFileField.getText().trim());
        config.setNeedsCompilation(needsCompilationCheck.isSelected());
        config.setCompileCommand(compileCommandField.getText().trim());
        config.setCompileArgs(compileArgsField.getText().trim());
        config.setRunCommand(runCommandField.getText().trim());
        config.setRunArgs(runArgsField.getText().trim());

        ConfigurationManager.getInstance().save(config);
        result = config;
        stage.close();
    }

    @FXML
    private void handleCancel() {
        result = null;
        stage.close();
    }

    private void updateCompileSection() {
        boolean compile = needsCompilationCheck.isSelected();
        compileSection.setVisible(compile);
        compileSection.setManaged(compile);
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }
}
