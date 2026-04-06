package org.sharpler.glag.aggregations;

import java.util.List;
import java.util.function.ToLongFunction;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.records.SafepointLogRecord;

/// Statistical summary of a set of safepoint events without storing the raw events themselves.
///
/// @param totalLogTimeSec observed wall-clock span of the full safepoint log
/// @param eventsCount number of safepoint events in the aggregate
/// @param totalTimeNsSum sum of `totalTimeNs` across all events
/// @param insideTimeNsSum sum of `insideTimeNs`, or `NO_TIME` if any event does not have it
/// @param totalTimeDistribution cumulative distribution of total safepoint time
/// @param reachingTimeDistribution cumulative distribution of time to safepoint
/// @param cleanupTimeDistribution cumulative distribution of cleanup time
/// @param insideTimeDistribution cumulative distribution of time inside safepoint
/// @param leavingTimeDistribution cumulative distribution of time to leave safepoint
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
    /// Sentinel for an unavailable optional timing value.
    public static final long NO_TIME = SafepointLogRecord.NO_TIME;

    /// Returns whether all aggregated events have `reachingTimeNs`.
    ///
    /// @return `true` if `reachingTimeNs` is available for all events
    public boolean hasReachingTimeNs() {
        return !reachingTimeDistribution.isEmpty();
    }

    /// Returns whether all aggregated events have `cleanupTimeNs`.
    ///
    /// @return `true` if `cleanupTimeNs` is available for all events
    public boolean hasCleanupTimeNs() {
        return !cleanupTimeDistribution.isEmpty();
    }

    /// Returns whether all aggregated events have `insideTimeNs`.
    ///
    /// @return `true` if `insideTimeNs` is available for all events
    public boolean hasInsideTimeNs() {
        return insideTimeNsSum != NO_TIME;
    }

    /// Returns whether all aggregated events have `leavingTimeNs`.
    ///
    /// @return `true` if `leavingTimeNs` is available for all events
    public boolean hasLeavingTimeNs() {
        return !leavingTimeDistribution.isEmpty();
    }

    /// Returns total pause time as a percentage of the observed log time.
    ///
    /// @return throughput loss caused by total pause time
    public double totalPauseThroughputLoss() {
        return totalTimeNsSum / totalLogTimeSec / 1E7;
    }

    /// Returns inside-safepoint time as a percentage of the observed log time.
    ///
    /// @return throughput loss caused by inside-safepoint time
    public double insideSafepointThroughputLoss() {
        assert hasInsideTimeNs();
        return insideTimeNsSum / totalLogTimeSec / 1E7;
    }

    /// Returns the average period between aggregated safepoint events.
    ///
    /// @return average pause period in seconds per event
    public double averagePausePeriodSec() {
        return totalLogTimeSec / eventsCount;
    }

    /// Builds a statistical summary for a non-empty set of safepoint events.
    ///
    /// @param totalLogTimeSec observed wall-clock span of the full safepoint log
    /// @param events events to summarize
    /// @return aggregate statistics for `events`
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
