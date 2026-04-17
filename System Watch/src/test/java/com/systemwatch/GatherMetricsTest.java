package com.systemwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GatherMetricsTest {

    // Retrieve Metrics from GatherMetrics class
    private GatherMetrics metrics;

    // Before each test, get the metrics from OSHI
    @BeforeEach
    void setUp() {
        metrics = new GatherMetrics();
    }

    // Tests if uptime is positive
    @Test
    void testUptimeNonNegative() {
        long uptime = metrics.getUptime();
        assertTrue(uptime >= 0);
        System.out.println("Uptime: ~" + (uptime / 60) + " minutes");
    }

    /*
        JUNIT TESTS
        FOR CPU
     */

    // Make sure total CPU usage is between 0-100%
    @Test
    void testCpuUsageRange() throws InterruptedException {
        // Initializes CPU tick baseline for the first measurement
        metrics.getCpuUsage();

        Thread.sleep(500);

        // Measures CPU usage over this time interval
        double cpu = metrics.getCpuUsage();
        assertTrue(cpu >= 0 && cpu <= 100, "CPU usage must be between 0–100%");
        System.out.println("CPU Usage: " + String.format("%.2f", cpu) + "%");
    }

    // Make sure system has at least one logical core
    @Test
    void testLogicalCoreCount() {
        int cores = metrics.getLogicalCoreCount();
        assertTrue(cores > 0, "System must have at least one core");
        System.out.println("Logical Cores: " + cores);
    }

    // Makes sure thread counts are being retrieved correctly
    @Test
    void testThreadCount() {
        int threads = metrics.getThreadCount();
        assertTrue(threads > 0, "Thread count should be positive");
        System.out.println("Threads: " + threads);
    }

    /*
        JUNIT TESTS
        FOR MEMORY (RAM)
     */

    // Get memory statistics (including used, available, and total memory)
    @Test
    void testMemoryConsistency() {
        long total = metrics.getTotalMemory();
        long used = metrics.getUsedMemory();
        long available = metrics.getAvailableMemory();

        assertTrue(total > 0);
        assertTrue(used >= 0);
        assertTrue(available >= 0);

        // used + available should roughly equal total memory
        assertEquals(total, used + available, total * 0.05,
                "Used + Available should approx equal total (within 5%)");

        // Output converted from bytes to MB
        System.out.println("Used Memory: " + used / 1_000_000 + " MB");
        System.out.println("Available Memory: " + available / 1_000_000 + " MB");
        System.out.println("Used + Available: " + (used + available) / 1_000_000 + " MB");
        System.out.println("Total Memory: " + total / 1_000_000 + " MB");
    }

    // Ensure memory usage is between 0-100%
    @Test
    void testMemoryUsagePercent() {
        double percent = metrics.getMemoryUsagePercent();
        assertTrue(percent >= 0 && percent <= 100);

        System.out.println("Memory Usage: " + String.format("%.2f", percent) + "%");
    }

    /*
        JUNIT TESTS
        FOR DISK
     */

    // Make sure disk models exist
    @Test
    void testDiskModelsNotEmpty() {
        List<String> models = metrics.getDiskModels();
        assertFalse(models.isEmpty());

        System.out.println(models);
    }

    // Get disk statistics (including used, free, and total disk usage)
    // Very similar to testMemoryConsistency()
    @Test
    void testDiskMetricsConsistency() {
        List<GatherMetrics.DiskMetrics> disks = metrics.getDiskMetrics();

        assertFalse(disks.isEmpty(), "There should be at least one disk");

        // Gathers metrics for each disk store
        for (GatherMetrics.DiskMetrics d : disks) {
            assertTrue(d.total >= 0);
            assertTrue(d.used >= 0);
            assertTrue(d.free >= 0);

            // used + available should roughly equal total memory
            assertEquals(d.total, d.used + d.free, d.total * 0.1,
                    "Disk used + free should approx equal total (within 10%)");

            System.out.println("-----------------------");
            System.out.println("Disk: " + d.id);
            System.out.println("Used Memory: " + d.used / 1_000_000 + " MB");
            System.out.println("Free Memory: " + d.free / 1_000_000 + " MB");
            System.out.println("Used + Free: " + (d.used + d.free) / 1_000_000 + " MB");
            System.out.println("Total Memory: " + d.total / 1_000_000 + " MB");
            System.out.println("-----------------------");
        }
    }

    /*
        JUNIT TESTS
        FOR PROCESSES
     */

    // Make sure processes exist
    @Test
    void testProcessListNotNull() {
        List<GatherMetrics.ProcessMetrics> processes = metrics.getProcessMetrics();
        assertNotNull(processes);
        System.out.println("Processes exist! Number of processes: " + processes.size());
    }

    // Makes sure each process has metric ranges from 0-100%
    @Test
    void testProcessMetricsRange() throws InterruptedException {
        // Warm-up call to establish baseline for delta-based metrics
        metrics.getProcessMetrics();

        Thread.sleep(500);

        List<GatherMetrics.ProcessMetrics> processes = metrics.getProcessMetrics();

        int printNum = 0;

        for (GatherMetrics.ProcessMetrics p : processes) {
            assertTrue(p.cpu >= 0 && p.cpu <= 100);
            assertTrue(p.ram >= 0 && p.ram <= 100);
            assertTrue(p.disk >= 0);
            assertNotNull(p.name);

            // ONLY PRINTS FIRST FIVE PROCESSES, BUT IF TEST SUCCEEDS, THEY ALL ARE VALID
            if (printNum < 5) {
                printNum++;

                System.out.println("-----------------------");
                System.out.println("Process: " + p.name + " (PID " + p.pid + ")");
                System.out.println("CPU Usage: " + String.format("%.2f", p.cpu) + "%");
                System.out.println("RAM Usage: " + String.format("%.2f", p.ram) + "%");
                System.out.println("Disk Usage: " + String.format("%.2f", p.disk) + "%");
                System.out.println("-----------------------");
            }
        }
    }
}