package com.systemwatch;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// BACKEND: Using OSHI library to gather system metrics
// Source: https://github.com/oshi/oshi
// Documentation: https://www.oshi.ooo/oshi-core-java11/apidocs/com.github.oshi/oshi/software/os/OperatingSystem.html
public class GatherMetrics {

    // Important computer software and hardware, set in class constructor
    private final OperatingSystem os;
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final List<HWDiskStore> diskStores;
    private long[] prevCpuTicks;

    // Process tracking
    private final Map<Integer, OSProcess> prevProcesses = new HashMap<>();
    private long lastProcessSampleTime;

    // Default constructor, which asserts system metrics
    GatherMetrics(){
        SystemInfo si = new SystemInfo();

        os = si.getOperatingSystem();
        processor = si.getHardware().getProcessor();
        memory = si.getHardware().getMemory();
        diskStores = si.getHardware().getDiskStores();
        prevCpuTicks = processor.getSystemCpuLoadTicks();

        // Assert basic system metrics
        assert(os != null) : "OS not detected";
        assert(os.getFamily() != null) : "OS Family not detected";
        assert(os.getVersionInfo() != null) : "OS Version info not detected";
        assert(os.getBitness() == 32 || os.getBitness() == 64) : "OS Bitness should be 32 or 64";

        assert(memory != null) : "Memory not detected";
        assert(!diskStores.isEmpty()) : "There should be at least one disk!";
    }

    // Gets system uptime
    public long getUptime() {
        // Returns value in seconds
        long uptime = os.getSystemUptime();

        // Uptime needs to be positive
        assert(uptime >= 0) : "Uptime should be non-negative!";

        return uptime;
    }

    // =================
    // CPU METRICS
    // =================

    // Gets processor identifier information
    public String getProcessorInfo() {
        // The CPU's identifier strings ,including name, vendor,
        // stepping, model, and family information (also called the signature of a CPU).
        CentralProcessor.ProcessorIdentifier identifier = processor.getProcessorIdentifier();

        return identifier.toString();
    }

    // CPU Usage
    public double getCpuUsage() {
        long[] ticks = processor.getSystemCpuLoadTicks();
        double load = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
        prevCpuTicks = ticks;
        return Math.max(0, load);
    }

    // Interrupts
    public long getInterrupts() {
        return processor.getInterrupts();
    }

    // User Time
    public long getUserTime() {
        return processor.getSystemCpuLoadTicks()[CentralProcessor.TickType.USER.getIndex()];
    }

    // Kernel Time
    public long getKernelTime() {
        return processor.getSystemCpuLoadTicks()[CentralProcessor.TickType.SYSTEM.getIndex()];
    }

    // Logical Cores
    public int getLogicalCoreCount() { return processor.getLogicalProcessorCount(); }

    // Thread Count
    public int getThreadCount() {
        return os.getThreadCount();
    }

    // =================
    // MEMORY METRICS
    // =================

    // Total Memory
    public long getTotalMemory() {
        return memory.getTotal();
    }

    // Used Memory
    public long getUsedMemory() {
        return memory.getTotal() - memory.getAvailable();
    }

    public long getSwapUsed() { return memory.getVirtualMemory().getSwapUsed(); }

    public long getSwapPagesIn() { return memory.getVirtualMemory().getSwapPagesIn(); }

    public long getSwapPagesOut() { return memory.getVirtualMemory().getSwapPagesOut(); }

    // =================
    // DISK METRICS
    // =================

    // Gets Disk Models
    public List<String> getDiskModels() {
        // Creates list of disk models
        List<String> models = new ArrayList<>();
        int num = 0;

        // Iterate through all disks and add model to list
        for (HWDiskStore disk : diskStores) {
            num++;
            models.add("Disk #" + num + ": " + disk.getModel());
        }

        return models;
    }

    // Create a list of info about each disk on the system
    // Create a list of info about each disk on the system
    public List<DiskMetrics> getDiskMetrics() {
        List<DiskMetrics> list = new ArrayList<>();

        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            long total = fs.getTotalSpace();
            long free = fs.getUsableSpace();
            long used = total - free;

            // Calculate disk usage percentage (but do not insert this into the database)
            double usedPercent = (double) used / total * 100;

            list.add(new DiskMetrics(fs.getMount(), total, used, free));
        }

        return list;
    }

    // =================
    // PROCESS METRICS
    // =================

    public List<ProcessMetrics> getProcessMetrics() {
        List<ProcessMetrics> list = new ArrayList<>();

        long now = System.currentTimeMillis();
        long elapsedMs = now - lastProcessSampleTime;
        lastProcessSampleTime = now;

        List<OSProcess> processes =
                os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 0);

        Map<Integer, OSProcess> currentMap = new HashMap<>();

        for (OSProcess p : processes) {
            int pid = p.getProcessID();
            currentMap.put(pid, p);

            OSProcess prev = prevProcesses.get(pid);

            double cpu = 0;
            double diskRate = 0;

            if (prev != null && elapsedMs > 0) {

                // CPU usage between ticks
                cpu = 100d * p.getProcessCpuLoadBetweenTicks(prev);

                // Disk rate (bytes/sec)
                long prevBytes = prev.getBytesRead() + prev.getBytesWritten();
                long currBytes = p.getBytesRead() + p.getBytesWritten();

                long deltaBytes = currBytes - prevBytes;
                diskRate = (deltaBytes * 1000d) / elapsedMs;
            }

            double ramPercent = 100d * p.getResidentSetSize() / memory.getTotal();

            list.add(new ProcessMetrics(
                    pid,
                    p.getName(),
                    cpu,
                    ramPercent,
                    diskRate
            ));
        }

        prevProcesses.clear();
        prevProcesses.putAll(currentMap);

        return list;
    }


    // =================
    // INNER DATA CLASSES
    // =================

    // Organizes disk metrics data in inner class
    public static class DiskMetrics {
        public String id;
        public long total;
        public long used;
        public long free;

        public DiskMetrics(String id, long total, long used, long free) {
            this.id = id;
            this.total = total;
            this.used = used;
            this.free = free;
        }

        // Disk usage percentage calculation (for use in the application, not stored in DB)
        public double getDiskUsagePercentage() {
            return (double) used / total * 100;
        }
    }

    // Organizes process metrics data in inner class
    public static class ProcessMetrics {
        public int pid;
        public String name;
        public double cpu;
        public double ram;
        public double disk;

        public ProcessMetrics(int pid, String name, double cpu, double ram, double disk) {
            this.pid = pid;
            this.name = name;
            this.cpu = cpu;
            this.ram = ram;
            this.disk = disk;
        }
    }

    // =================
    // TESTING
    // =================
    public void printResults() {
        System.out.println("\n--------------------------------------");
        System.out.println("BASIC METRICS TEST:");

        // Print system uptime
        System.out.println("System Uptime: " + this.getUptime() + " seconds");

        // Gets processor information
        System.out.println("Processor Information: " + this.getProcessorInfo());

        // Gets memory information
        System.out.println("Total Memory: " + this.getTotalMemory());
        System.out.println("Used Memory: " + this.getUsedMemory());

        System.out.println("Disk Models: " + this.getDiskModels());

        System.out.println("--------------------------------------\n");
    }
}
