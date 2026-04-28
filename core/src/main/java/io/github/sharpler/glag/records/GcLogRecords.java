package io.github.sharpler.glag.records;

import io.github.sharpler.glag.index.WithTimeRange;
import java.util.List;

/// GC log records grouped by GC iteration number.
///
/// @param records raw GC log records for the iteration
/// @param gcNum GC iteration number
/// @param startTimeSec earliest timestamp in `records`
/// @param finishTimeSec latest timestamp in `records`
public record GcLogRecords(List<GcLogRecord> records, int gcNum, double startTimeSec, double finishTimeSec) implements WithTimeRange {
    /// Builds a grouped GC iteration and derives its time range from `records`.
    ///
    /// @param records raw GC log records for one iteration
    /// @param gcNum GC iteration number
    public GcLogRecords(List<GcLogRecord> records, int gcNum) {
        if (records.isEmpty()) {
            throw new IllegalStateException("Should has at least single record");
        }
        var min = Double.POSITIVE_INFINITY;
        var max = Double.NEGATIVE_INFINITY;
        for (var record : records) {
            min = Math.min(min, record.timestampSec());
            max = Math.max(max, record.timestampSec());
        }
        this(records, gcNum, min, max);
    }
}
