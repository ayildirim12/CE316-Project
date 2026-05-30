package com.iae.ui;

import com.iae.model.TestCase;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;

public class TestCaseDialogController {

    @FXML private Label     dialogTitle;
    @FXML private TextField inputArgsField;
    @FXML private TextField expectedOutputFileField;

    private Stage    stage;
    private TestCase result;
    private TestCase editTarget;

    /** Factory — loads FXML and creates a modal Stage owned by {@code owner}. */
    public static TestCaseDialogController create(Window owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                TestCaseDialogController.class.getResource(
                        "/com/iae/fxml/TestCaseDialog.fxml"));
        Parent root = loader.load();
        TestCaseDialogController ctrl = loader.getController();

        Stage dlg = new Stage(StageStyle.DECORATED);
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.initOwner(owner);
        dlg.setTitle("Test Case");
        dlg.setResizable(false);
        dlg.setScene(new Scene(root));
        ctrl.stage = dlg;
        return ctrl;
    }

    /** Populate the form for editing an existing test case. */
    public void setTestCase(TestCase tc) {
        editTarget = tc;
        dialogTitle.setText("Edit Test Case");
        inputArgsField.setText(nullSafe(tc.getInputArgs()));
        expectedOutputFileField.setText(nullSafe(tc.getExpectedOutputFilePath()));
    }

    /** Returns the created/updated TestCase, or {@code null} if cancelled. */
    public TestCase getResult() { return result; }

    /** Opens the dialog and blocks until closed. */
    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    @FXML
    private void handleBrowseOutputFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Expected Output File");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files", "*.txt", "*.out", "*.expected"));
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All files", "*.*"));

        String current = expectedOutputFileField.getText().trim();
        if (!current.isEmpty()) {
            File currentFile = new File(current);
            File parent = currentFile.getParentFile();
            if (parent != null && parent.exists()) fc.setInitialDirectory(parent);
        } else {
            File home = new File(System.getProperty("user.home"));
            if (home.exists()) fc.setInitialDirectory(home);
        }

        File file = fc.showOpenDialog(stage);
        if (file != null) expectedOutputFileField.setText(file.getAbsolutePath());
    }

    @FXML
    private void handleAdd() {
        String outputFile = expectedOutputFileField.getText().trim();
        if (outputFile.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "Expected output file is required.", ButtonType.OK)
                    .showAndWait();
            return;
        }

        TestCase tc = (editTarget != null) ? editTarget : new TestCase();
        tc.setInputArgs(inputArgsField.getText().trim());
        tc.setExpectedOutputFilePath(outputFile);

        result = tc;
        stage.close();
    }

    @FXML
    private void handleCancel() {
        result = null;
        stage.close();
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }
}
