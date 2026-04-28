package io.github.sharpler.glag.aggregations;

import io.github.sharpler.glag.records.GcLogRecords;
import io.github.sharpler.glag.records.SafepointLogRecord;
import java.util.List;

/// A group of overlapping GC iterations correlated with the same safepoint window.
///
/// @param safepointLog safepoint events overlapping the grouped GC iterations
/// @param gcs overlapping GC iterations sorted by GC number
public record SimultaneousGcIterations(
    List<SafepointLogRecord> safepointLog,
    List<GcLogRecords> gcs
) {
}
