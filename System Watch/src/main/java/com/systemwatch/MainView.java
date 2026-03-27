package com.systemwatch;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Comparator;
import java.util.Random;

import com.systemwatch.db.*;
import com.systemwatch.model.*;
import java.util.List;

public class MainView {

    private final BorderPane root = new BorderPane();

    private final TableView<ProcessRow> processTable = new TableView<>();
    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();

    private final Label selectedProcessLabel = new Label("No process selected");
    private final Button suspendResumeButton = new Button("Resume / Suspend");
    private final Button exportPdfButton = new Button("Export PDF");

    private final LineChart<String, Number> cpuChart = buildChart("CPU Usage Graph");
    private final LineChart<String, Number> ramChart = buildChart("RAM Usage Graph");
    private final LineChart<String, Number> diskChart = buildChart("Disk Usage Graph");

    private final GridPane heatmap = new GridPane();

    public MainView() {
        buildLayout();
        loadProcesses();
        wireEvents();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildLayout() {
        Label title = new Label("System Watch");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label instructionLeft = new Label("Select process for visualization by clicking on a PID row.");
        Label instructionRight = new Label("Use Resume / Suspend to change the selected process state.");
        HBox instructions = new HBox(40, instructionLeft, instructionRight);
        instructions.setPadding(new Insets(0, 0, 5, 0));

        VBox topBox = new VBox(8, title, instructions);
        topBox.setPadding(new Insets(12));
        root.setTop(topBox);

        configureProcessTable();

        VBox leftPanel = new VBox(10,
                new Label("Process List Panel"),
                processTable
        );
        leftPanel.setPadding(new Insets(10));
        leftPanel.setPrefWidth(430);
        VBox.setVgrow(processTable, Priority.ALWAYS);

        heatmap.setHgap(2);
        heatmap.setVgap(2);
        heatmap.setPadding(new Insets(10));
        TitledPane heatmapPane = new TitledPane("Heatmap (time vs % usage)", heatmap);
        heatmapPane.setCollapsible(false);

        HBox actionBar = new HBox(12, selectedProcessLabel, suspendResumeButton, exportPdfButton);

        VBox rightPanel = new VBox(12,
                new Label("Visual Panel"),
                actionBar,
                cpuChart,
                ramChart,
                diskChart,
                heatmapPane
        );
        rightPanel.setPadding(new Insets(10));

        VBox.setVgrow(cpuChart, Priority.ALWAYS);
        VBox.setVgrow(ramChart, Priority.ALWAYS);
        VBox.setVgrow(diskChart, Priority.ALWAYS);
        VBox.setVgrow(heatmapPane, Priority.ALWAYS);

        // Wrap the visualization panel in a ScrollPane
        ScrollPane rightScrollPane = new ScrollPane(rightPanel);
        rightScrollPane.setFitToWidth(true);   // makes content match panel width
        rightScrollPane.setFitToHeight(false); // allows vertical scrolling
        rightScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        SplitPane splitPane = new SplitPane(leftPanel, rightScrollPane);

        
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.34);

        root.setCenter(splitPane);
    }

    private void configureProcessTable() {
        TableColumn<ProcessRow, Number> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(data -> data.getValue().pidProperty());
        pidCol.setPrefWidth(70);

        TableColumn<ProcessRow, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().processNameProperty());
        nameCol.setPrefWidth(150);

        TableColumn<ProcessRow, Number> cpuCol = new TableColumn<>("CPU");
        cpuCol.setCellValueFactory(data -> data.getValue().cpuPercentProperty());
        cpuCol.setPrefWidth(80);
        cpuCol.setCellFactory(col -> percentCell());

        TableColumn<ProcessRow, Number> ramCol = new TableColumn<>("RAM");
        ramCol.setCellValueFactory(data -> data.getValue().ramPercentProperty());
        ramCol.setPrefWidth(80);
        ramCol.setCellFactory(col -> percentCell());

        TableColumn<ProcessRow, Number> diskCol = new TableColumn<>("Disk");
        diskCol.setCellValueFactory(data -> data.getValue().diskPercentProperty());
        diskCol.setPrefWidth(80);
        diskCol.setCellFactory(col -> percentCell());

        TableColumn<ProcessRow, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(data -> data.getValue().stateProperty());
        stateCol.setPrefWidth(100);

