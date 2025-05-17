package org.sharpler.glag.records;

import java.util.List;

public record SimultaneousGcIterations(
    List<SafepointLogRecord> safepointLog,
    List<GcLogRecords> gcs
) {
}
