package org.sharpler.glag.records;

import org.jspecify.annotations.Nullable;

public enum GcName {
    Serial("Serial"),
    Parallel("Parallel"),
    G1("G1"),
    Shenandoah("Shenandoah"),
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

    public String getName() {
        return name;
    }

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
