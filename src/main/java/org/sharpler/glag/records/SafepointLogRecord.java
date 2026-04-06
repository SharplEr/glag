package org.sharpler.glag.records;

import static org.sharpler.glag.util.TimeUtils.NO_TIME;

import java.util.Objects;
import org.sharpler.glag.index.WithTimeRange;
import org.sharpler.glag.util.TimeUtils;

/// One parsed safepoint log record.
///
/// @param startTimeSec   start of the safepoint in seconds
/// @param finishTimeSec  end of the safepoint in seconds
/// @param origin         original log line
/// @param operationName  safepoint operation name
/// @param reachingTimeNs reaching time in nanoseconds, or `NO_TIME`
/// @param cleanupTimeNs  cleanup time in nanoseconds, or `NO_TIME`
/// @param insideTimeNs   inside-safepoint time in nanoseconds, or `NO_TIME`
/// @param leavingTimeNs  leaving-safepoint time in nanoseconds, or `NO_TIME`
/// @param totalTimeNs    total safepoint time in nanoseconds
public record SafepointLogRecord(
    double startTimeSec,
    double finishTimeSec,
    String origin,
    String operationName,
    long reachingTimeNs,
    long cleanupTimeNs,
    long insideTimeNs,
    long leavingTimeNs,
    long totalTimeNs
) implements WithTimeRange {
    /// Validates invariants for a parsed safepoint event.
    public SafepointLogRecord {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(operationName);
        if (Double.isNaN(startTimeSec) || Double.isNaN(finishTimeSec)) {
            throw new IllegalStateException("Safepoint time range should be defined");
        }
        if (totalTimeNs < 0L) {
            throw new IllegalStateException("Safepoint total time should be defined");
        }
        if (!TimeUtils.isOptionalTime(reachingTimeNs)) {
            throw new IllegalStateException("Safepoint reaching time should be non-negative or NO_TIME");
        }
        if (!TimeUtils.isOptionalTime(cleanupTimeNs)) {
            throw new IllegalStateException("Safepoint cleanup time should be non-negative or NO_TIME");
        }
        if (!TimeUtils.isOptionalTime(insideTimeNs)) {
            throw new IllegalStateException("Safepoint inside time should be non-negative or NO_TIME");
        }
        if (!TimeUtils.isOptionalTime(leavingTimeNs)) {
            throw new IllegalStateException("Safepoint leaving time should be non-negative or NO_TIME");
        }
    }

    /// Returns whether `reachingTimeNs` is available.
    ///
    /// @return `true` if `reachingTimeNs` is not `NO_TIME`
    public boolean hasReachingTimeNs() {
        return reachingTimeNs != NO_TIME;
    }

    /// Returns whether `insideTimeNs` is available.
    ///
    /// @return `true` if `insideTimeNs` is not `NO_TIME`
    public boolean hasInsideTimeNs() {
        return insideTimeNs != NO_TIME;
    }

    /// Returns whether `cleanupTimeNs` is available.
    ///
    /// @return `true` if `cleanupTimeNs` is not `NO_TIME`
    public boolean hasCleanupTimeNs() {
        return cleanupTimeNs != NO_TIME;
    }

    /// Returns whether `leavingTimeNs` is available.
    ///
    /// @return `true` if `leavingTimeNs` is not `NO_TIME`
    public boolean hasLeavingTimeNs() {
        return leavingTimeNs != NO_TIME;
    }
}
