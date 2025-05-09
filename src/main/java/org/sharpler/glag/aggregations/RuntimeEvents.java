package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.sharpler.glag.index.ValueWithRange;
import org.sharpler.glag.records.GcLogRecord;
import org.sharpler.glag.records.GcName;
import org.sharpler.glag.records.RuntimeEvent;

public record RuntimeEvents(
    @Nullable
    GcName gcName,
    SafepointLog safepointLog,
    int thresholdMs,
    List<RuntimeEvent.GcIteration> slowGcs,
    Map<String, List<RuntimeEvent.SingleVMOperation>> slowSingleVmOperations
) {
    public static RuntimeEvents create(GcLog gcLog, SafepointLog safepointLog, int thresholdMs) {
        var thresholdNs = TimeUnit.MILLISECONDS.toNanos(thresholdMs);

        var slowGcs = new HashMap<Integer, RuntimeEvent.GcIteration>();
        var slowSingleVmOperations = new HashMap<String, List<RuntimeEvent.SingleVMOperation>>();

        for (var safepoint : safepointLog.events()) {
            if (safepoint.totalTimeNs() > thresholdNs) {
                var gcs = gcLog.timeIndex().findByRange(safepoint.startTimeSec(), safepoint.finishTimeSec());
                if (gcs.isEmpty()) {
                    slowSingleVmOperations
                        .computeIfAbsent(safepoint.operationName(), key -> new ArrayList<>())
                        .add(new RuntimeEvent.SingleVMOperation(safepoint));
                } else if (gcs.size() == 1) {
                    var gc = gcs.get(0);
                    var gcNum = gc.value().get(0).gcNum();
                    if (slowGcs.containsKey(gcNum)) {
                        continue;
                    }

                    var safepointsWithGc = safepointLog.timeIndex()
                        .findByRange(gc.start(), gc.finish())
                        .stream()
                        .map(ValueWithRange::value)
                        .toList();

                    slowGcs.put(gcNum, new RuntimeEvent.GcIteration(safepointsWithGc, gc.value()));
                } else {
                    System.err.printf(
                        "More than one gc at the same time: start = %f (s), finish = %f (s), GCs = %s %n",
                        safepoint.startTimeSec(),
                        safepoint.finishTimeSec(),
                        gcs.stream()
                            .map(x ->
                                "num = %s, start = %f (s) finish = %f (s)".formatted(
                                    Arrays.toString(x.value().stream().mapToInt(GcLogRecord::gcNum).sorted().distinct().toArray()),
                                    x.start(),
                                    x.finish()
                                )
                            )
                            .toList()
                    );
                }
            }
        }

        return new RuntimeEvents(gcLog.gcName(), safepointLog, thresholdMs, List.copyOf(slowGcs.values()), slowSingleVmOperations);
    }
}
