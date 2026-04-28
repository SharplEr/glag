package io.github.sharpler.glag.distribution;

import io.github.sharpler.glag.records.SafepointLogRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/// Builds cumulative distributions for safepoint timing metrics.
public final class CumulativeDistributionBuilder {
    private final int size;
    private final ArrayList<CumulativeDistributionPoint> points = new ArrayList<>();
    private int current = 0;

    /// Builds a cumulative distribution for an arbitrary safepoint metric.
    ///
    /// @param events source safepoint events
    /// @param metric extracts the metric value from each event
    /// @return cumulative distribution of the extracted metric
    public static List<CumulativeDistributionPoint> distribution(
        List<SafepointLogRecord> events,
        ToLongFunction<SafepointLogRecord> metric
    ) {
        var builder = new CumulativeDistributionBuilder(events.size());
        events.stream()
            .mapToLong(metric)
            .sorted()
            .forEach(builder::addValue);
        return builder.build();
    }

    private CumulativeDistributionBuilder(int size) {
        this.size = size;
    }

    private void addValue(long value) {
        if (current == size) {
            throw new IllegalStateException("All values already processed: size=" + size);
        }
        current++;
        var prob = (double) current / size;
        if (points.isEmpty()) {
            points.add(new CumulativeDistributionPoint(value, prob));
            return;
        }

        var lastPoint = points.getLast();
        if (roundCompare(value, lastPoint.value())) {
            points.removeLast();
            points.add(new CumulativeDistributionPoint(value, prob));
        } else if (current == size || prob - lastPoint.prob() >= 0.1 || value > 2 * lastPoint.value()) {
            points.add(new CumulativeDistributionPoint(value, prob));
        }
    }

    private static boolean roundCompare(long a, long b) {
        return round(a) == round(b);
    }

    static long round(long value) {
        return value / 1000 + (value % 1000 >= 500 ? 1 : 0);
    }

    private List<CumulativeDistributionPoint> build() {
        return points;
    }
}
