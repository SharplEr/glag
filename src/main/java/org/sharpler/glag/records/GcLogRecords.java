package org.sharpler.glag.records;

import java.util.List;
import org.sharpler.glag.index.ValueWithRange;

public record GcLogRecords(List<GcLogRecord> records, int gcNum) {
    public ValueWithRange<GcLogRecords> withRange() {
        if (records.isEmpty()) {
            throw new IllegalStateException("Should has at least single record");
        }
        var min = Double.POSITIVE_INFINITY;
        var max = Double.NEGATIVE_INFINITY;
        for (var record : records) {
            min = Math.min(min, record.timestampSec());
            max = Math.max(max, record.timestampSec());
        }
        return new ValueWithRange<>(this, min, max);
    }
}
