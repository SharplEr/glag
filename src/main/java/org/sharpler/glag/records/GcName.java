package org.sharpler.glag.records;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum GcName {
    Serial("Serial"),
    Parallel("Parallel"),
    G1("G1"),
    Shenandoah("Shenandoah"),
    Z("ZGC"),
    ;
    public static final List<GcName> VALUES = List.of(values());
    public static final Map<String, GcName> MAP = VALUES.stream()
        .collect(Collectors.toUnmodifiableMap(GcName::getName, Function.identity()));
    private final String name;

    GcName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
