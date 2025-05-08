package org.sharpler.glag.aggregations;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.index.ValueWithRange;
import org.sharpler.glag.records.GcLogRecord;
import org.sharpler.glag.records.GcName;

public record GcLog(
    @Nullable GcName gcName,
    RangeIndex<List<GcLogRecord>> timeIndex
) {
    public static RangeIndex<List<GcLogRecord>> buildIndex(Int2ObjectOpenHashMap<List<GcLogRecord>> events) {
        var ranges = new ArrayList<ValueWithRange<List<GcLogRecord>>>(events.size());
        for (var value : events.values()) {
            var stats = value.stream().mapToDouble(GcLogRecord::timestampSec).summaryStatistics();
            ranges.add(new ValueWithRange<>(value, stats.getMin(), stats.getMax()));
        }
        return new RangeIndex<>(ranges);
    }
}
