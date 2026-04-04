package org.sharpler.glag.aggregations;

import java.util.List;
import java.util.function.ToLongFunction;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointAggregate(
    double totalLogTimeSec,
    int eventsCount,
    long totalTimeNsSum,
    long insideTimeNsSum,
    List<CumulativeDistributionPoint> totalTimeDistribution,
    List<CumulativeDistributionPoint> reachingTimeDistribution,
    List<CumulativeDistributionPoint> cleanupTimeDistribution,
    List<CumulativeDistributionPoint> insideTimeDistribution,
    List<CumulativeDistributionPoint> leavingTimeDistribution
) {
    public double totalPauseThroughputLoss() {
        return totalPauseThroughputLoss(totalLogTimeSec);
    }

    public double totalPauseThroughputLoss(double observedTimeSec) {
        return totalTimeNsSum / observedTimeSec / 1E7;
    }

    public double insideSafepointThroughputLoss() {
        return insideSafepointThroughputLoss(totalLogTimeSec);
    }

    public double insideSafepointThroughputLoss(double observedTimeSec) {
        assert !insideTimeDistribution.isEmpty();
        return insideTimeNsSum / observedTimeSec / 1E7;
    }

    public double averagePausePeriodSec() {
        return totalLogTimeSec / eventsCount;
    }

    public static SafepointAggregate from(List<SafepointLogRecord> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("SafepointAggregate requires at least one event");
        }

        var totalTimeNsSum = events.stream().mapToLong(SafepointLogRecord::totalTimeNs).sum();
        var insideTimeNsSum = events.stream()
            .filter(SafepointLogRecord::hasInsideTimeNs)
            .mapToLong(SafepointLogRecord::insideTimeNs)
            .sum();

        return new SafepointAggregate(
            events.getLast().finishTimeSec() - events.getFirst().startTimeSec(),
            events.size(),
            totalTimeNsSum,
            insideTimeNsSum,
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
