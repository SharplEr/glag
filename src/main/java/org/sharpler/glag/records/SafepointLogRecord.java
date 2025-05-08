package org.sharpler.glag.records;

public record SafepointLogRecord(
    String time, // to remove
    double startTimeSec,
    double finishTimeSec,
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs,
    int line
) {

}
