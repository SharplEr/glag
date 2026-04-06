package org.sharpler.glag.records;

/// One parsed GC log line.
///
/// @param origin original log line
/// @param timestampSec parsed uptime timestamp in seconds
/// @param gcNum GC iteration number from the log line
public record GcLogRecord(
    String origin,
    double timestampSec,
    int gcNum
) {
}
