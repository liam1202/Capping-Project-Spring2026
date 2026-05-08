package com.systemwatch;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors;

import com.systemwatch.db.*;
import com.systemwatch.model.*;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;


import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;


public class MainView {

    private final BorderPane root = new BorderPane();

    private final Timeline refreshTimeline = new Timeline();

    private final TableView<ProcessRow> processTable = new TableView<>();
    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();

    private final FilteredList<ProcessRow> filteredProcessRows = new FilteredList<>(processRows, p -> true);
    private final SortedList<ProcessRow> sortedProcessRows = new SortedList<>(filteredProcessRows);
    private final TextField searchField = new TextField();

    private final Label selectedProcessLabel = new Label("Overall System Metrics");
    private final Button suspendResumeButton = new Button("Resume / Suspend");
    private final Button exportPdfButton = new Button("Export PNG");
    private final Button clearSelectionButton = new Button("Clear Selection");

    private VBox rightPanel;
    private final ScrollPane rightScrollPane = new ScrollPane();

    private final LineChart<String, Number> cpuChart = buildChart("CPU Usage Graph");
    private final LineChart<String, Number> ramChart = buildChart("RAM Usage Graph");
    private final LineChart<String, Number> diskChart = buildChart("Disk Usage Graph");

    private final GridPane heatmap = new GridPane();

    public MainView() {
        buildLayout();
        loadProcesses();
        wireEvents();
        loadOverallVisualizations();   // default state when nothing is selected
        updateActionState(null);
        startAutoRefresh();
    }

    public Parent getRoot() {
        return root;
    }

