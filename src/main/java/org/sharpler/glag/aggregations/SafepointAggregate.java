package org.sharpler.glag.aggregations;

import java.util.List;
import java.util.function.ToLongFunction;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointAggregate(
    double totalLogTimeSec,
    List<CumulativeDistributionPoint> totalTimeDistribution,
    List<CumulativeDistributionPoint> reachingTimeDistribution,
    List<CumulativeDistributionPoint> cleanupTimeDistribution,
    List<CumulativeDistributionPoint> insideTimeDistribution,
    List<CumulativeDistributionPoint> leavingTimeDistribution
) {
    public static SafepointAggregate from(List<SafepointLogRecord> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("SafepointAggregate requires at least one event");
        }

        return new SafepointAggregate(
            events.getLast().finishTimeSec() - events.getFirst().startTimeSec(),
            CumulativeDistributionBuilder.distribution(events, SafepointLogRecord::totalTimeNs),
            optionalDistribution(events, SafepointLogRecord::hasReachingTimeNs, SafepointLogRecord::reachingTimeNs),
            optionalDistribution(events, SafepointLogRecord::hasCleanupTimeNs, SafepointLogRecord::cleanupTimeNs),
            optionalDistribution(events, SafepointLogRecord::hasInsideTimeNs, SafepointLogRecord::insideTimeNs),
            optionalDistribution(events, SafepointLogRecord::hasLeavingTimeNs, SafepointLogRecord::leavingTimeNs)
        );
    }

    private static List<CumulativeDistributionPoint> optionalDistribution(
        List<SafepointLogRecord> events,
        java.util.function.Predicate<SafepointLogRecord> isPresent,
        ToLongFunction<SafepointLogRecord> metric
    ) {
        return events.stream().allMatch(isPresent) ? CumulativeDistributionBuilder.distribution(events, metric) : List.of();
    }

}
