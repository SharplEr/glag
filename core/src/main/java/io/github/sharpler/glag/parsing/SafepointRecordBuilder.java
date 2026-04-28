package io.github.sharpler.glag.parsing;

import io.github.sharpler.glag.records.SafepointLogRecord;
import io.github.sharpler.glag.util.TimeUtils;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

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

    @Nullable SafepointLogRecord buildOrNull() {
        if (!TimeUtils.isTime(startTimeSec)) {
            return null;
        }
        if (!TimeUtils.isTime(finishTimeSec)) {
            return null;
        }
        if (operationName == null) {
            return null;
        }
        if (totalTimeNs < 0L) {
            return null;
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

    boolean addFinishTimeSec(double finishTimeSec) {
        assert TimeUtils.isTime(finishTimeSec);

        if (!Double.isNaN(this.finishTimeSec)) {
            return false;
        }
        this.finishTimeSec = finishTimeSec;
        return true;
    }

    private boolean addStartTimeSec(double startTimeSec) {
        if (!Double.isNaN(this.startTimeSec) || !TimeUtils.isTime(startTimeSec)) {
            return false;
        }
        this.startTimeSec = startTimeSec;
        return true;
    }

    boolean addOperationName(String operationName) {
        if (this.operationName != null) {
            return false;
        }
        this.operationName = operationName;
        return true;
    }

    boolean addReachingTimeNs(long reachingTimeNs) {
        assert reachingTimeNs >= 0L;
        if (this.reachingTimeNs != TimeUtils.NO_TIME) {
            return false;
        }
        this.reachingTimeNs = reachingTimeNs;
        return true;
    }

    boolean addCleanupTimeNs(long cleanupTimeNs) {
        assert cleanupTimeNs >= 0L;
        if (this.cleanupTimeNs != TimeUtils.NO_TIME) {
            return false;
        }
        this.cleanupTimeNs = cleanupTimeNs;
        return true;
    }

    boolean addInsideTimeNs(long insideTimeNs) {
        assert insideTimeNs >= 0L;
        if (this.insideTimeNs != TimeUtils.NO_TIME) {
            return false;
        }
        this.insideTimeNs = insideTimeNs;
        return true;
    }

    boolean addLeavingTimeNs(long leavingTimeNs) {
        assert leavingTimeNs >= 0L;
        if (this.leavingTimeNs != TimeUtils.NO_TIME) {
            return false;
        }
        this.leavingTimeNs = leavingTimeNs;
        return true;
    }

    boolean addTotalTimeNs(long totalTimeNs) {
        assert totalTimeNs >= 0L;
        if (this.totalTimeNs != TimeUtils.NO_TIME) {
            return false;
        }
        this.totalTimeNs = totalTimeNs;
        return addStartTimeSec(finishTimeSec - totalTimeNs / 1E9);
    }
}
