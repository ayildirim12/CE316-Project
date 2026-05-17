package com.iae.ui;

import com.iae.model.ComparisonResult;
import com.iae.model.EvaluationResult;
import com.iae.model.ExecutionResult;
import com.iae.model.Status;
import com.iae.model.TestCase;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls the Results View.
 *
 * Row format for the internal String[][]:
 *   row[0]              = studentId
 *   row[1]              = compileStatus  ("OK" | "FAILED")
 *   row[2..2+tcCount-1] = per-test-case  ("PASS" | "FAIL" | "TIMEOUT" | "—")
 *   row[2+tcCount]      = overallStatus  ("SUCCESS" | "WRONG_OUTPUT" | "COMPILE_ERROR" | …)
 *   row[3+tcCount]      = errorMessage
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
    private String[][]              results;
    private int                     tcCount;
    private List<EvaluationResult>  evalResults = new ArrayList<>();

    /* ── Public entry point ── */

    /**
     * Called by MainWindowController after evaluation finishes.
     *
     * @param results  String[][] – one row per student submission
     * @param tcCount  number of test cases (= number of dynamic TC columns)
     */
    public void initData(String[][] results, int tcCount, List<EvaluationResult> evalResults) {
        this.results     = results;
        this.tcCount     = tcCount;
        this.evalResults = evalResults != null ? evalResults : new ArrayList<>();
        buildColumns();
        resultsTable.setItems(FXCollections.observableArrayList(results));
        updateSummaryCards();

        resultsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> showDetail(newVal));
    }

    /**
     * Convenience overload: converts EvaluationResult objects to the internal
     * String[][] format and delegates to {@link #initData(String[][], int, List)}.
     */
    public void initData(List<EvaluationResult> evalResults, List<TestCase> testCases) {
        int tc = testCases.size();
        String[][] rows = new String[evalResults.size()][];
        for (int i = 0; i < evalResults.size(); i++) {
            EvaluationResult r = evalResults.get(i);
            Status st = r.getStatus();
            String[] row = new String[4 + tc];
            row[0] = r.getStudentId();

            boolean compileOk = st != Status.COMPILE_ERROR && st != Status.SOURCE_MISSING;
            row[1] = compileOk ? "OK" : "FAILED";

            List<ComparisonResult> comps = r.getComparisonResults();
            List<ExecutionResult>  execs = r.getExecutionResults();
            for (int t = 0; t < tc; t++) {
                if (!compileOk) {
                    row[2 + t] = "—";
                } else if (t < comps.size()) {
                    row[2 + t] = comps.get(t).isMatch() ? "PASS" : "FAIL";
                } else if (t < execs.size() && execs.get(t).isTimedOut()) {
                    row[2 + t] = "TIMEOUT";
                } else {
                    row[2 + t] = "—";
                }
            }

            row[2 + tc] = st != null ? st.name() : "UNKNOWN";
            row[3 + tc] = r.getErrorMessage() != null ? r.getErrorMessage() : "";
            rows[i] = row;
        }
        initData(rows, tc, evalResults);
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

        String studentId = row[0];
        detailStudentLabel.setText(studentId);

        EvaluationResult er = evalResults.stream()
                .filter(r -> studentId.equals(r.getStudentId()))
                .findFirst().orElse(null);

        StringBuilder sb = new StringBuilder();
        sb.append("Student : ").append(studentId).append("\n");
        sb.append("─".repeat(44)).append("\n\n");

        // Compile
        String compileStatus = safeGet(row, 1);
        sb.append("Compile : ").append(compileStatus).append("\n");
        if (er != null && er.getCompileResult() != null) {
            String compileErr = er.getCompileResult().getStderr();
            if (compileErr != null && !compileErr.isBlank()) {
                sb.append("  ").append(compileErr.trim().replace("\n", "\n  ")).append("\n");
            }
        }
        sb.append("\n");

        // Per test-case output
        List<ExecutionResult>  execs = er != null ? er.getExecutionResults()  : List.of();
        List<ComparisonResult> comps = er != null ? er.getComparisonResults() : List.of();

        for (int t = 0; t < tcCount; t++) {
            String tcStatus = safeGet(row, 2 + t);
            sb.append("TC").append(t + 1).append(" : ").append(tcStatus).append("\n");

            if (t < execs.size()) {
                String stdout = execs.get(t).getStdout();
                String stderr = execs.get(t).getStderr();
                if (stdout != null && !stdout.isBlank()) {
                    sb.append("  Output : ")
                      .append(stdout.trim().replace("\n", "\n           "))
                      .append("\n");
                }
                if (stderr != null && !stderr.isBlank()) {
                    sb.append("  Stderr : ")
                      .append(stderr.trim().replace("\n", "\n           "))
                      .append("\n");
                }
            }

            if (t < comps.size() && !comps.get(t).isMatch()) {
                String diff = comps.get(t).getDiff();
                if (diff != null && !diff.isBlank()) {
                    sb.append("  Diff   :\n");
                    for (String line : diff.split("\n")) {
                        sb.append("    ").append(line).append("\n");
                    }
                }
            }
            sb.append("\n");
        }

        // Overall
        sb.append("Overall : ").append(safeGet(row, 2 + tcCount)).append("\n");
        String errMsg = safeGet(row, 3 + tcCount);
        if (!errMsg.isBlank()) {
            sb.append("Error   : ").append(errMsg).append("\n");
        }

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
