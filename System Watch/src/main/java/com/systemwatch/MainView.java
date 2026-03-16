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
        loadMockProcesses();
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

    private void loadMockProcesses() {
        processRows.clear();
        processRows.addAll(
                new ProcessRow(1, "java.exe", 35.0, 28.0, 12.0, "Running"),
                new ProcessRow(0, "Chrome.exe", 18.0, 22.0, 5.0, "Running"),
                new ProcessRow(2, "FireFox.exe", 7.0, 14.0, 2.0, "Suspended"),
                new ProcessRow(3, "explorer.exe", 2.0, 4.0, 1.0, "Running")
        );

        processRows.sort(Comparator.comparingDouble(ProcessRow::getCpuPercent).reversed());
    }

    private void wireEvents() {
        processTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                selectedProcessLabel.setText("Selected: " + selected.getProcessName() + " (PID " + selected.getPid() + ")");
                loadMockVisualizations(selected);
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

    private void loadMockVisualizations(ProcessRow selected) {
        populateChart(cpuChart, selected.getCpuPercent());
        populateChart(ramChart, selected.getRamPercent());
        populateChart(diskChart, selected.getDiskPercent());
        populateHeatmap(selected);
    }

    private void populateChart(LineChart<String, Number> chart, double baseValue) {
        chart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Random random = new Random();

        for (int i = 1; i <= 12; i++) {
            double offset = (random.nextDouble() * 12.0) - 6.0;
            double value = Math.max(0, Math.min(100, baseValue + offset));
            series.getData().add(new XYChart.Data<>("T" + i, value));
        }

        chart.getData().add(series);
    }

    private void populateHeatmap(ProcessRow selected) {
        heatmap.getChildren().clear();

        Random random = new Random();

        addHeatmapRow("CPU", 0, selected.getCpuPercent(), random);
        addHeatmapRow("RAM", 1, selected.getRamPercent(), random);
        addHeatmapRow("Disk", 2, selected.getDiskPercent(), random);
    }

    private void addHeatmapRow(String label, int rowIndex, double baseValue, Random random) {
        heatmap.add(new Label(label), 0, rowIndex);

        for (int i = 0; i < 12; i++) {
            double value = Math.max(0, Math.min(100, baseValue + ((random.nextDouble() * 20) - 10)));

            Rectangle rect = new Rectangle(24, 24);
            rect.setFill(colorForPercent(value));
            rect.setStroke(Color.GRAY);

            Tooltip.install(rect, new Tooltip(label + ": " + String.format("%.1f%%", value)));
            heatmap.add(rect, i + 1, rowIndex);
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