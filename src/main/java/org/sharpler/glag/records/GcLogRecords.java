package org.sharpler.glag.records;

import java.util.List;

public record GcLogRecords(List<GcLogRecord> records, int gcNum) {
}
