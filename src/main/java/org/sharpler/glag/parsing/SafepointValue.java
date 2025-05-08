package org.sharpler.glag.parsing;

public record SafepointValue(
    SafepointValueType type,
    int start,
    int end
) {
}
