package org.sharpler.glag.records;

public record SafepointLogRecord(
    String time,
    double startTimeSec,
    double finishTimeSec,
    String level, // info
    String type, // safepoint
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs,
    int line
) {

}
