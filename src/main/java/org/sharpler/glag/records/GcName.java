package org.sharpler.glag.records;

import java.util.List;

public enum GcName {
    Serial("Serial"),
    Parallel("Parallel"),
    G1("G1"),
    Shenandoah("Shenandoah"),
    Z("ZGC"),
    ;
    public static final List<GcName> VALUES = List.of(values());

    private final String name;

    GcName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
