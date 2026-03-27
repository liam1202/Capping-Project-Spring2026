PRAGMA foreign_keys = ON; -- Foreign key constraints enabled

------------------------------------------------------------
-- CPU TABLE
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cpu (
    timestamp INTEGER PRIMARY KEY,                -- Unix epoch time in ms
    cpu_usage_percentage REAL NOT NULL,           -- Overall CPU utilization
    interrupts BIGINT NOT NULL,                   -- Interrupt count
    user_mode_time REAL NOT NULL,                 -- Time spent in user mode
    kernel_mode_time REAL NOT NULL,               -- Time spent in kernel mode
    thread_count INTEGER NOT NULL                 -- Total CPU threads
);

CREATE INDEX IF NOT EXISTS idx_cpu_timestamp
ON cpu(timestamp);

------------------------------------------------------------
-- RAM TABLE
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ram (
    timestamp INTEGER PRIMARY KEY,                -- Unix epoch time in ms
    total_memory_bytes BIGINT NOT NULL,           -- Total RAM
    used_memory_bytes BIGINT NOT NULL,            -- RAM in use
    cached_memory_bytes BIGINT NOT NULL,          -- Cached RAM
    page_faults BIGINT NOT NULL                   -- Page faults
);

CREATE INDEX IF NOT EXISTS idx_ram_timestamp
ON ram(timestamp);

------------------------------------------------------------
-- DISK TABLE
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS disk (
    timestamp INTEGER NOT NULL,
    disk_id TEXT NOT NULL,                        -- Disk identifier (e.g., sda1)
    disk_total_bytes BIGINT NOT NULL,
    disk_used_bytes BIGINT NOT NULL,
    disk_free_bytes BIGINT NOT NULL,

    PRIMARY KEY (timestamp, disk_id)
);

CREATE INDEX IF NOT EXISTS idx_disk_timestamp
ON disk(timestamp);

CREATE INDEX IF NOT EXISTS idx_disk_disk_id
ON disk(disk_id);

------------------------------------------------------------
-- PROCESS TABLE
------------------------------------------------------------
CREATE TABLE IF NOT EXISTS process (
    timestamp INTEGER NOT NULL,
    pid INTEGER NOT NULL,
    process_name TEXT NOT NULL,

    cpu_percent REAL NOT NULL,
    ram_percent REAL NOT NULL,
    disk_percent REAL NOT NULL,

    marked_for_suspension INTEGER NOT NULL DEFAULT 0,
    valid_for_tracking INTEGER NOT NULL DEFAULT 1,

    PRIMARY KEY (timestamp, pid)
);

CREATE INDEX IF NOT EXISTS idx_process_timestamp
ON process(timestamp);

CREATE INDEX IF NOT EXISTS idx_process_pid
ON process(pid);

CREATE INDEX IF NOT EXISTS idx_process_tracking
ON process(valid_for_tracking);