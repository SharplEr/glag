package org.sharpler.glag.parsing;

import static org.sharpler.glag.parsing.SafepointValueType.AT_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.CLEANUP;
import static org.sharpler.glag.parsing.SafepointValueType.REACHING_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.SAFEPOINT_NAME;
import static org.sharpler.glag.parsing.SafepointValueType.TIME_SINCE_LAST;
import static org.sharpler.glag.parsing.SafepointValueType.TOTAL;

import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.records.SafepointLogRecord;

final class SafepointRecordBuilder {
    private final String origin;

    private double startTimeSec = Double.NaN;
    private double finishTimeSec = Double.NaN;
    private @Nullable String operationName = null;
    private long reachingTimeNs = SafepointLogRecord.NO_TIME;
    private long cleanupTimeNs = SafepointLogRecord.NO_TIME;
    private long insideTimeNs = SafepointLogRecord.NO_TIME;
    private long totalTimeNs = SafepointLogRecord.NO_TIME;

    SafepointRecordBuilder(String origin) {
        this.origin = origin;
    }

    void addFinishTimeSec(double finishTimeSec) {
        assert Double.isNaN(this.finishTimeSec);
        assert !Double.isNaN(finishTimeSec);
        this.finishTimeSec = finishTimeSec;
    }

    void addValue(SafepointValueType type, String line, int start, int end) {
        switch (type) {
            case SAFEPOINT_NAME -> addOperationName(SAFEPOINT_NAME.parseString(line, start, end));
            case TIME_SINCE_LAST -> TIME_SINCE_LAST.parseLong(line, start, end);
            case REACHING_SAFEPOINT -> addReachingTimeNs(REACHING_SAFEPOINT.parseLong(line, start, end));
            case CLEANUP -> addCleanupTimeNs(CLEANUP.parseLong(line, start, end));
            case AT_SAFEPOINT -> addInsideTimeNs(AT_SAFEPOINT.parseLong(line, start, end));
            case TOTAL -> addTotalTimeNs(TOTAL.parseLong(line, start, end));
        }
    }

    SafepointLogRecord build() {
        assert !Double.isNaN(startTimeSec);
        assert !Double.isNaN(finishTimeSec);
        assert operationName != null;
        assert totalTimeNs >= 0L;
        var builtOperationName = Objects.requireNonNull(operationName);
        return new SafepointLogRecord(
            startTimeSec,
            finishTimeSec,
            origin,
            builtOperationName,
            reachingTimeNs,
            insideTimeNs,
            totalTimeNs
        );
    }

    private void addStartTimeSec(double startTimeSec) {
        assert Double.isNaN(this.startTimeSec);
        assert !Double.isNaN(startTimeSec);
        this.startTimeSec = startTimeSec;
    }

    private void addOperationName(String operationName) {
        assert this.operationName == null;
        this.operationName = operationName;
    }

    private void addReachingTimeNs(long reachingTimeNs) {
        assert this.reachingTimeNs == SafepointLogRecord.NO_TIME;
        assert reachingTimeNs >= 0L;
        this.reachingTimeNs = reachingTimeNs;
    }

    private void addCleanupTimeNs(long cleanupTimeNs) {
        assert this.cleanupTimeNs == SafepointLogRecord.NO_TIME;
        assert cleanupTimeNs >= 0L;
        this.cleanupTimeNs = cleanupTimeNs;
    }

    private void addInsideTimeNs(long insideTimeNs) {
        assert this.insideTimeNs == SafepointLogRecord.NO_TIME;
        assert insideTimeNs >= 0L;
        this.insideTimeNs = insideTimeNs;
    }

    private void addTotalTimeNs(long totalTimeNs) {
        assert this.totalTimeNs == SafepointLogRecord.NO_TIME;
        assert totalTimeNs >= 0L;
        this.totalTimeNs = totalTimeNs;
        addStartTimeSec(finishTimeSec - totalTimeNs / 1E9);
    }
}
