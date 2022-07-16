package org.sharpler.glag.distribution;

import java.util.ArrayList;
import java.util.List;
import org.sharpler.glag.aggregations.SafepointLog;
import org.sharpler.glag.records.SafepointLogRecord;

public final class CumulativeDistributionBuilder {
    private final int size;
    private final ArrayList<CumulativeDistributionPoint> points = new ArrayList<>();
    private int current = 0;

    public CumulativeDistributionBuilder(int size) {
        this.size = size;
    }

    public void addValue(long value) {
        if (current == size) {
            throw new IllegalStateException("All values already processed: size=" + size);
        }
        current++;
        if (current == 1) {
            points.add(new CumulativeDistributionPoint(value, 0d));
        } else if (current == size) {
            points.add(new CumulativeDistributionPoint(value, 1d));
        } else {
            var lastPoint = points.get(points.size() - 1);
            var prob = (double) current / size;

            if (prob - lastPoint.prob() >= 0.1 || value > 2 * lastPoint.value()) {
                points.add(new CumulativeDistributionPoint(value, prob));
            }
        }
    }

    public List<CumulativeDistributionPoint> build() {
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
