package com.systemwatch.model;

public class CpuRecord {
    public long timestamp;
    public double usage;
    public long interrupts;
    public double userTime;
    public double kernelTime;
    public int threadCount;
}