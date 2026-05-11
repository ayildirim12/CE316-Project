package com.iae.ui;

import com.iae.logic.ProgressListener;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ProgressDialog implements ProgressListener {

    /* ── FXML injected fields ── */
    @FXML private Label       statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label       progressCountLabel;
    @FXML private TextArea    logArea;
    @FXML private Button      cancelBtn;

    private Stage stage;
    private volatile boolean cancelled = false;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /* ── Factory: creates the dialog stage and returns the controller ── */
    public static ProgressDialog create(Stage owner) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                ProgressDialog.class.getResource("/com/iae/fxml/ProgressDialog.fxml"));
        Parent root = loader.load();
        ProgressDialog controller = loader.getController();

        Stage dlgStage = new Stage(StageStyle.DECORATED);
        dlgStage.initModality(Modality.WINDOW_MODAL);
        dlgStage.initOwner(owner);
        dlgStage.setTitle("Processing Submissions");
        dlgStage.setResizable(false);
        dlgStage.setScene(new Scene(root));
        dlgStage.setOnCloseRequest(e -> e.consume()); // force Cancel button
        controller.stage = dlgStage;
        return controller;
    }

    public void show() {
        if (stage != null) stage.show();
    }

    public void close() {
        if (stage != null) Platform.runLater(stage::close);
    }

    public boolean isCancelled() { return cancelled; }

    /* ── ProgressListener implementation ── */

    @Override
    public void onProgress(int current, int total, String studentId) {
        Platform.runLater(() -> {
            double pct = total > 0 ? (double) current / total : 0;
            progressBar.setProgress(pct);
            progressCountLabel.setText(current + " / " + total);
            statusLabel.setText(
                    "Processing student " + current + " of " + total + ": " + studentId);
            appendLog("Processing " + studentId + "…");
        });
    }

    @Override
    public void onComplete() {
        Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            progressCountLabel.setText("Done");
            statusLabel.setText("Evaluation complete.");
            appendLog("All submissions processed.");
            if (cancelBtn != null) cancelBtn.setText("Close");
        });
    }

    /* ── Log helper ── */

    public void appendLog(String message) {
        Platform.runLater(() -> {
            String ts = LocalTime.now().format(TIME_FMT);
            logArea.appendText("[" + ts + "] " + message + "\n");
        });
    }

    /* ── FXML handler ── */

    @FXML
    private void handleCancel() {
        cancelled = true;
        appendLog("Cancellation requested.");
        if (stage != null) stage.close();
    }
}
