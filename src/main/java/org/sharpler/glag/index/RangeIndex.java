package org.sharpler.glag.index;

import java.util.List;

public record RangeIndex<T>(List<ValueWithRange<T>> values) {

    public List<ValueWithRange<T>> findByRange(Range range) {
        return values.stream()
            .filter(range::match)
            .toList();
    }

    public List<ValueWithRange<T>> findByRange(double start, double finish) {
        return findByRange(new Range(start, finish));
    }
}
