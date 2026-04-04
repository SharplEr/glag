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
    public static final long NO_TIME = SafepointLogRecord.NO_TIME;

    public boolean hasReachingTimeNs() {
        return !reachingTimeDistribution.isEmpty();
    }

    public boolean hasCleanupTimeNs() {
        return !cleanupTimeDistribution.isEmpty();
    }

    public boolean hasInsideTimeNs() {
        return insideTimeNsSum != NO_TIME;
    }

    public boolean hasLeavingTimeNs() {
        return !leavingTimeDistribution.isEmpty();
    }

    public double totalPauseThroughputLoss() {
        return totalTimeNsSum / totalLogTimeSec / 1E7;
    }

    public double insideSafepointThroughputLoss() {
        assert hasInsideTimeNs();
        return insideTimeNsSum / totalLogTimeSec / 1E7;
    }

    public double averagePausePeriodSec() {
        return totalLogTimeSec / eventsCount;
    }

    public static SafepointAggregate from(double totalLogTimeSec, List<SafepointLogRecord> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("SafepointAggregate requires at least one event");
        }

        var totalTimeNsSum = events.stream().mapToLong(SafepointLogRecord::totalTimeNs).sum();
        var insideTimeNsSum = events.stream().allMatch(SafepointLogRecord::hasInsideTimeNs) ?
            events.stream().mapToLong(SafepointLogRecord::insideTimeNs).sum() :
            NO_TIME;

        return new SafepointAggregate(
            totalLogTimeSec,
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
