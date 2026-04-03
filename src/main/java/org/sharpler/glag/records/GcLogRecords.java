package org.sharpler.glag.records;

import java.util.List;
import org.sharpler.glag.index.WithTimeRange;

public record GcLogRecords(List<GcLogRecord> records, int gcNum, double startTimeSec, double finishTimeSec) implements WithTimeRange {
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
