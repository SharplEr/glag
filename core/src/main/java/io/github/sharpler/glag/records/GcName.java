package io.github.sharpler.glag.records;

import org.jspecify.annotations.Nullable;

/// Supported garbage collector names that can be detected from log text.
public enum GcName {
    /// Serial GC.
    Serial("Serial"),
    /// Parallel GC.
    Parallel("Parallel"),
    /// G1 GC.
    G1("G1"),
    /// Shenandoah GC.
    Shenandoah("Shenandoah"),
    /// Z Garbage Collector.
    Z("ZGC", "The Z Garbage Collector"),
    ;
    private static final GcName[] VALUES = values();

    private final String name;
    private final @Nullable String extraAlias;

    GcName(String name) {
        this.name = name;
        extraAlias = null;
    }

    GcName(String name, String extraAlias) {
        this.name = name;
        this.extraAlias = extraAlias;
    }

    /// Returns the display name used in reports.
    ///
    /// @return display name of the collector
    public String getName() {
        return name;
    }

    /// Tries to detect the collector name from a raw log line.
    ///
    /// @param line raw GC log line
    /// @return detected collector name, or `null` if the line is inconclusive
    public static @Nullable GcName findGcName(String line) {
        for (var gc : GcName.VALUES) {
            if (line.contains(gc.name)) {
                return gc;
            }
            if (gc.extraAlias != null && line.contains(gc.extraAlias)) {
                return gc;
            }
        }
        return null;
    }
}
