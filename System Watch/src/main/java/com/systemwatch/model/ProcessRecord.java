package com.systemwatch.model;

public class ProcessRecord {
    public long timestamp;
    public int pid;
    public String name;
    public double cpuPercent;
    public double ramPercent;
    public double diskPercent;
    public int markedForSuspension;
}