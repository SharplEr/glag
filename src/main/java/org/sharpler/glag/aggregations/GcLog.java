package org.sharpler.glag.aggregations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.sharpler.glag.records.GcEvent;
import org.sharpler.glag.records.GcName;
import org.sharpler.glag.records.GcTime;

public record GcLog(
    @Nullable GcName gcName,
    Map<Integer, List<GcEvent>> events,
    List<GcTime> times,
    double startLogSec,
    double finishLogSec
) {

    public List<Integer> findGcByTime(double timeSec, double delta) {
        var result = new ArrayList<Integer>();
        var lowBound = timeSec - delta;
        var upperBound = timeSec + delta;
        for (var time : times) {
            if (match(lowBound, upperBound, time.startSec(), time.finishSec())) {
                result.add(time.gcNum());
            }
        }
        return result;
    }

    private static boolean match(double xStart, double xFinish, double yStart, double yFinish) {
        if (xStart == yStart || xStart == yFinish || yFinish == yStart || yFinish == xFinish) {
            return true;
        }
        if (xStart < yStart) {
            return xFinish > yStart;
        } else {
            return xStart < yFinish;
        }
    }
}
