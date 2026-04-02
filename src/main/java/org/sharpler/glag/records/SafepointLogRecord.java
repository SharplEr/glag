package org.sharpler.glag.records;

import org.sharpler.glag.index.ValueWithRange;

public record SafepointLogRecord(
    double startTimeSec,
    double finishTimeSec,
    String origin,
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs
) {
    public ValueWithRange<SafepointLogRecord> withRange() {
        return new ValueWithRange<>(
            this,
            finishTimeSec() - totalTimeNs() / 1E9,
            finishTimeSec()
        );
    }
}
