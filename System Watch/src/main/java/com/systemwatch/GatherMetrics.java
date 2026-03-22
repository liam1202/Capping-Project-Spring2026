package com.systemwatch;

import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

// BACKEND: Using OSHI library to gather system metrics
// Source: https://github.com/oshi/oshi
public class GatherMetrics {

    // Default constructor, which asserts system metrics
    GatherMetrics(){
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();

        // Assert basic system metrics
        assert(os != null) : "OS not detected";
        assert(os.getFamily() != null) : "OS Family not detected";
        assert(os.getVersionInfo() != null) : "OS Version info not detected";
        assert(os.getBitness() == 32 || os.getBitness() == 64) : "OS Bitness should be 32 or 64";
    }
}
