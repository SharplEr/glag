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
        assert Double.isNaN(this.finishTimeSec);
        assert TimeUtils.isTime(finishTimeSec) : "Ivalid finish time '%f'".formatted(startTimeSec);
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
        assert Double.isNaN(this.startTimeSec);
        assert TimeUtils.isTime(startTimeSec) : "Ivalid start time '%f'".formatted(startTimeSec);
        this.startTimeSec = startTimeSec;
    }

    void addOperationName(String operationName) {
        assert this.operationName == null;
        this.operationName = operationName;
    }

    void addReachingTimeNs(long reachingTimeNs) {
        assert this.reachingTimeNs == TimeUtils.NO_TIME;
        assert reachingTimeNs >= 0L;
        this.reachingTimeNs = reachingTimeNs;
    }

    void addCleanupTimeNs(long cleanupTimeNs) {
        assert this.cleanupTimeNs == TimeUtils.NO_TIME;
        assert cleanupTimeNs >= 0L;
        this.cleanupTimeNs = cleanupTimeNs;
    }

    void addInsideTimeNs(long insideTimeNs) {
        assert this.insideTimeNs == TimeUtils.NO_TIME;
        assert insideTimeNs >= 0L;
        this.insideTimeNs = insideTimeNs;
    }

    void addLeavingTimeNs(long leavingTimeNs) {
        assert this.leavingTimeNs == TimeUtils.NO_TIME;
        assert leavingTimeNs >= 0L;
        this.leavingTimeNs = leavingTimeNs;
    }

    void addTotalTimeNs(long totalTimeNs) {
        assert this.totalTimeNs == TimeUtils.NO_TIME;
        assert totalTimeNs >= 0L;
        this.totalTimeNs = totalTimeNs;
        addStartTimeSec(finishTimeSec - totalTimeNs / 1E9);
    }
}
