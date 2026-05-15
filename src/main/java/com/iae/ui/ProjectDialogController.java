package com.iae.ui;

import com.iae.logic.ConfigurationManager;
import com.iae.logic.ProjectManager;
import com.iae.model.Configuration;
import com.iae.model.Project;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ProjectDialogController {

    @FXML private Label                   dialogTitle;
    @FXML private TextField               nameField;
    @FXML private ComboBox<Configuration> configComboBox;
    @FXML private TextField               submissionsDirField;

    private Stage   stage;
    private Project result;
    private Project editTarget;

    /** Factory — loads FXML and creates a modal Stage owned by {@code owner}. */
    public static ProjectDialogController create(Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ProjectDialogController.class.getResource(
                        "/com/iae/fxml/NewProjectDialog.fxml"));
        Parent root = loader.load();
        ProjectDialogController ctrl = loader.getController();

        Stage dlg = new Stage(StageStyle.DECORATED);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.initOwner(owner);
        dlg.setTitle("Project");
        dlg.setResizable(false);
        dlg.setScene(new Scene(root));
        ctrl.stage = dlg;
        return ctrl;
    }

    @FXML
    public void initialize() {
        configComboBox.setConverter(new StringConverter<>() {
            @Override public String toString(Configuration c) {
                return c == null ? "" : c.getName();
            }
            @Override public Configuration fromString(String s) { return null; }
        });

        List<Configuration> configs = ConfigurationManager.getInstance().findAll();
        configComboBox.getItems().setAll(configs);
        if (!configs.isEmpty()) configComboBox.getSelectionModel().selectFirst();
    }

    /** Populate the form for editing an existing project. */
    public void setProject(Project project) {
        editTarget = project;
        dialogTitle.setText("Edit Project");
        nameField.setText(project.getName());
        submissionsDirField.setText(nullSafe(project.getSubmissionsDirectory()));

        ConfigurationManager.getInstance()
                .findById(project.getConfigurationId())
                .ifPresent(configComboBox.getSelectionModel()::select);
    }

    /** Returns the saved/updated Project, or {@code null} if cancelled. */
    public Project getResult() { return result; }

    /** Opens the dialog and blocks until closed. */
    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    @FXML
    private void handleBrowse() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Submissions Directory");
        File dir = dc.showDialog(stage);
        if (dir != null) submissionsDirField.setText(dir.getAbsolutePath());
    }

    @FXML
    private void handleCreate() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Project name is required.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        Project project = (editTarget != null) ? editTarget : new Project();
        project.setName(name);
        project.setSubmissionsDirectory(submissionsDirField.getText().trim());

        Configuration selectedConfig = configComboBox.getValue();
        if (selectedConfig != null) project.setConfigurationId(selectedConfig.getId());

        ProjectManager.getInstance().save(project);
        result = project;
        stage.close();
    }

    @FXML
    private void handleCancel() {
        result = null;
        stage.close();
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }
}
