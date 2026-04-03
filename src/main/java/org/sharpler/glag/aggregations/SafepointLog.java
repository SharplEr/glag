package org.sharpler.glag.aggregations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointLog(
    List<SafepointLogRecord> events,
    Map<String, List<SafepointLogRecord>> byTypes,
    Map<String, List<CumulativeDistributionPoint>> distributions,
    boolean hasReachingTimeNs,
    boolean hasCleanupTimeNs,
    boolean hasInsideTimeNs,
    boolean hasLeavingTimeNs,
    RangeIndex<SafepointLogRecord> timeIndex,
    double totalLogTimeSec
) {
    public static SafepointLog parse(List<String> lines) {
        var events = lines.stream().map(SafepointParser::parse).toList();
        var hasReachingTimeNs = events.stream().allMatch(SafepointLogRecord::hasReachingTimeNs);
        var hasCleanupTimeNs = events.stream().allMatch(SafepointLogRecord::hasCleanupTimeNs);
        var hasInsideTimeNs = events.stream().allMatch(SafepointLogRecord::hasInsideTimeNs);
        var hasLeavingTimeNs = events.stream().allMatch(SafepointLogRecord::hasLeavingTimeNs);

        var operations2events = events.stream()
            .collect(Collectors.groupingBy(SafepointLogRecord::operationName));

        for (var entry : operations2events.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(SafepointLogRecord::totalTimeNs));
        }

        var operations2stat = operations2events.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> CumulativeDistributionBuilder.operationTimeDistribution(x.getValue())));

        return new SafepointLog(
            events,
            operations2events,
            operations2stat,
            hasReachingTimeNs,
            hasCleanupTimeNs,
            hasInsideTimeNs,
            hasLeavingTimeNs,
            new RangeIndex<>(events),
            events.getLast().finishTimeSec() - events.getFirst().startTimeSec()
        );
    }
}
