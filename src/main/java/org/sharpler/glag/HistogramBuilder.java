package org.sharpler.glag;

import java.util.ArrayList;
import java.util.List;

public final class HistogramBuilder {
    private final int size;
    private final ArrayList<HistogramPoint> points = new ArrayList<>();
    private int current = 0;


    public HistogramBuilder(int size) {
        this.size = size;
    }

    public void addValue(long value) {
        current++;
        if (current == 1) {
            points.add(new HistogramPoint(value, 0d));
        } else if (current == size) {
            points.add(new HistogramPoint(value, 1d));
        } else {
            var lastPoint = points.get(points.size() - 1);
            var prob = (double) current / size;

            if (prob - lastPoint.prob() >= 0.1 || value > 2 * lastPoint.value()) {
                points.add(new HistogramPoint(value, prob));
            }
        }
    }

    public List<HistogramPoint> build() {
        return points;
    }
}
