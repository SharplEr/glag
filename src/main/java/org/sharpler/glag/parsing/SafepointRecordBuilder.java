package org.sharpler.glag.parsing;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.records.SafepointLogRecord;
import org.sharpler.glag.util.TimeUtils;

final class SafepointRecordBuilder {
    private final String origin;
    private double startTimeSec = Double.NaN;
    private double finishTimeSec = Double.NaN;
    private @Nullable String operationName = null;
    private long reachingTimeNs = TimeUtils.NO_TIME;
    private long cleanupTimeNs = TimeUtils.NO_TIME;
    private long insideTimeNs = TimeUtils.NO_TIME;
    private long leavingTimeNs = TimeUtils.NO_TIME;
    private long totalTimeNs = TimeUtils.NO_TIME;

    SafepointRecordBuilder(String origin) {
        this.origin = origin;
    }

    String origin() {
        return origin;
    }

    void addFinishTimeSec(double finishTimeSec) {
        assert TimeUtils.isTime(finishTimeSec) : "Ivalid finish time '%f'".formatted(startTimeSec);

        if (!Double.isNaN(this.finishTimeSec)) {
            throw new IllegalStateException("finishTimeSec already set as %f".formatted(this.finishTimeSec));
        }
        this.finishTimeSec = finishTimeSec;
    }

    SafepointLogRecord build() {
        if (!TimeUtils.isTime(startTimeSec) || !TimeUtils.isTime(finishTimeSec)) {
            throw new IllegalStateException("Safepoint time range should be defined");
        }
        if (operationName == null) {
            throw new IllegalStateException("Safepoint operation name should be defined");
        }
        if (totalTimeNs < 0L) {
            throw new IllegalStateException("Safepoint total time should be defined");
        }
        return new SafepointLogRecord(
            startTimeSec,
            finishTimeSec,
            origin,
            Objects.requireNonNull(operationName),
            reachingTimeNs,
            cleanupTimeNs,
            insideTimeNs,
            leavingTimeNs,
            totalTimeNs
        );
    }

    private void addStartTimeSec(double startTimeSec) {
        if (!Double.isNaN(this.startTimeSec)) {
            throw new IllegalStateException("startTimeSec already set as %f".formatted(this.startTimeSec));
        }
        if (!TimeUtils.isTime(startTimeSec)) {
            throw new IllegalArgumentException("Ivalid start time '%f'".formatted(startTimeSec));
        }
        this.startTimeSec = startTimeSec;
    }

    void addOperationName(String operationName) {
        if (this.operationName != null) {
            throw new IllegalStateException("operationName already set as %s".formatted(this.operationName));
        }
        this.operationName = operationName;
    }

    void addReachingTimeNs(long reachingTimeNs) {
        assert reachingTimeNs >= 0L;

        if (this.reachingTimeNs != TimeUtils.NO_TIME) {
            throw new IllegalStateException("reachingTimeNs already set as %d".formatted(this.reachingTimeNs));
        }
        this.reachingTimeNs = reachingTimeNs;
    }

    void addCleanupTimeNs(long cleanupTimeNs) {
        assert cleanupTimeNs >= 0L;

        if (this.cleanupTimeNs != TimeUtils.NO_TIME) {
            throw new IllegalStateException("cleanupTimeNs already set as %d".formatted(this.cleanupTimeNs));
        }
        this.cleanupTimeNs = cleanupTimeNs;
    }

    void addInsideTimeNs(long insideTimeNs) {
        assert insideTimeNs >= 0L;

        if (this.insideTimeNs != TimeUtils.NO_TIME) {
            throw new IllegalStateException("insideTimeNs already set as %d".formatted(this.insideTimeNs));
        }

        this.insideTimeNs = insideTimeNs;
    }

    void addLeavingTimeNs(long leavingTimeNs) {
        assert leavingTimeNs >= 0L;

        if (this.leavingTimeNs != TimeUtils.NO_TIME) {
            throw new IllegalStateException("leavingTimeNs already set as %d".formatted(this.leavingTimeNs));
        }
        this.leavingTimeNs = leavingTimeNs;
    }

    void addTotalTimeNs(long totalTimeNs) {
        assert totalTimeNs >= 0L;

        if (this.totalTimeNs != TimeUtils.NO_TIME) {
            throw new IllegalStateException("totalTimeNs already set as %d".formatted(this.totalTimeNs));
        }

        this.totalTimeNs = totalTimeNs;
        addStartTimeSec(finishTimeSec - totalTimeNs / 1E9);
    }
}
