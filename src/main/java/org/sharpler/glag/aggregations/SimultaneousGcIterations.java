package org.sharpler.glag.aggregations;

import java.util.List;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.SafepointLogRecord;

public record SimultaneousGcIterations(
    List<SafepointLogRecord> safepointLog,
    List<GcLogRecords> gcs
) {
}
