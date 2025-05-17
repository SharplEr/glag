package org.sharpler.glag.records;

import java.util.List;

public record GcIteration(List<SafepointLogRecord> safepointLog, GcLogRecords gcLog) {
}
