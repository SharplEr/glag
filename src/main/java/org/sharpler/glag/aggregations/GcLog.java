package org.sharpler.glag.aggregations;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import javax.annotation.Nullable;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.index.ValueWithRange;
import org.sharpler.glag.records.GcLogRecord;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.GcName;

public record GcLog(
    @Nullable GcName gcName,
    RangeIndex<GcLogRecords> timeIndex
) {
    public static RangeIndex<GcLogRecords> buildIndex(Int2ObjectOpenHashMap<GcLogRecords> events) {
        var ranges = new ArrayList<ValueWithRange<GcLogRecords>>(events.size());
        for (var value : events.values()) {
            var stats = value.records().stream().mapToDouble(GcLogRecord::timestampSec).summaryStatistics();
            ranges.add(new ValueWithRange<>(value, stats.getMin(), stats.getMax()));
        }
        return new RangeIndex<>(ranges);
    }
}
