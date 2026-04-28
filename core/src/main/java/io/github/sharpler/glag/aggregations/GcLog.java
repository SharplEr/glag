package io.github.sharpler.glag.aggregations;

import io.github.sharpler.glag.index.RangeIndex;
import io.github.sharpler.glag.parsing.GcParser;
import io.github.sharpler.glag.records.GcLogRecord;
import io.github.sharpler.glag.records.GcLogRecords;
import io.github.sharpler.glag.records.GcName;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// Parsed GC log together with the detected collector name and an index by time range.
///
/// @param gcName detected collector name, or `null` if detection failed
/// @param timeIndex GC iterations indexed by their time range
public record GcLog(
    @Nullable GcName gcName,
    RangeIndex<GcLogRecords> timeIndex
) {
    /// Parses raw GC log lines into grouped GC iterations.
    ///
    /// @param lines raw GC log lines
    /// @return parsed GC log with grouped iterations and detected GC name
    public static GcLog parse(List<String> lines) {
        var gcIteration = new Int2ObjectOpenHashMap<ArrayList<GcLogRecord>>();
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
                .computeIfAbsent(logRecord.gcNum(), key -> new ArrayList<>())
                .add(logRecord);
        }

        var gcRecords = gcIteration.int2ObjectEntrySet().stream()
            .map(entry -> new GcLogRecords(entry.getValue(), entry.getIntKey()))
            .toList();

        return new GcLog(gcName, new RangeIndex<>(gcRecords));
    }
}
