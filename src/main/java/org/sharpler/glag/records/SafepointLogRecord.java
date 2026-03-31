package org.sharpler.glag.records;

public record SafepointLogRecord(
    double startTimeSec,
    double finishTimeSec,
    String origin,
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs
) {

}
