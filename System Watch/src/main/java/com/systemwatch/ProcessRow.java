package com.systemwatch;

import javafx.beans.property.*;

public class ProcessRow {
    private final IntegerProperty pid = new SimpleIntegerProperty();
    private final StringProperty processName = new SimpleStringProperty();
    private final DoubleProperty cpuPercent = new SimpleDoubleProperty();
    private final DoubleProperty ramPercent = new SimpleDoubleProperty();
    private final DoubleProperty diskPercent = new SimpleDoubleProperty();
    private final StringProperty state = new SimpleStringProperty();

    public ProcessRow(int pid, String processName, double cpuPercent, double ramPercent, double diskPercent, String state) {
        this.pid.set(pid);
        this.processName.set(processName);
        this.cpuPercent.set(cpuPercent);
        this.ramPercent.set(ramPercent);
        this.diskPercent.set(diskPercent);
        this.state.set(state);
    }

    public int getPid() { return pid.get(); }
    public IntegerProperty pidProperty() { return pid; }

    public String getProcessName() { return processName.get(); }
    public StringProperty processNameProperty() { return processName; }

    public double getCpuPercent() { return cpuPercent.get(); }
    public DoubleProperty cpuPercentProperty() { return cpuPercent; }

    public double getRamPercent() { return ramPercent.get(); }
    public DoubleProperty ramPercentProperty() { return ramPercent; }

    public double getDiskPercent() { return diskPercent.get(); }
    public DoubleProperty diskPercentProperty() { return diskPercent; }

    public String getState() { return state.get(); }
    public StringProperty stateProperty() { return state; }

    public void setState(String value) { state.set(value); }
}