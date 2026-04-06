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
        assert TimeUtils.isTime(startTimeSec);
        assert TimeUtils.isTime(finishTimeSec);
        assert totalTimeNs >= 0L;
        assert TimeUtils.isOptionalTime(reachingTimeNs);
        assert TimeUtils.isOptionalTime(cleanupTimeNs);
        assert TimeUtils.isOptionalTime(insideTimeNs);
        assert TimeUtils.isOptionalTime(leavingTimeNs);
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
