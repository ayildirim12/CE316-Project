package com.iae.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Controls the Results View.
 *
 * Receives evaluation data as a plain String[][] so this controller compiles
 * and runs independently of the model / logic layer.
 *
 * Row format (matches MainWindowController.runDemoEvaluation):
 *   row[0]            = studentId
 *   row[1]            = compileStatus  ("OK" | "FAILED")
 *   row[2..2+tcCount-1] = per-test-case ("PASS" | "FAIL" | "—")
 *   row[2+tcCount]    = overallStatus  ("SUCCESS" | "WRONG_OUTPUT" | "COMPILE_ERROR" | …)
 *   row[3+tcCount]    = errorMessage
 *
 * TODO: When Furkan's EvaluationResult model is ready, add an overloaded
 *       initData(List<EvaluationResult>, List<TestCase>) that converts the
 *       model objects into this String[][] format and delegates here.
 */
public class ResultsViewController {

    /* ── FXML ── */
    @FXML private Label                  totalLabel;
    @FXML private Label                  passLabel;
    @FXML private Label                  failLabel;
    @FXML private Label                  errorLabel;
    @FXML private TableView<String[]>    resultsTable;
    @FXML private TextArea               detailOutputArea;
    @FXML private Label                  detailStudentLabel;

    /* ── State ── */
    private String[][] results;
    private int        tcCount;

    /* ── Public entry point ── */

    /**
     * Called by MainWindowController after evaluation finishes.
     *
     * @param results  String[][] – one row per student submission
     * @param tcCount  number of test cases (= number of dynamic TC columns)
     */
    public void initData(String[][] results, int tcCount) {
        this.results = results;
        this.tcCount = tcCount;
        buildColumns();
        resultsTable.setItems(FXCollections.observableArrayList(results));
        updateSummaryCards();

        resultsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showDetail(newVal));
    }

    /* ── Build dynamic columns ── */

    private void buildColumns() {
        resultsTable.getColumns().clear();

        /* Student ID */
        addCol("Student ID", 0, 130, 90);

        /* Compile Status */
        addStatusCol("Compile", 1, 90, 70);

        /* Per-test-case columns */
        for (int t = 0; t < tcCount; t++) {
            addStatusCol("TC" + (t + 1), 2 + t, 70, 60);
        }

        /* Overall status */
        addStatusCol("Status", 2 + tcCount, 120, 90);

        /* Error / Notes */
        TableColumn<String[], String> errCol = new TableColumn<>("Error / Notes");
        final int errIdx = 3 + tcCount;
        errCol.setCellValueFactory(c -> new SimpleStringProperty(
                safeGet(c.getValue(), errIdx)));
        errCol.setPrefWidth(240);
        resultsTable.getColumns().add(errCol);
    }

    /* Plain text column */
    private void addCol(String title, int idx, double pref, double min) {
        TableColumn<String[], String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new SimpleStringProperty(safeGet(c.getValue(), idx)));
        col.setPrefWidth(pref);
        col.setMinWidth(min);
        resultsTable.getColumns().add(col);
    }

    /* Coloured status column */
    private void addStatusCol(String title, int idx, double pref, double min) {
        TableColumn<String[], String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new SimpleStringProperty(safeGet(c.getValue(), idx)));
        col.setCellFactory(tc -> new StatusCell());
        col.setPrefWidth(pref);
        col.setMinWidth(min);
        resultsTable.getColumns().add(col);
    }

    /* ── Summary cards ── */

    private void updateSummaryCards() {
        if (results == null) return;
        int statusIdx = 2 + tcCount;
        int pass = 0, fail = 0, error = 0;
        for (String[] row : results) {
            String s = safeGet(row, statusIdx);
            if ("SUCCESS".equals(s))      pass++;
            else if ("WRONG_OUTPUT".equals(s)) fail++;
            else                           error++;
        }
        totalLabel.setText(String.valueOf(results.length));
        passLabel.setText(String.valueOf(pass));
        failLabel.setText(String.valueOf(fail));
        errorLabel.setText(String.valueOf(error));
    }

    /* ── Detail panel ── */

    private void showDetail(String[] row) {
        if (row == null) {
            detailOutputArea.clear();
            detailStudentLabel.setText("");
            return;
        }
        detailStudentLabel.setText(row[0]);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Student: ").append(row[0]).append(" ===\n");
        sb.append("Compile Status : ").append(safeGet(row, 1)).append("\n\n");

        for (int t = 0; t < tcCount; t++) {
            sb.append("TC").append(t + 1).append(" : ").append(safeGet(row, 2 + t)).append("\n");
        }

        sb.append("\nOverall Status : ").append(safeGet(row, 2 + tcCount)).append("\n");
        String err = safeGet(row, 3 + tcCount);
        if (!err.isBlank()) sb.append("Error          : ").append(err).append("\n");

        detailOutputArea.setText(sb.toString());
    }

    /* ── Toolbar actions ── */

    @FXML
    private void handleReRun() {
        Object ctrl = resultsTable.getScene().getRoot()
                .getProperties().get("mainWindowController");
        if (ctrl instanceof MainWindowController mwc) mwc.handleRun(null);
    }

    @FXML
    private void handleExport() {
        if (results == null || results.length == 0) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Export Results as CSV");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        var file = fc.showSaveDialog(resultsTable.getScene().getWindow());
        if (file == null) return;
        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(Paths.get(file.toURI())))) {
            // header
            StringBuilder hdr = new StringBuilder("Student ID,Compile");
            for (int t = 0; t < tcCount; t++) hdr.append(",TC").append(t + 1);
            hdr.append(",Status,Error");
            pw.println(hdr);
            // rows
            for (String[] row : results) {
                StringBuilder line = new StringBuilder();
                for (int c = 0; c < row.length; c++) {
                    if (c > 0) line.append(",");
                    String val = row[c] != null ? row[c] : "";
                    if (val.contains(",") || val.contains("\""))
                        val = "\"" + val.replace("\"", "\"\"") + "\"";
                    line.append(val);
                }
                pw.println(line);
            }
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Export failed: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void handleClearDetail() {
        resultsTable.getSelectionModel().clearSelection();
        detailOutputArea.clear();
        detailStudentLabel.setText("");
    }

    /* ── Utilities ── */

    private static String safeGet(String[] row, int idx) {
        return (row != null && idx < row.length && row[idx] != null) ? row[idx] : "";
    }

    /* Coloured cell based on value */
    private static class StatusCell extends TableCell<String[], String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll("cell-pass", "cell-fail", "cell-error");
            if (empty || item == null) { setText(null); return; }
            setText(item);
            switch (item) {
                case "PASS", "OK", "SUCCESS"                    -> getStyleClass().add("cell-pass");
                case "FAIL", "FAILED", "WRONG_OUTPUT"           -> getStyleClass().add("cell-fail");
                case "COMPILE_ERROR", "RUNTIME_ERROR",
                     "TIMEOUT", "ERROR"                         -> getStyleClass().add("cell-error");
                default -> { /* neutral */ }
            }
        }
    }
}
