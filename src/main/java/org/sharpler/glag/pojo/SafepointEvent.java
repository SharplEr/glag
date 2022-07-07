package org.sharpler.glag.pojo;

public record SafepointEvent(
    String time,
    double timestampSec,
    String level, // info
    String type, // safepoint
    String operationName,
    long reachingTimeNs,
    long insideTimeNs,
    long totalTimeNs,
    int line
) {

}
