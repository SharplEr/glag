package org.sharpler.glag.records;

public record SafepointLogRecord(
    double startTimeSec,
    double finishTimeSec,
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs,
    int line
) {

}
