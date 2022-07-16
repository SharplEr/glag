package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.index.ValueWithRange;
import org.sharpler.glag.records.GcLogRecord;
import org.sharpler.glag.records.GcName;

public record GcLog(
    @Nullable GcName gcName,
    RangeIndex<List<GcLogRecord>> timeIndex
) {
    public static RangeIndex<List<GcLogRecord>> buildIndex(Map<Integer, List<GcLogRecord>> events) {
        var ranges = new ArrayList<ValueWithRange<List<GcLogRecord>>>(events.size());
        for (var e : events.entrySet()) {
            var stats = e.getValue().stream().mapToDouble(GcLogRecord::timestampSec).summaryStatistics();
            ranges.add(new ValueWithRange<>(e.getValue(), stats.getMin(), stats.getMax()));
        }
        return new RangeIndex<>(ranges);
    }
}
