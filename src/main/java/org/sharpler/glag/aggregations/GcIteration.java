package org.sharpler.glag.aggregations;

import java.util.List;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.SafepointLogRecord;

/// A single GC iteration correlated with the safepoints that overlap it.
///
/// @param safepointLog safepoint events overlapping this GC iteration
/// @param gcLog grouped GC log records for the iteration
public record GcIteration(List<SafepointLogRecord> safepointLog, GcLogRecords gcLog) {
}
