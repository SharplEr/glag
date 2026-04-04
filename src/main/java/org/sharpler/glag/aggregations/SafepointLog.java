package org.sharpler.glag.aggregations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointLog(
    RangeIndex<SafepointLogRecord> events,
    Map<String, List<SafepointLogRecord>> byTypes,
    SafepointAggregate aggregate,
    Map<String, SafepointAggregate> aggregatesByType,
    boolean hasReachingTimeNs,
    boolean hasCleanupTimeNs,
    boolean hasInsideTimeNs,
    boolean hasLeavingTimeNs,
    double totalLogTimeSec
) {
    public double totalPauseThroughputLoss() {
        return sumTimeNs(SafepointLogRecord::totalTimeNs) / totalLogTimeSec / 1E7;
    }

    public double insideSafepointThroughputLoss() {
        assert hasInsideTimeNs;
        return sumTimeNs(SafepointLogRecord::insideTimeNs) / totalLogTimeSec / 1E7;
    }

    public double averagePausePeriodSec() {
        return totalLogTimeSec / events.values().size();
    }

    public static SafepointLog parse(List<String> lines) {
        var parsedEvents = lines.stream().map(SafepointParser::parse).toList();
        var hasReachingTimeNs = parsedEvents.stream().allMatch(SafepointLogRecord::hasReachingTimeNs);
        var hasCleanupTimeNs = parsedEvents.stream().allMatch(SafepointLogRecord::hasCleanupTimeNs);
        var hasInsideTimeNs = parsedEvents.stream().allMatch(SafepointLogRecord::hasInsideTimeNs);
        var hasLeavingTimeNs = parsedEvents.stream().allMatch(SafepointLogRecord::hasLeavingTimeNs);

        var operations2events = parsedEvents.stream()
            .collect(Collectors.groupingBy(SafepointLogRecord::operationName));

        for (var entry : operations2events.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(SafepointLogRecord::totalTimeNs));
        }

        var events = new RangeIndex<>(parsedEvents);
        var aggregate = SafepointAggregate.from(parsedEvents);
        var aggregatesByType = operations2events.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> SafepointAggregate.from(x.getValue())));

        return new SafepointLog(
            events,
            operations2events,
            aggregate,
            aggregatesByType,
            hasReachingTimeNs,
            hasCleanupTimeNs,
            hasInsideTimeNs,
            hasLeavingTimeNs,
            parsedEvents.getLast().finishTimeSec() - parsedEvents.getFirst().startTimeSec()
        );
    }

    private long sumTimeNs(ToLongFunction<SafepointLogRecord> metric) {
        return events.values().stream().mapToLong(metric).sum();
    }
}