        processTable.getColumns().addAll(pidCol, nameCol, cpuCol, ramCol, diskCol, stateCol);
        processTable.setItems(processRows);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        processTable.getSortOrder().add(cpuCol);
        cpuCol.setSortType(TableColumn.SortType.DESCENDING);
    }

    private TableCell<ProcessRow, Number> percentCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f%%", item.doubleValue()));
            }
        };
    }

    private LineChart<String, Number> buildChart(String title) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis(0, 100, 10);

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setMinHeight(180);

        return chart;
    }

    private void loadProcesses() {
        try {
            ProcessDao dao = new ProcessDao();
            List<ProcessRecord> records = dao.getCurrentProcesses();

            System.out.println("Records returned: " + records.size());
            for (ProcessRecord r : records) {
                System.out.println("PID=" + r.pid + " name=" + r.name +
                        " CPU=" + r.cpuPercent +
                        " RAM=" + r.ramPercent +
                        " Disk=" + r.diskPercent);
            }

            processRows.clear();
            for (ProcessRecord r : records) {
                String state = r.markedForSuspension == 0 ? "Running" : "Suspended";
                processRows.add(new ProcessRow(r.pid, r.name, r.cpuPercent, r.ramPercent, r.diskPercent, state));
            }
            processRows.sort(Comparator.comparingDouble(ProcessRow::getCpuPercent).reversed());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load processes: " + e.getMessage());
        }
    }

    private void wireEvents() {
        processTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                selectedProcessLabel.setText("Selected: " + selected.getProcessName() + " (PID " + selected.getPid() + ")");
                loadVisualizations(selected);
            }
        });

        suspendResumeButton.setOnAction(event -> {
            ProcessRow selected = processTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No process selected", "Please select a process first.");
                return;
            }

            // TODO: Replace with backend partner logic later
            if ("Running".equals(selected.getState())) {
                selected.setState("Suspended");
            } else {
                selected.setState("Running");
            }

            processTable.refresh();
        });

        exportPdfButton.setOnAction(event -> {
            ProcessRow selected = processTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No process selected", "Please select a process first.");
                return;
            }

            // TODO: Replace with backend PDF export later
            showAlert("Export Placeholder", "PDF export will be connected later for PID " + selected.getPid() + ".");
        });
    }

    private void loadVisualizations(ProcessRow selected) {
        try {
            ProcessDao dao = new ProcessDao();
            List<ProcessRecord> history = dao.getHistoryForPid(selected.getPid(), 12);
            populateChart(cpuChart, history, "cpu");
            populateChart(ramChart, history, "ram");
            populateChart(diskChart, history, "disk");
            populateHeatmap(selected, history);
        } catch (Exception e) {
            showAlert("Error", "Failed to load visualizations: " + e.getMessage());
        }
    }

    private void populateChart(LineChart<String, Number> chart, List<ProcessRecord> history, String type) {
        chart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = history.size() - 1; i >= 0; i--) {
            ProcessRecord r = history.get(i);
            double value = 0;
            switch (type) {
                case "cpu": value = r.cpuPercent; break;
                case "ram": value = r.ramPercent; break;
                case "disk": value = r.diskPercent; break;
            }
            series.getData().add(new XYChart.Data<>("T" + (history.size() - i), value));
        }

        chart.getData().add(series);
    }

    private void populateHeatmap(ProcessRow selected, List<ProcessRecord> history) {
        heatmap.getChildren().clear();

        if (history.isEmpty()) return;

        addHeatmapRow("CPU", 0, history);
        addHeatmapRow("RAM", 1, history);
        addHeatmapRow("Disk", 2, history);
    }

    private void addHeatmapRow(String label, int rowIndex, List<ProcessRecord> history) {
        heatmap.add(new Label(label), 0, rowIndex);

        for (int i = history.size() - 1; i >= 0; i--) {
            ProcessRecord r = history.get(i);
            double value = 0;
            switch (label) {
                case "CPU": value = r.cpuPercent; break;
                case "RAM": value = r.ramPercent; break;
                case "Disk": value = r.diskPercent; break;
            }

            Rectangle rect = new Rectangle(24, 24);
            rect.setFill(colorForPercent(value));
            rect.setStroke(Color.GRAY);

            Tooltip.install(rect, new Tooltip(label + ": " + String.format("%.1f%%", value)));
            heatmap.add(rect, (history.size() - i), rowIndex);
        }
    }

    private Color colorForPercent(double value) {
        if (value < 20) return Color.LIGHTGREEN;
        if (value < 40) return Color.KHAKI;
        if (value < 60) return Color.GOLD;
        if (value < 80) return Color.ORANGE;
        return Color.TOMATO;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("System Watch");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}