package com.systemwatch;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OperatingSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;

import java.util.ArrayList;
import java.util.List;

// BACKEND: Using OSHI library to gather system metrics
// Source: https://github.com/oshi/oshi
// Documentation: https://www.oshi.ooo/oshi-core-java11/apidocs/com.github.oshi/oshi/software/os/OperatingSystem.html
public class GatherMetrics {

    // Important computer software and hardware, set in class constructor
    private final OperatingSystem os;
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final List<HWDiskStore> diskStores;
    private long[] prevTicks;

    // Default constructor, which asserts system metrics
    GatherMetrics(){
        SystemInfo si = new SystemInfo();
        os = si.getOperatingSystem();
        processor = si.getHardware().getProcessor();
        memory = si.getHardware().getMemory();
        diskStores = si.getHardware().getDiskStores();
        prevTicks = processor.getSystemCpuLoadTicks();

        // Assert basic system metrics
        assert(os != null) : "OS not detected";
        assert(os.getFamily() != null) : "OS Family not detected";
        assert(os.getVersionInfo() != null) : "OS Version info not detected";
        assert(os.getBitness() == 32 || os.getBitness() == 64) : "OS Bitness should be 32 or 64";

        assert(processor != null) : "Processor not detected";
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
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();
        return load;
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

    // Thread Count
    public int getThreadCount() {
        return processor.getLogicalProcessorCount();
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

    // Cached Memory (approximation)
    public long getCachedMemory() {
        return memory.getVirtualMemory().getSwapUsed();
    }

    // Page Faults
    public long getPageFaults() {
        return memory.getVirtualMemory().getSwapPagesIn();
    }

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
    public List<DiskMetrics> getDiskMetrics() {
        List<DiskMetrics> list = new ArrayList<>();

        for (OSFileStore fs : os.getFileSystem().getFileStores()) {

            long total = fs.getTotalSpace();
            long free = fs.getUsableSpace();
            long used = total - free;

            list.add(new DiskMetrics(
                    fs.getMount(),
                    total,
                    used,
                    free
            ));
        }

        return list;
    }

    // =================
    // PROCESS METRICS
    // =================

    public List<ProcessMetrics> getProcessMetrics() {
        List<ProcessMetrics> list = new ArrayList<>();

        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 0);

        for (OSProcess p : processes) {

            double cpu = 100d * p.getProcessCpuLoadCumulative() / processor.getLogicalProcessorCount();
            double ram = 100d * p.getResidentSetSize() / memory.getTotal();

            long diskBytes = p.getBytesRead() + p.getBytesWritten();
            double diskPercent = diskBytes / (double) (1024 * 1024 * 1024); // normalize

            list.add(new ProcessMetrics(
                    p.getProcessID(),
                    p.getName(),
                    cpu,
                    ram,
                    diskPercent
            ));
        }

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
