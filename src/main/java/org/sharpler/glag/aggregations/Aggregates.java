package org.sharpler.glag.aggregations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sharpler.glag.records.SafepointLogRecord;

public record Aggregates(
    SafepointAggregate aggregate,
    Map<String, SafepointAggregate> aggregatesByType
) {
    public static Aggregates from(List<SafepointLogRecord> events) {
        var totalLogTimeSec = events.getLast().finishTimeSec() - events.getFirst().startTimeSec();

        var eventsByOperation = events.stream()
            .collect(Collectors.groupingBy(SafepointLogRecord::operationName));

        for (var entry : eventsByOperation.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(SafepointLogRecord::totalTimeNs));
        }

        return new Aggregates(
            SafepointAggregate.from(totalLogTimeSec, events),
            eventsByOperation.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, x -> SafepointAggregate.from(totalLogTimeSec, x.getValue())))
        );
    }
}
