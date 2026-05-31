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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationDialogController {

    @FXML private Label             dialogTitle;
    @FXML private TextField         nameField;
    @FXML private ComboBox<String>  languageCombo;
    @FXML private TextField         sourceFileField;
    @FXML private CheckBox          needsCompilationCheck;
    @FXML private VBox              compileSection;
    @FXML private TextField         compileCommandField;
    @FXML private TextField         compileArgsField;
    @FXML private TextField         runCommandField;
    @FXML private TextField         runArgsField;

    private Stage         stage;
    private Configuration result;
    private Configuration editTarget;
    private boolean       settingConfiguration = false;

    private record Preset(
            String source,
            boolean compile,
            String compileCmd,
            String compileArgs,
            String runCmd,
            String runArgs) {}

    private static final Map<String, Preset> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("Java",       new Preset("Main.java",    true,  "javac", "",          "java",    "-cp . Main"));
        PRESETS.put("C",          new Preset("main.c",       true,  "gcc",   "-o main",   "./main",  ""));
        PRESETS.put("C++",        new Preset("main.cpp",     true,  "g++",   "-o main",   "./main",  ""));
        PRESETS.put("Python",     new Preset("solution.py",  false, "",      "",          "python",  "solution.py"));
        PRESETS.put("JavaScript", new Preset("solution.js",  false, "",      "",          "node",    "solution.js"));
        PRESETS.put("Ruby",       new Preset("solution.rb",  false, "",      "",          "ruby",    "solution.rb"));
        PRESETS.put("Go",         new Preset("main.go",      true,  "go",    "build -o main", "./main", ""));
        PRESETS.put("Rust",       new Preset("main.rs",      true,  "rustc", "-o main",   "./main",  ""));
        PRESETS.put("Haskell",    new Preset("Main.hs",      true,  "ghc",   "-o main",   "./main",  ""));
    }

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
        languageCombo.getItems().addAll(PRESETS.keySet());
        languageCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!settingConfiguration && newVal != null && PRESETS.containsKey(newVal)) {
                applyPreset(PRESETS.get(newVal));
            }
        });
        updateCompileSection();
    }

    private void applyPreset(Preset p) {
        sourceFileField.setText(p.source());
        needsCompilationCheck.setSelected(p.compile());
        compileCommandField.setText(p.compileCmd());
        compileArgsField.setText(p.compileArgs());
        runCommandField.setText(p.runCmd());
        runArgsField.setText(p.runArgs());
        updateCompileSection();
    }

    public void setConfiguration(Configuration config) {
        editTarget = config;
        dialogTitle.setText("Edit Configuration");
        settingConfiguration = true;
        nameField.setText(config.getName());
        languageCombo.setValue(nullSafe(config.getLanguage()));
        sourceFileField.setText(nullSafe(config.getSourceFile()));
        needsCompilationCheck.setSelected(config.isNeedsCompilation());
        compileCommandField.setText(nullSafe(config.getCompileCommand()));
        compileArgsField.setText(nullSafe(config.getCompileArgs()));
        runCommandField.setText(nullSafe(config.getRunCommand()));
        runArgsField.setText(nullSafe(config.getRunArgs()));
        settingConfiguration = false;
        updateCompileSection();
    }

    public Configuration getResult() { return result; }

    public void showAndWait() {
        if (stage != null) stage.showAndWait();
    }

    @FXML
    private void handleNeedsCompilationToggle() {
        updateCompileSection();
    }

    @FXML
    private void handleSave() {
        String name        = nameField.getText().trim();
        String language    = nullSafe(languageCombo.getValue()).trim();
        String sourceFile  = sourceFileField.getText().trim();
        String runCommand  = runCommandField.getText().trim();
        String compileCmd  = compileCommandField.getText().trim();

        StringBuilder errors = new StringBuilder();
        if (name.isEmpty())       errors.append("• Configuration Name\n");
        if (language.isEmpty())   errors.append("• Language\n");
        if (sourceFile.isEmpty()) errors.append("• Source File Name\n");
        if (runCommand.isEmpty()) errors.append("• Run Command\n");
        if (needsCompilationCheck.isSelected() && compileCmd.isEmpty())
            errors.append("• Compile Command\n");

        if (!errors.isEmpty()) {
            new Alert(Alert.AlertType.WARNING,
                    "The following fields are required:\n\n" + errors,
                    ButtonType.OK).showAndWait();
            return;
        }

        Configuration config = (editTarget != null) ? editTarget : new Configuration();
        config.setName(name);
        config.setLanguage(nullSafe(languageCombo.getValue()).trim());
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
        if (stage != null) stage.sizeToScene();
    }

    private static String nullSafe(String s) { return s != null ? s : ""; }
}
