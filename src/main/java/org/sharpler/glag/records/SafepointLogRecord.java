package org.sharpler.glag.records;

import org.sharpler.glag.index.WithTimeRange;

public record SafepointLogRecord(
    double startTimeSec,
    double finishTimeSec,
    String origin,
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs
) implements WithTimeRange {
}
