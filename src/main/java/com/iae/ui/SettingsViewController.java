package com.iae.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Controller for SettingsView.fxml.
 *
 * <p>Persists two user preferences via {@link java.util.prefs.Preferences}
 * so that settings survive application restarts:
 * <ul>
 *   <li>{@code defaultTimeoutSeconds} — per-test-case execution timeout (default 10)</li>
 *   <li>{@code defaultSubmissionsDir} — pre-fills submissions directory in new projects (default "")</li>
 * </ul>
 *
 * <p>Static helpers {@link #getDefaultTimeoutMs()} and {@link #getDefaultSubmissionsDir()}
 * can be called from other controllers (e.g. {@link MainWindowController}) without
 * instantiating this controller.
 */
public class SettingsViewController {

    // ── Preference keys & defaults ────────────────────────────────────────────

    private static final String PREF_TIMEOUT_SECONDS  = "defaultTimeoutSeconds";
    private static final String PREF_SUBMISSIONS_DIR  = "defaultSubmissionsDir";

    private static final int    DEFAULT_TIMEOUT_SECS  = 10;
    private static final String DEFAULT_SUBS_DIR      = "";

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(SettingsViewController.class);

    // ── FXML injections ───────────────────────────────────────────────────────

    @FXML private TextField timeoutField;
    @FXML private TextField defaultSubmissionsDirField;
    @FXML private Label     settingsStatusLabel;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        loadPrefsIntoFields();
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    @FXML
    private void handleSave() {
        String timeoutText = timeoutField.getText().trim();
        int timeoutSecs;
        try {
            timeoutSecs = Integer.parseInt(timeoutText);
            if (timeoutSecs <= 0) throw new NumberFormatException("must be positive");
        } catch (NumberFormatException e) {
            showStatus("Timeout must be a positive integer (e.g. 10).", false);
            return;
        }

        String subsDir = defaultSubmissionsDirField.getText().trim();

        PREFS.putInt(PREF_TIMEOUT_SECONDS, timeoutSecs);
        PREFS.put(PREF_SUBMISSIONS_DIR, subsDir);

        showStatus("Settings saved.", true);
    }

    @FXML
    private void handleReset() {
        PREFS.remove(PREF_TIMEOUT_SECONDS);
        PREFS.remove(PREF_SUBMISSIONS_DIR);
        loadPrefsIntoFields();
        showStatus("Settings reset to defaults.", true);
    }

    @FXML
    private void handleBrowse() {
        Window owner = defaultSubmissionsDirField.getScene() != null
                ? defaultSubmissionsDirField.getScene().getWindow()
                : null;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Default Submissions Directory");
        String current = defaultSubmissionsDirField.getText().trim();
        if (!current.isEmpty()) {
            File cur = new File(current);
            if (cur.isDirectory()) dc.setInitialDirectory(cur);
        }
        File chosen = dc.showDialog(owner);
        if (chosen != null) {
            defaultSubmissionsDirField.setText(chosen.getAbsolutePath());
        }
    }

    // ── Static API (used by MainWindowController) ─────────────────────────────

    /**
     * Returns the persisted execution timeout in <em>milliseconds</em>.
     * Falls back to 10 000 ms if the preference has not been set.
     */
    public static int getDefaultTimeoutMs() {
        return PREFS.getInt(PREF_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECS) * 1_000;
    }

    /**
     * Returns the persisted default submissions directory path,
     * or an empty string if not set.
     */
    public static String getDefaultSubmissionsDir() {
        return PREFS.get(PREF_SUBMISSIONS_DIR, DEFAULT_SUBS_DIR);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void loadPrefsIntoFields() {
        timeoutField.setText(
                String.valueOf(PREFS.getInt(PREF_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECS)));
        defaultSubmissionsDirField.setText(
                PREFS.get(PREF_SUBMISSIONS_DIR, DEFAULT_SUBS_DIR));
        if (settingsStatusLabel != null) settingsStatusLabel.setText("");
    }

    private void showStatus(String message, boolean success) {
        if (settingsStatusLabel == null) return;
        settingsStatusLabel.setText(message);
        settingsStatusLabel.setStyle(
                success
                ? "-fx-font-family:'Inter'; -fx-font-size:12px; -fx-text-fill:#1a7a34;"
                : "-fx-font-family:'Inter'; -fx-font-size:12px; -fx-text-fill:#ba1a1a;");
    }
}
