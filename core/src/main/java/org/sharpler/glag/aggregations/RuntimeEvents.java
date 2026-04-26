package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.GcName;
import org.sharpler.glag.records.SafepointLogRecord;

/// Correlated runtime events derived from safepoint and GC logs.
///
/// @param gcName                 detected GC name, or `null` if detection failed
/// @param safepointLog           parsed safepoint log with indexes and aggregates
/// @param thresholdMs            slow-event threshold in milliseconds
/// @param slowSingleVmOperations slow safepoints that were not matched to a GC iteration
/// @param slowGcs                slow GC iterations matched to safepoints
/// @param slowSimultaneousGcs    groups of overlapping GC iterations matched to safepoints
public record RuntimeEvents(
    @Nullable
    GcName gcName,
    SafepointLog safepointLog,
    int thresholdMs,
    Map<String, List<SingleVMOperation>> slowSingleVmOperations,
    List<GcIteration> slowGcs,
    List<SimultaneousGcIterations> slowSimultaneousGcs
) {
    private static final long GC_DURATION_MATCHING_TOLERANCE_NS = TimeUnit.MILLISECONDS.toNanos(50);

    /// Correlates a parsed GC log with a parsed safepoint log.
    ///
    /// @param gcLog        parsed GC log
    /// @param safepointLog parsed safepoint log
    /// @param thresholdMs  slow-event threshold in milliseconds
    /// @return correlated runtime events for reporting
    public static RuntimeEvents create(GcLog gcLog, SafepointLog safepointLog, int thresholdMs) {
        var thresholdNs = TimeUnit.MILLISECONDS.toNanos(thresholdMs);

        var singleVmOperationsCollector = new SingleVmOperationsCollector(new HashMap<>());
        var gcIterationCollector = new GcIterationCollector(new ArrayList<>(), safepointLog.events());
        var simultaneousGcCollector = new SimultaneousGcCollector(new ArrayList<>(), safepointLog.events());
        for (var safepoint : safepointLog.events().values()) {
            if (safepoint.totalTimeNs() > thresholdNs) {
                var gcs = gcLog.timeIndex().findByRange(safepoint.startTimeSec(), safepoint.finishTimeSec())
                    .stream()
                    .filter(gc -> matchesSafepointDuration(gc, safepoint.totalTimeNs()))
                    .toList();
                if (gcs.isEmpty()) {
                    singleVmOperationsCollector.add(safepoint);
                } else if (gcs.size() == 1) {
                    gcIterationCollector.add(gcs.getFirst());
                } else {
                    simultaneousGcCollector.add(gcs);
                }
            }
        }

        return new RuntimeEvents(
            gcLog.gcName(),
            safepointLog,
            thresholdMs,
            singleVmOperationsCollector.slowSingleVmOperations(),
            gcIterationCollector.slowGcs(),
            simultaneousGcCollector.slowSimultaneousGcs()
        );
    }

    private static boolean matchesSafepointDuration(GcLogRecords gc, long safepointTotalTimeNs) {
        var gcDurationNs = Math.round((gc.finishTimeSec() - gc.startTimeSec()) * 1E9);
        return gcDurationNs + GC_DURATION_MATCHING_TOLERANCE_NS >= safepointTotalTimeNs;
    }

    private record SingleVmOperationsCollector(Map<String, List<SingleVMOperation>> slowSingleVmOperations) {
        private void add(SafepointLogRecord safepoint) {
            slowSingleVmOperations
                .computeIfAbsent(safepoint.operationName(), key -> new ArrayList<>())
                .add(new SingleVMOperation(safepoint));
        }
    }

    private record GcIterationCollector(List<GcIteration> slowGcs, RangeIndex<SafepointLogRecord> safepointEvents) {
        private void add(GcLogRecords gc) {
            var safepointsWithGc = safepointEvents.findByRange(gc.startTimeSec(), gc.finishTimeSec())
                .stream()
                .toList();
            slowGcs.add(new GcIteration(safepointsWithGc, gc));
        }
    }

    private record SimultaneousGcCollector(
        List<SimultaneousGcIterations> slowSimultaneousGcs,
        RangeIndex<SafepointLogRecord> safepointEvents
    ) {
        private void add(List<GcLogRecords> gcs) {
            var start = Double.POSITIVE_INFINITY;
            var finish = Double.NEGATIVE_INFINITY;
            for (var gc : gcs) {
                start = Math.min(start, gc.startTimeSec());
                finish = Math.max(finish, gc.finishTimeSec());
            }

            slowSimultaneousGcs.add(new SimultaneousGcIterations(
                safepointEvents.findByRange(start, finish)
                    .stream()
                    .toList(),
                gcs.stream().sorted(Comparator.comparingInt(GcLogRecords::gcNum)).toList()
            ));
        }
    }
}
