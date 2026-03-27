package org.sharpler.glag.distribution;

import java.util.ArrayList;
import java.util.List;
import org.sharpler.glag.aggregations.SafepointLog;
import org.sharpler.glag.records.SafepointLogRecord;

public final class CumulativeDistributionBuilder {
    private final int size;
    private final ArrayList<CumulativeDistributionPoint> points = new ArrayList<>();
    private int current = 0;

    private CumulativeDistributionBuilder(int size) {
        this.size = size;
    }

    private void addValue(long value) {
        if (current == size) {
            throw new IllegalStateException("All values already processed: size=" + size);
        }
        current++;
        if (current == size) {
            points.add(new CumulativeDistributionPoint(value, 1d));
        } else if (current == 1) {
            points.add(new CumulativeDistributionPoint(value, 1d / size));
        } else {
            var lastPoint = points.getLast();
            var prob = (double) current / size;

            if (roundCompare(value, lastPoint.value())) {
                points.removeLast();
                points.add(new CumulativeDistributionPoint(value, prob));
            } else if (prob - lastPoint.prob() >= 0.1 || value > 2 * lastPoint.value()) {
                points.add(new CumulativeDistributionPoint(value, prob));
            }
        }
    }

    private static boolean roundCompare(long a, long b) {
        return round(a) == round(b);
    }

    private static long round(long value) {
        return value / 1000 + (value % 1000 >= 500 ? 1 : 0);
    }

    private List<CumulativeDistributionPoint> build() {
        return points;
    }

    public static List<CumulativeDistributionPoint> operationTimeDistribution(List<SafepointLogRecord> events) {
        var builder = new CumulativeDistributionBuilder(events.size());
        events.forEach(x -> builder.addValue(x.insideTimeNs()));
        return builder.build();
    }

    public static List<CumulativeDistributionPoint> reachingDistribution(SafepointLog safepoints) {
        var builder = new CumulativeDistributionBuilder(safepoints.events().size());

        safepoints.events().stream()
            .mapToLong(SafepointLogRecord::reachingTimeNs)
            .sorted()
            .forEach(builder::addValue);

        return builder.build();
    }
}
