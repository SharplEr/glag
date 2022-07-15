package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.index.ValueWithRange;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointLog(
    List<SafepointLogRecord> events,
    Map<String, List<SafepointLogRecord>> byTypes,
    Map<String, List<CumulativeDistributionPoint>> distributions,

    RangeIndex<SafepointLogRecord> timeIndex,

    double startLogSec,
    double finishLogSec
) {
    public static RangeIndex<SafepointLogRecord> buildIndex(List<SafepointLogRecord> events) {
        var ranges = new ArrayList<ValueWithRange<SafepointLogRecord>>(events.size());

        for (var event : events) {
            ranges.add(new ValueWithRange<>(
                event,
                event.finishTimeSec() - event.totalTimeNs() / 1E9,
                event.finishTimeSec()
            ));
        }

        return new RangeIndex<>(ranges);
    }
}
