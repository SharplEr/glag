package org.sharpler.glag.records;

import java.util.Objects;
import org.sharpler.glag.index.WithTimeRange;

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
    public static final long NO_TIME = -1L;

    public SafepointLogRecord {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(operationName);
        if (Double.isNaN(startTimeSec) || Double.isNaN(finishTimeSec)) {
            throw new IllegalStateException("Safepoint time range should be defined");
        }
        if (totalTimeNs < 0L) {
            throw new IllegalStateException("Safepoint total time should be defined");
        }
        if (reachingTimeNs < 0L && reachingTimeNs != NO_TIME) {
            throw new IllegalStateException("Safepoint reaching time should be non-negative or NO_TIME");
        }
        if (cleanupTimeNs < 0L && cleanupTimeNs != NO_TIME) {
            throw new IllegalStateException("Safepoint cleanup time should be non-negative or NO_TIME");
        }
        if (insideTimeNs < 0L && insideTimeNs != NO_TIME) {
            throw new IllegalStateException("Safepoint inside time should be non-negative or NO_TIME");
        }
        if (leavingTimeNs < 0L && leavingTimeNs != NO_TIME) {
            throw new IllegalStateException("Safepoint leaving time should be non-negative or NO_TIME");
        }
    }

    public boolean hasReachingTimeNs() {
        return reachingTimeNs != NO_TIME;
    }

    public boolean hasInsideTimeNs() {
        return insideTimeNs != NO_TIME;
    }

    public boolean hasCleanupTimeNs() {
        return cleanupTimeNs != NO_TIME;
    }

    public boolean hasLeavingTimeNs() {
        return leavingTimeNs != NO_TIME;
    }
}
