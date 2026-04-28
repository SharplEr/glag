package io.github.sharpler.glag.aggregations;

import io.github.sharpler.glag.records.GcLogRecords;
import io.github.sharpler.glag.records.SafepointLogRecord;
import java.util.List;

/// A single GC iteration correlated with the safepoints that overlap it.
///
/// @param safepointLog safepoint events overlapping this GC iteration
/// @param gcLog grouped GC log records for the iteration
public record GcIteration(List<SafepointLogRecord> safepointLog, GcLogRecords gcLog) {
}
