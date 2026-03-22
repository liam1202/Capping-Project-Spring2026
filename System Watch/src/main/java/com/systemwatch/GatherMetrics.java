package com.systemwatch;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.software.os.OperatingSystem;

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

    // Gets processor identifier information
    public String getProcessorInfo() {
        // The CPU's identifier strings ,including name, vendor,
        // stepping, model, and family information (also called the signature of a CPU).
        CentralProcessor.ProcessorIdentifier identifier = processor.getProcessorIdentifier();

        // TODO: Organize output (especially if not Intel chip)
        return identifier.toString();
    }

    // GATHER MEMORY METRICS
    public long getTotalMemory() { return memory.getTotal(); }
    public long getAvailableMemory() { return memory.getAvailable(); }

    // GATHER DISK METRICS
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
}
