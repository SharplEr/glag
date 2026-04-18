package org.sharpler.glag.aggregations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sharpler.glag.records.SafepointLogRecord;

/// Precomputed safepoint aggregates for the whole log and for each operation type.
///
/// @param aggregate        overall safepoint statistics for the log
/// @param aggregatesByType safepoint statistics grouped by operation name
public record Aggregates(
    SafepointAggregate aggregate,
    Map<String, SafepointAggregate> aggregatesByType
) {
    /// Builds aggregates for a non-empty list of parsed safepoint events.
    ///
    /// @param events parsed safepoint events from a single log
    /// @return overall and per-operation aggregates for `events`
    public static Aggregates from(List<SafepointLogRecord> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("`events` should be not empty");
        }
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
