package org.sharpler.glag.parsing;

import static org.sharpler.glag.parsing.SafepointValueType.AT_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.CLEANUP;
import static org.sharpler.glag.parsing.SafepointValueType.LEAVING_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.REACHING_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.SAFEPOINT_NAME;
import static org.sharpler.glag.parsing.SafepointValueType.TOTAL;

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

    void addFinishTimeSec(double finishTimeSec) {
        assert Double.isNaN(this.finishTimeSec);
        assert TimeUtils.isTime(finishTimeSec) : "Ivalid finish time '%f'".formatted(startTimeSec);
        this.finishTimeSec = finishTimeSec;
    }

    void addValue(SafepointValueType type, int start, int end) {
        switch (type) {
            case SAFEPOINT_NAME -> addOperationName(SAFEPOINT_NAME.parseString(origin, start, end));
            case REACHING_SAFEPOINT -> addReachingTimeNs(REACHING_SAFEPOINT.parseLong(origin, start, end));
            case CLEANUP -> addCleanupTimeNs(CLEANUP.parseLong(origin, start, end));
            case AT_SAFEPOINT -> addInsideTimeNs(AT_SAFEPOINT.parseLong(origin, start, end));
            case LEAVING_SAFEPOINT -> addLeavingTimeNs(LEAVING_SAFEPOINT.parseLong(origin, start, end));
            case TOTAL -> addTotalTimeNs(TOTAL.parseLong(origin, start, end));
        }
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

    private void addOperationName(String operationName) {
        assert this.operationName == null;
        this.operationName = operationName;
    }

    private void addReachingTimeNs(long reachingTimeNs) {
        assert this.reachingTimeNs == TimeUtils.NO_TIME;
        assert reachingTimeNs >= 0L;
        this.reachingTimeNs = reachingTimeNs;
    }

    private void addCleanupTimeNs(long cleanupTimeNs) {
        assert this.cleanupTimeNs == TimeUtils.NO_TIME;
        assert cleanupTimeNs >= 0L;
        this.cleanupTimeNs = cleanupTimeNs;
    }

    private void addInsideTimeNs(long insideTimeNs) {
        assert this.insideTimeNs == TimeUtils.NO_TIME;
        assert insideTimeNs >= 0L;
        this.insideTimeNs = insideTimeNs;
    }

    private void addLeavingTimeNs(long leavingTimeNs) {
        assert this.leavingTimeNs == TimeUtils.NO_TIME;
        assert leavingTimeNs >= 0L;
        this.leavingTimeNs = leavingTimeNs;
    }

    private void addTotalTimeNs(long totalTimeNs) {
        assert this.totalTimeNs == TimeUtils.NO_TIME;
        assert totalTimeNs >= 0L;
        this.totalTimeNs = totalTimeNs;
        addStartTimeSec(finishTimeSec - totalTimeNs / 1E9);
    }
}
