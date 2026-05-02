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
import java.util.concurrent.ConcurrentHashMap;

// BACKEND: Using OSHI library to gather system metrics
// Source: https://github.com/oshi/oshi
// Documentation: https://www.oshi.ooo/oshi-core-java11/apidocs/com.github.oshi/oshi/software/os/OperatingSystem.html
public class GatherMetrics {

    // Important computer software and hardware, set in class constructor
    private final OperatingSystem os;
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final List<HWDiskStore> diskStores;

    // Previous CPU tick snapshot used to calculate CPU usage
    private long[] prevCpuTicks;

    // Stores previous disk I/O byte counts per process for disk percentage calculations
    private final Map<Integer, Long> prevIoBytes = new ConcurrentHashMap<>();

    // Stores previous process snapshots used for CPU usage calculations
    private Map<Integer, OSProcess> prevProcMap = new ConcurrentHashMap<>();

    private long lastCpuSampleTime = 0;
    private static final long MIN_CPU_SAMPLE_INTERVAL_MS = 500;

    // Default constructor, which asserts system metrics
    GatherMetrics(){
        SystemInfo si = new SystemInfo();

        os = si.getOperatingSystem();
        processor = si.getHardware().getProcessor();
        memory = si.getHardware().getMemory();
        diskStores = si.getHardware().getDiskStores();

        // Assert basic system metrics
        assert(os != null) : "OS not detected";
        assert(os.getFamily() != null) : "OS Family not detected";
        assert(os.getVersionInfo() != null) : "OS Version info not detected";
        assert(os.getBitness() == 32 || os.getBitness() == 64) : "OS Bitness should be 32 or 64";

        assert(memory != null) : "Memory not detected";
        assert(!diskStores.isEmpty()) : "There should be at least one disk!";

        prevCpuTicks = processor.getSystemCpuLoadTicks();
    }

    // Helper function that makes sure values remain within a valid percentage range
    private double clamp(double val) {
        if (Double.isNaN(val) || Double.isInfinite(val))
            return 0;
        return Math.max(0, Math.min(100, val));
    }

    // Gets system uptime (in seconds) since boot
    public long getUptime() {
        return Math.max(0, os.getSystemUptime());
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

    // Calculates total CPU usage between tick snapshots (as a percentage)
    public synchronized double getCpuUsage() {
        long now = System.currentTimeMillis();

        if (now - lastCpuSampleTime < MIN_CPU_SAMPLE_INTERVAL_MS) {
            return 0;
        }

        long[] newTicks = processor.getSystemCpuLoadTicks();
        double load = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100.0;

        prevCpuTicks = newTicks;
        lastCpuSampleTime = now;

        return clamp(load);
    }

    // Hardware Interrupts
    public long getInterrupts() {
        return processor.getInterrupts();
    }

    // CPU time spent in USER mode using tick counters
    public long getUserTime() {
        return processor.getSystemCpuLoadTicks()[CentralProcessor.TickType.USER.getIndex()];
    }

    // CPU time spent in SYSTEM (kernel) mode using tick counters
    public long getKernelTime() {
        return processor.getSystemCpuLoadTicks()[CentralProcessor.TickType.SYSTEM.getIndex()];
    }

    // Logical Cores
    public int getLogicalCoreCount() {
        return processor.getLogicalProcessorCount();
    }

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

    // Available Memory
    public long getAvailableMemory() {
        return memory.getAvailable();
    }

    // Total Swap Memory Used
    public long getSwapUsed() {
        return memory.getVirtualMemory().getSwapUsed();
    }

    // Number of pages swapped into memory from disk
    public long getSwapPagesIn() {
        return memory.getVirtualMemory().getSwapPagesIn();
    }

    // Number of pages swapped out of memory to disk
    public long getSwapPagesOut() {
        return memory.getVirtualMemory().getSwapPagesOut();
    }

    // Calculates memory usage as a percentage of total system memory
    public double getMemoryUsagePercent() {
        return clamp((getUsedMemory() * 100.0) / memory.getTotal());
    }

    // =================
    // DISK METRICS
    // =================

    // Returns a list of disk model names detected on the system
    public List<String> getDiskModels() {
        // Creates list of disk models
        List<String> models = new ArrayList<>();
        int num = 0;

        // Iterate through all disks and add model to list
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            num++;
            models.add("Disk #" + num + ": " + disk.getModel());
        }

        return models;
    }

