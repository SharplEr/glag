package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.GcName;

/// Correlated runtime events derived from safepoint and GC logs.
///
/// @param gcName detected GC name, or `null` if detection failed
/// @param safepointLog parsed safepoint log with indexes and aggregates
/// @param thresholdMs slow-event threshold in milliseconds
/// @param slowSingleVmOperations slow safepoints that were not matched to a GC iteration
/// @param slowGcs slow GC iterations matched to safepoints
/// @param slowSimultaneousGcs groups of overlapping GC iterations matched to safepoints
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
    /// @param gcLog parsed GC log
    /// @param safepointLog parsed safepoint log
    /// @param thresholdMs slow-event threshold in milliseconds
    /// @return correlated runtime events for reporting
    public static RuntimeEvents create(GcLog gcLog, SafepointLog safepointLog, int thresholdMs) {
        var thresholdNs = TimeUnit.MILLISECONDS.toNanos(thresholdMs);

        var slowGcs = new ArrayList<GcIteration>();
        var slowSingleVmOperations = new HashMap<String, List<SingleVMOperation>>();
        var slowSimultaneousGcs = new ArrayList<SimultaneousGcIterations>();
        for (var safepoint : safepointLog.events().values()) {
            if (safepoint.totalTimeNs() > thresholdNs) {
                var gcs = gcLog.timeIndex().findByRange(safepoint.startTimeSec(), safepoint.finishTimeSec())
                    .stream()
                    .filter(gc -> matchesSafepointDuration(gc, safepoint.totalTimeNs()))
                    .toList();
                if (gcs.isEmpty()) {
                    slowSingleVmOperations
                        .computeIfAbsent(safepoint.operationName(), key -> new ArrayList<>())
                        .add(new SingleVMOperation(safepoint));
                } else if (gcs.size() == 1) {
                    var gc = gcs.getFirst();
                    var safepointsWithGc = safepointLog.events()
                        .findByRange(gc.startTimeSec(), gc.finishTimeSec())
                        .stream()
                        .toList();
                    slowGcs.add(new GcIteration(safepointsWithGc, gc));
                } else {
                    var start = Double.POSITIVE_INFINITY;
                    var finish = Double.NEGATIVE_INFINITY;
                    for (var gc : gcs) {
                        start = Math.min(start, gc.startTimeSec());
                        finish = Math.max(finish, gc.finishTimeSec());
                    }

                    slowSimultaneousGcs.add(new SimultaneousGcIterations(
                        safepointLog.events()
                            .findByRange(start, finish)
                            .stream()
                            .toList(),
                        gcs.stream().sorted(Comparator.comparingInt(GcLogRecords::gcNum)).toList()
                    ));
                }
            }
        }

        return new RuntimeEvents(
            gcLog.gcName(),
            safepointLog,
            thresholdMs,
            slowSingleVmOperations,
            slowGcs,
            slowSimultaneousGcs
        );
    }

    private static boolean matchesSafepointDuration(GcLogRecords gc, long safepointTotalTimeNs) {
        var gcDurationNs = Math.round((gc.finishTimeSec() - gc.startTimeSec()) * 1E9);
        return gcDurationNs + GC_DURATION_MATCHING_TOLERANCE_NS >= safepointTotalTimeNs;
    }
}