    private void startAutoRefresh() {
        refreshTimeline.getKeyFrames().setAll(
            new KeyFrame(Duration.seconds(5), event -> refreshUiFromDatabase())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void refreshUiFromDatabase() {
        try {
            ProcessRow selected = processTable.getSelectionModel().getSelectedItem();
            Integer selectedPid = (selected == null) ? null : selected.getPid();

            loadProcesses();

            if (selectedPid != null) {
                ProcessRow reselected = null;
                for (ProcessRow row : processRows) {
                    if (row.getPid() == selectedPid) {
                        reselected = row;
                        break;
                    }
                }

                if (reselected != null) {
                    processTable.getSelectionModel().select(reselected);
                    selectedProcessLabel.setText("Selected: " + reselected.getProcessName() + " (PID " + reselected.getPid() + ")");
                    loadProcessVisualizations(reselected);
                    updateActionState(reselected);
                } else {
                    processTable.getSelectionModel().clearSelection();
                    selectedProcessLabel.setText("Overall System Metrics");
                    loadOverallVisualizations();
                    updateActionState(null);
                }
            } else {
                loadOverallVisualizations();
                updateActionState(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildLayout() {
        Label title = new Label("System Watch");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label instructionLeft = new Label("Select a process to view process-specific metrics.");
        Label instructionRight = new Label("Clear selection to return to overall system metrics.");
        HBox instructions = new HBox(40, instructionLeft, instructionRight);
        instructions.setPadding(new Insets(0, 0, 5, 0));

        VBox topBox = new VBox(8, title, instructions);
        topBox.setPadding(new Insets(12));
        root.setTop(topBox);

        configureProcessTable();

        searchField.setPromptText("Search by PID or process name...");
        searchField.setMaxWidth(Double.MAX_VALUE);

        VBox leftPanel = new VBox(10,
                new Label("Process List Panel"),
                searchField,
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

        HBox actionBar = new HBox(12,
                selectedProcessLabel,
                suspendResumeButton,
                exportPdfButton,
                clearSelectionButton
        );

        rightPanel = new VBox(12,
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

        rightScrollPane.setContent(rightPanel);
        rightScrollPane.setFitToWidth(true);
        rightScrollPane.setFitToHeight(false);
        rightScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        SplitPane splitPane = new SplitPane(leftPanel, rightScrollPane);
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.34);

        root.setCenter(splitPane);
    }

    private void preserveScrollPosition(Runnable action) {
        double currentV = rightScrollPane.getVvalue();
        action.run();
        Platform.runLater(() -> rightScrollPane.setVvalue(currentV));
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
        sortedProcessRows.comparatorProperty().bind(processTable.comparatorProperty());
        processTable.setItems(sortedProcessRows);
        processTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        processTable.getSortOrder().add(cpuCol);
        cpuCol.setSortType(TableColumn.SortType.DESCENDING);

        // Clicking selected row again clears the selection
        processTable.setRowFactory(tv -> {
            TableRow<ProcessRow> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.SECONDARY) {
                    ProcessRow clickedItem = row.getItem();
                    ProcessRow selectedItem = processTable.getSelectionModel().getSelectedItem();

                    if (clickedItem == selectedItem) {
                        processTable.getSelectionModel().clearSelection();
                    } else {
                        processTable.getSelectionModel().select(clickedItem);
                    }

                    event.consume();
                }
            });

            return row;
        });
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

            processRows.clear();
            for (ProcessRecord r : records) {
                String state = r.markedForSuspension == 0 ? "Running" : "Suspended";
                processRows.add(new ProcessRow(r.pid, r.name, r.cpuPercent, r.ramPercent, r.diskPercent, state));
            }
            applySearchFilter(searchField.getText());

            // Preserve whatever sort the user currently has applied
            // if (!processTable.getSortOrder().isEmpty()) {
            //     processTable.sort();
            // }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load processes: " + e.getMessage());
        }
    }

    private void wireEvents() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
        applySearchFilter(newValue);
    });
        processTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            preserveScrollPosition(() -> {
                updateActionState(selected);

                if (selected != null) {
                    selectedProcessLabel.setText("Selected: " + selected.getProcessName() + " (PID " + selected.getPid() + ")");
                    loadProcessVisualizations(selected);
                } else {
                    selectedProcessLabel.setText("Overall System Metrics");
                    loadOverallVisualizations();
                }
            });
        });

        clearSelectionButton.setOnAction(event -> {
            preserveScrollPosition(() -> processTable.getSelectionModel().clearSelection());
        });

        suspendResumeButton.setOnAction(event -> {
            ProcessRow selected = processTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("No process selected", "Please select a process first.");
                return;
            }

            if ("Running".equals(selected.getState())) {
                selected.setState("Suspended");
            } else {
                selected.setState("Running");
            }

            processTable.refresh();
        });

        exportPdfButton.setOnAction(event -> exportSelectedProcessImage());
    }

    private void applySearchFilter(String searchText) {
        String filter = searchText == null ? "" : searchText.trim().toLowerCase();

        filteredProcessRows.setPredicate(row -> {
            if (filter.isEmpty()) {
                return true;
            }

            String pidText = String.valueOf(row.getPid());
            String nameText = row.getProcessName() == null
                    ? ""
                    : row.getProcessName().toLowerCase();

            return pidText.contains(filter) || nameText.contains(filter);
        });

        // if (!processTable.getSortOrder().isEmpty()) {
        //     processTable.sort();
        // }
    }

    private void exportSelectedProcessImage() {
        ProcessRow selected = processTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("No process selected", "Please select a process first.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Visualization");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG Files", "*.png")
            );

            fileChooser.setInitialFileName(
                    "SystemWatch_PID_" + selected.getPid() + ".png"
            );

            File outputFile = fileChooser.showSaveDialog(root.getScene().getWindow());

            if (outputFile == null) {
                return;
            }

            WritableImage snapshot =
                    rightPanel.snapshot(new SnapshotParameters(), null);

            BufferedImage bufferedImage =
                    SwingFXUtils.fromFXImage(snapshot, null);

            ImageIO.write(bufferedImage, "png", outputFile);

            showAlert(
                    "Export Complete",
                    "Visualization exported to:\n" + outputFile.getAbsolutePath()
            );

        } catch (Exception e) {
            e.printStackTrace();

            showAlert(
                    "Export Failed",
                    "Could not export visualization:\n" + e.getMessage()
            );
        }
    }

    private void updateActionState(ProcessRow selected) {
        boolean hasSelection = selected != null;
        suspendResumeButton.setDisable(!hasSelection);
        exportPdfButton.setDisable(!hasSelection);
        clearSelectionButton.setDisable(!hasSelection);
    }

    private void loadProcessVisualizations(ProcessRow selected) {
        try {
            ProcessDao dao = new ProcessDao();
            List<ProcessRecord> history = dao.getHistoryForPid(selected.getPid(), 12);

            populateProcessChart(cpuChart, history, "cpu");
            populateProcessChart(ramChart, history, "ram");
            populateProcessChart(diskChart, history, "disk");
            populateProcessHeatmap(history);

        } catch (Exception e) {
            showAlert("Error", "Failed to load process visualizations: " + e.getMessage());
        }
    }

    private void loadOverallVisualizations() {
        try {
            CpuDao cpuDao = new CpuDao();
            RamDao ramDao = new RamDao();
            DiskDao diskDao = new DiskDao();

            List<CpuRecord> cpuHistory = reverseCpu(cpuDao.getLatest(12));
            List<RamRecord> ramHistory = reverseRam(ramDao.getLatest(12));
            List<DiskRecord> diskHistory = reverseDisk(diskDao.getLatest(24)); // two disks × 12 timestamps

            populateCpuChart(cpuHistory);
            populateRamChart(ramHistory);
            populateDiskChart(diskHistory);
            populateOverallHeatmap(cpuHistory, ramHistory, diskHistory);

        } catch (Exception e) {
            showAlert("Error", "Failed to load overall visualizations: " + e.getMessage());
        }
    }

    private List<CpuRecord> reverseCpu(List<CpuRecord> list) {
        List<CpuRecord> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    private List<RamRecord> reverseRam(List<RamRecord> list) {
        List<RamRecord> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    private List<DiskRecord> reverseDisk(List<DiskRecord> list) {
        List<DiskRecord> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return copy;
    }

    private void populateProcessChart(LineChart<String, Number> chart, List<ProcessRecord> history, String type) {
        chart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<ProcessRecord> ordered = new ArrayList<>(history);
        Collections.reverse(ordered);

        for (int i = 0; i < ordered.size(); i++) {
            ProcessRecord r = ordered.get(i);
            double value = switch (type) {
                case "cpu" -> r.cpuPercent;
                case "ram" -> r.ramPercent;
                case "disk" -> r.diskPercent;
                default -> 0;
            };
            series.getData().add(new XYChart.Data<>("T" + (i + 1), value));
        }

        chart.getData().add(series);
    }

    private void populateCpuChart(List<CpuRecord> history) {
        cpuChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (int i = 0; i < history.size(); i++) {
            CpuRecord r = history.get(i);
            series.getData().add(new XYChart.Data<>("T" + (i + 1), r.usage));
        }

        cpuChart.getData().add(series);
    }

    private void populateRamChart(List<RamRecord> history) {
        ramChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (int i = 0; i < history.size(); i++) {
            RamRecord r = history.get(i);
            double percent = r.total == 0 ? 0 : (r.used * 100.0 / r.total);
            series.getData().add(new XYChart.Data<>("T" + (i + 1), percent));
        }

        ramChart.getData().add(series);
    }

    private void populateDiskChart(List<DiskRecord> history) {
        diskChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        List<Double> diskPercents = aggregateDiskPercentByTimestamp(history);

        for (int i = 0; i < diskPercents.size(); i++) {
            series.getData().add(new XYChart.Data<>("T" + (i + 1), diskPercents.get(i)));
        }

        diskChart.getData().add(series);
    }

    private List<Double> aggregateDiskPercentByTimestamp(List<DiskRecord> history) {
        Map<Long, List<DiskRecord>> grouped = history.stream()
                .collect(Collectors.groupingBy(r -> r.timestamp, LinkedHashMap::new, Collectors.toList()));

        List<Double> result = new ArrayList<>();
        for (List<DiskRecord> records : grouped.values()) {
            long total = 0;
            long used = 0;
            for (DiskRecord r : records) {
                total += r.total;
                used += r.used;
            }
            result.add(total == 0 ? 0 : used * 100.0 / total);
        }
        return result;
    }

    private void populateProcessHeatmap(List<ProcessRecord> history) {
        heatmap.getChildren().clear();
        if (history.isEmpty()) return;

        List<ProcessRecord> ordered = new ArrayList<>(history);
        Collections.reverse(ordered);

        addProcessHeatmapRow("CPU", 0, ordered);
        addProcessHeatmapRow("RAM", 1, ordered);
        addProcessHeatmapRow("Disk", 2, ordered);
    }

    private void populateOverallHeatmap(List<CpuRecord> cpuHistory, List<RamRecord> ramHistory, List<DiskRecord> diskHistory) {
        heatmap.getChildren().clear();

        addCpuHeatmapRow("CPU", 0, cpuHistory);
        addRamHeatmapRow("RAM", 1, ramHistory);
        addDiskHeatmapRow("Disk", 2, diskHistory);
    }

    private void addProcessHeatmapRow(String label, int rowIndex, List<ProcessRecord> history) {
        heatmap.add(new Label(label), 0, rowIndex);

        for (int i = 0; i < history.size(); i++) {
            ProcessRecord r = history.get(i);
            double value = switch (label) {
                case "CPU" -> r.cpuPercent;
                case "RAM" -> r.ramPercent;
                case "Disk" -> r.diskPercent;
                default -> 0;
            };

            Rectangle rect = new Rectangle(24, 24);
            rect.setFill(colorForPercent(value));
            rect.setStroke(Color.GRAY);

            Tooltip.install(rect, new Tooltip(label + ": " + String.format("%.1f%%", value)));
            heatmap.add(rect, i + 1, rowIndex);
        }
    }

    private void addCpuHeatmapRow(String label, int rowIndex, List<CpuRecord> history) {
        heatmap.add(new Label(label), 0, rowIndex);

        for (int i = 0; i < history.size(); i++) {
            double value = history.get(i).usage;

            Rectangle rect = new Rectangle(24, 24);
            rect.setFill(colorForPercent(value));
            rect.setStroke(Color.GRAY);

            Tooltip.install(rect, new Tooltip(label + ": " + String.format("%.1f%%", value)));
            heatmap.add(rect, i + 1, rowIndex);
        }
    }

    private void addRamHeatmapRow(String label, int rowIndex, List<RamRecord> history) {
        heatmap.add(new Label(label), 0, rowIndex);

        for (int i = 0; i < history.size(); i++) {
            RamRecord r = history.get(i);
            double value = r.total == 0 ? 0 : (r.used * 100.0 / r.total);

            Rectangle rect = new Rectangle(24, 24);
            rect.setFill(colorForPercent(value));
            rect.setStroke(Color.GRAY);

            Tooltip.install(rect, new Tooltip(label + ": " + String.format("%.1f%%", value)));
            heatmap.add(rect, i + 1, rowIndex);
        }
    }

    private void addDiskHeatmapRow(String label, int rowIndex, List<DiskRecord> history) {
        heatmap.add(new Label(label), 0, rowIndex);

        List<Double> diskPercents = aggregateDiskPercentByTimestamp(history);

        for (int i = 0; i < diskPercents.size(); i++) {
            double value = diskPercents.get(i);

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