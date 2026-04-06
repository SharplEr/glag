package org.sharpler.glag.aggregations;

import java.util.List;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.SafepointLogRecord;

public record GcIteration(List<SafepointLogRecord> safepointLog, GcLogRecords gcLog) {
}
