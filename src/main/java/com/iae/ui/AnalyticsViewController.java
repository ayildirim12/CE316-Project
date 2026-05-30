package com.iae.ui;

import com.iae.logic.DatabaseManager;
import com.iae.logic.ProjectManager;
import com.iae.logic.ReportManager;
import com.iae.model.Project;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.text.SimpleDateFormat;
import java.util.List;

public class AnalyticsViewController {

    // Summary labels
    @FXML private Label totalProjectsLabel;
    @FXML private Label totalSubmissionsLabel;
    @FXML private Label overallPassRateLabel;

    // Table
    @FXML private TableView<ProjectRow>         analyticsTable;
    @FXML private TableColumn<ProjectRow, String>  colProject;
    @FXML private TableColumn<ProjectRow, Number>  colTotal;
    @FXML private TableColumn<ProjectRow, Number>  colPass;
    @FXML private TableColumn<ProjectRow, Number>  colFail;
    @FXML private TableColumn<ProjectRow, Number>  colError;
    @FXML private TableColumn<ProjectRow, String>  colPassRate;
    @FXML private TableColumn<ProjectRow, String>  colLastRun;

    @FXML private Button refreshButton;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    // Lifecycle 
    @FXML
    public void initialize() {
        bindColumns();
        refresh();
    }

    // Public API (called by MainWindowController after a pipeline run)
    public void refresh() {
        List<Project> projects = ProjectManager.getInstance().getAllProjects();

        ObservableList<ProjectRow> rows = FXCollections.observableArrayList();

        int grandTotal = 0;
        int grandPass  = 0;

        for (Project project : projects) {
            // Deduplicate by studentId to avoid counting multiple runs
            long pass  = project.getResults().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        r -> r.getStudentId(),
                        r -> r,
                        (existing, replacement) -> replacement)) // keep latest
                    .values().stream()
                    .filter(r -> r.getStatus() == com.iae.model.Status.SUCCESS)
                    .count();

            long fail  = project.getResults().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        r -> r.getStudentId(),
                        r -> r,
                        (existing, replacement) -> replacement))
                    .values().stream()
                    .filter(r -> r.getStatus() == com.iae.model.Status.WRONG_OUTPUT)
                    .count();

            long error = project.getResults().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        r -> r.getStudentId(),
                        r -> r,
                        (existing, replacement) -> replacement))
                    .values().stream()
                    .filter(r -> r.getStatus() != com.iae.model.Status.SUCCESS
                            && r.getStatus() != com.iae.model.Status.WRONG_OUTPUT)
                    .count();

            int total = (int)(pass + fail + error);
            grandTotal += total;
            grandPass  += (int) pass;

            String passRate = (total > 0)
                    ? String.format("%.1f%%", (pass * 100.0) / total)
                    : "—";

            String lastRun = (project.getLastRunAt() != null)
                    ? DATE_FMT.format(project.getLastRunAt())
                    : "Never";

            rows.add(new ProjectRow(project.getName(), total, (int)pass, (int)fail, (int)error, passRate, lastRun));
        }

        analyticsTable.setItems(rows);

        totalProjectsLabel.setText(String.valueOf(projects.size()));
        totalSubmissionsLabel.setText(String.valueOf(grandTotal));
        overallPassRateLabel.setText(grandTotal > 0
                ? String.format("%.1f%%", (grandPass * 100.0) / grandTotal)
                : "—");
    }

    // Handlers
    @FXML
    private void handleRefresh() {
        refresh();
    }

    // Column binding
    private void bindColumns() {
        colProject.setCellValueFactory(c -> c.getValue().projectName);
        colTotal.setCellValueFactory(c -> c.getValue().total);
        colPass.setCellValueFactory(c -> c.getValue().pass);
        colFail.setCellValueFactory(c -> c.getValue().fail);
        colError.setCellValueFactory(c -> c.getValue().error);
        colPassRate.setCellValueFactory(c -> c.getValue().passRate);
        colLastRun.setCellValueFactory(c -> c.getValue().lastRun);

        colPass.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        });

        colFail.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            }
        });

        colError.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toString());
                setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;");
            }
        });

        colPassRate.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                try {
                    double val = Double.parseDouble(item.replace("%", ""));
                    if      (val >= 70) setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    else if (val >= 40) setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
                    else                setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                } catch (NumberFormatException e) {
                    setStyle("");
                }
            }
        });
    }

    // Inner data model
    public static class ProjectRow {
        final SimpleStringProperty  projectName;
        final SimpleIntegerProperty total;
        final SimpleIntegerProperty pass;
        final SimpleIntegerProperty fail;
        final SimpleIntegerProperty error;
        final SimpleStringProperty  passRate;
        final SimpleStringProperty  lastRun;

        ProjectRow(String name, int total, int pass, int fail, int error,
                   String passRate, String lastRun) {
            this.projectName = new SimpleStringProperty(name);
            this.total       = new SimpleIntegerProperty(total);
            this.pass        = new SimpleIntegerProperty(pass);
            this.fail        = new SimpleIntegerProperty(fail);
            this.error       = new SimpleIntegerProperty(error);
            this.passRate    = new SimpleStringProperty(passRate);
            this.lastRun     = new SimpleStringProperty(lastRun);
        }
    }
}