    // Gets usage statistics for each mounted file system on the system
    public List<DiskMetrics> getDiskMetrics() {
        List<DiskMetrics> list = new ArrayList<>();

        // Iterates through file stores and calculates total, used, and free space
        for (OSFileStore fs : os.getFileSystem().getFileStores()) {
            long total = fs.getTotalSpace();
            long free = fs.getUsableSpace();
            long used = Math.max(0, total - free);

            list.add(new DiskMetrics(fs.getMount(), total, used, free));
        }

        return list;
    }

    // =================
    // PROCESS METRICS
    // =================

    // Returns CPU, memory, and disk usage metrics for active system processes
    public List<ProcessMetrics> getProcessMetrics() {

        List<ProcessMetrics> list = new ArrayList<>();

        // Retrieves processes from the operating system
        List<OSProcess> processes = os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 0);

        // Stores the latest process snapshot for CPU delta calculations in the next cycle
        Map<Integer, OSProcess> newSnapshot = new HashMap<>();

        // Build delta map and total disk activity
        long totalDiskDelta = 0;

        // Stores the current total I/O bytes per process for delta comparison
        Map<Integer, Long> currentIoMap = new HashMap<>();
        Map<Integer, Long> deltaMap = new HashMap<>();

        // First: Calculates per-process disk I/O deltas and total system disk activity.
        for (OSProcess p : processes) {
            int pid = p.getProcessID();

            long currentIo = p.getBytesRead() + p.getBytesWritten();
            currentIoMap.put(pid, currentIo);

            Long prevIo = prevIoBytes.get(pid);

            if (prevIo != null) {
                long delta = Math.max(0, currentIo - prevIo);

                // Only include meaningful disk activity
                if (delta > 0) {
                    deltaMap.put(pid, delta);
                    totalDiskDelta += delta;
                }
            }
        }

        // Second: Computes CPU, RAM, and disk percentage for each process
        for (OSProcess p : processes) {

            int pid = p.getProcessID();

            // Calculates CPU usage between the previous and current OSHI tick snapshot
            OSProcess prev = prevProcMap.get(pid);

            double cpu = 0;
            if (prev != null) {
                cpu = p.getProcessCpuLoadBetweenTicks(prev) * 100.0;
            }
            cpu = clamp(cpu);

            // Calculates RAM usage as a percentage of total system memory
            double ram = (memory.getTotal() > 0)
                    ? (p.getResidentSetSize() * 100.0) / memory.getTotal()
                    : 0;
            ram = clamp(ram);

            // Calculates disk usage as a percentage of total system disk activity
            double disk = 0;

            Long delta = deltaMap.get(pid);

            if (delta != null && totalDiskDelta > 0) {
                disk = (delta * 100.0) / totalDiskDelta;
            }

            disk = clamp(disk);

            // Adds the computed metrics for this process to the result list
            list.add(new ProcessMetrics(pid, p.getName(), cpu, ram, disk));

            // Stores the current OSProcess snapshot for future CPU delta calculations
            newSnapshot.put(pid, p);
        }

        // Updates previous process snapshot
        prevProcMap = newSnapshot;

        // Updates previous disk I/O tracking data
        prevIoBytes.clear();
        prevIoBytes.putAll(currentIoMap);

        return list;
    }

    // =================
    // INNER DATA CLASSES
    // =================

    // Stores disk usage information for a single mounted file system
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

    // Stores CPU, RAM, and disk usage metrics for a single process
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
