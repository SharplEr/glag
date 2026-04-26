package org.sharpler.glag.aggregations;

import java.util.List;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.SafepointLogRecord;

/// A group of overlapping GC iterations correlated with the same safepoint window.
///
/// @param safepointLog safepoint events overlapping the grouped GC iterations
/// @param gcs overlapping GC iterations sorted by GC number
public record SimultaneousGcIterations(
    List<SafepointLogRecord> safepointLog,
    List<GcLogRecords> gcs
) {
}
