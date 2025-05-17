package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.sharpler.glag.index.ValueWithRange;
import org.sharpler.glag.records.GcIteration;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.GcName;
import org.sharpler.glag.records.SimultaneousGcIterations;
import org.sharpler.glag.records.SingleVMOperation;

public record RuntimeEvents(
    @Nullable
    GcName gcName,
    SafepointLog safepointLog,
    int thresholdMs,
    Map<String, List<SingleVMOperation>> slowSingleVmOperations,
    List<GcIteration> slowGcs,
    List<SimultaneousGcIterations> slowSimultaneousGcs
) {
    public static RuntimeEvents create(GcLog gcLog, SafepointLog safepointLog, int thresholdMs) {
        var thresholdNs = TimeUnit.MILLISECONDS.toNanos(thresholdMs);

        var slowGcs = new ArrayList<GcIteration>();
        var slowSingleVmOperations = new HashMap<String, List<SingleVMOperation>>();
        var slowSimultaneousGcs = new ArrayList<SimultaneousGcIterations>();
        for (var safepoint : safepointLog.events()) {
            if (safepoint.totalTimeNs() > thresholdNs) {
                var gcs = gcLog.timeIndex().findByRange(safepoint.startTimeSec(), safepoint.finishTimeSec());
                if (gcs.isEmpty()) {
                    slowSingleVmOperations
                        .computeIfAbsent(safepoint.operationName(), key -> new ArrayList<>())
                        .add(new SingleVMOperation(safepoint));
                } else if (gcs.size() == 1) {
                    var gc = gcs.getFirst();
                    var safepointsWithGc = safepointLog.timeIndex()
                        .findByRange(gc.start(), gc.finish())
                        .stream()
                        .map(ValueWithRange::value)
                        .toList();
                    slowGcs.add(new GcIteration(safepointsWithGc, gc.value()));
                } else {
                    var start = Double.POSITIVE_INFINITY;
                    var finish = Double.NEGATIVE_INFINITY;
                    for (var gc : gcs) {
                        start = Math.min(start, gc.start());
                        finish = Math.max(finish, gc.finish());
                    }

                    slowSimultaneousGcs.add(new SimultaneousGcIterations(
                        safepointLog.timeIndex()
                            .findByRange(start, finish)
                            .stream()
                            .map(ValueWithRange::value)
                            .toList(),
                        gcs.stream().map(ValueWithRange::value).sorted(Comparator.comparingInt(GcLogRecords::gcNum)).toList()
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
}
