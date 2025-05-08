package org.sharpler.glag.parsing;

import static org.sharpler.glag.parsing.SafepointValueType.AT_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.CLEANUP;
import static org.sharpler.glag.parsing.SafepointValueType.REACHING_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.SAFEPOINT_NAME;
import static org.sharpler.glag.parsing.SafepointValueType.TIME_SINCE_LAST;
import static org.sharpler.glag.parsing.SafepointValueType.TOTAL;

import javax.annotation.Nullable;
import org.sharpler.glag.records.SafepointLogRecord;

public final class SafepointRecordBuilder {
    private final int line;

    @Nullable
    private String time = null;
    private double startTimeSec = Double.NaN;
    private double finishTimeSec = Double.NaN;
    @Nullable
    private String operationName = null;
    private long reachingTimeNs = -1L;
    private long cleanupTimeNs = -1L;
    private long insideTimeNs = -1L;
    private long totalTimeNs = -1L;

    public SafepointRecordBuilder(int line) {
        assert line >= 0;
        this.line = line;
    }

    public void addTime(String time) {
        assert this.time == null;
        this.time = time;
    }

    private void addStartTimeSec(double startTimeSec) {
        assert Double.isNaN(this.startTimeSec);
        assert !Double.isNaN(startTimeSec);
        this.startTimeSec = startTimeSec;
    }

    public void addFinishTimeSec(double finishTimeSec) {
        assert Double.isNaN(this.finishTimeSec);
        assert !Double.isNaN(finishTimeSec);
        this.finishTimeSec = finishTimeSec;
    }

    public void addOperationName(String operationName) {
        assert this.operationName == null;
        this.operationName = operationName;
    }

    public void addReachingTimeNs(long reachingTimeNs) {
        assert this.reachingTimeNs == -1L;
        assert reachingTimeNs >= 0L;
        this.reachingTimeNs = reachingTimeNs;
    }

    public void addCleanupTimeNs(long cleanupTimeNs) {
        assert this.cleanupTimeNs == -1L;
        assert cleanupTimeNs >= 0L;
        this.cleanupTimeNs = cleanupTimeNs;
    }

    public void addInsideTimeNs(long insideTimeNs) {
        assert this.insideTimeNs == -1L;
        assert insideTimeNs >= 0L;
        this.insideTimeNs = insideTimeNs;
    }

    public void addTotalTimeNs(long totalTimeNs) {
        assert this.totalTimeNs == -1L;
        assert totalTimeNs >= 0L;
        this.totalTimeNs = totalTimeNs;
        addStartTimeSec(finishTimeSec - totalTimeNs / 1E9);
    }

    public void addValue(SafepointValueType type, String line, int start, int end) {
        switch (type) {
            case SAFEPOINT_NAME -> addOperationName(SAFEPOINT_NAME.parseString(line, start, end));
            case TIME_SINCE_LAST -> TIME_SINCE_LAST.parseLong(line, start, end);
            case REACHING_SAFEPOINT -> addReachingTimeNs(REACHING_SAFEPOINT.parseLong(line, start, end));
            case CLEANUP -> addCleanupTimeNs(CLEANUP.parseLong(line, start, end));
            case AT_SAFEPOINT -> addInsideTimeNs(AT_SAFEPOINT.parseLong(line, start, end));
            case TOTAL -> addTotalTimeNs(TOTAL.parseLong(line, start, end));
        }
    }

    public SafepointLogRecord build() {
        assert time != null;
        assert !Double.isNaN(startTimeSec);
        assert !Double.isNaN(finishTimeSec);
        assert operationName != null;
        assert reachingTimeNs >= 0L;
        assert insideTimeNs >= 0L;
        assert totalTimeNs >= 0L;
        return new SafepointLogRecord(
            time,
            startTimeSec,
            finishTimeSec,
            operationName,
            reachingTimeNs,
            insideTimeNs,
            totalTimeNs,
            line
        );
    }
}
