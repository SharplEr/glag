package org.sharpler.glag.distribution;

import java.util.ArrayList;
import java.util.List;

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
}
