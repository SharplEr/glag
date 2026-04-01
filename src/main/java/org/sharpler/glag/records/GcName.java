package org.sharpler.glag.records;

import org.jspecify.annotations.Nullable;

public enum GcName {
    Serial("Serial"),
    Parallel("Parallel"),
    G1("G1"),
    Shenandoah("Shenandoah"),
    Z("ZGC"),
    ;
    private static final GcName[] VALUES = values();

    private final String name;

    GcName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static @Nullable GcName findGcName(String line) {
        for (var gc : GcName.VALUES) {
            if (line.contains(gc.name)) {
                return gc;
            }
        }
        return null;
    }
}
