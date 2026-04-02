package org.sharpler.glag.aggregations;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.parsing.GcParser;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.GcName;

public record GcLog(
    @Nullable GcName gcName,
    RangeIndex<GcLogRecords> timeIndex
) {
    public static GcLog parse(List<String> lines) {
        var gcIteration = new Int2ObjectOpenHashMap<GcLogRecords>();
        GcName gcName = null;

        for (var line : lines) {
            if (gcName == null) {
                gcName = GcName.findGcName(line);
            }

            var logRecord = GcParser.parse(line);
            if (logRecord == null) {
                continue;
            }
            gcIteration
                .computeIfAbsent(logRecord.gcNum(), key -> new GcLogRecords(new ArrayList<>(), key))
                .records()
                .add(logRecord);
        }

        return new GcLog(gcName, RangeIndex.create(gcIteration.values(), GcLogRecords::withRange));
    }
}
