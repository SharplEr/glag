package org.sharpler.glag.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.sharpler.glag.util.TimeUtils;

public final class RangeIndex<T> {
    private final List<ValueWithRange<T>> valuesByStart;
    private final double[] prefixMaxFinish;

    public static <T> RangeIndex<T> create(Collection<? extends T> values, Function<? super T, ValueWithRange<T>> converter) {
        return new RangeIndex<>(values.stream().map(converter).toList());
    }

    RangeIndex(List<ValueWithRange<T>> values) {
        valuesByStart = values.stream()
            .sorted(Comparator.comparingDouble(ValueWithRange::start))
            .toList();

        prefixMaxFinish = new double[valuesByStart.size()];
        var maxFinish = Double.NEGATIVE_INFINITY;
        for (var i = 0; i < valuesByStart.size(); i++) {
            maxFinish = Math.max(maxFinish, valuesByStart.get(i).finish());
            prefixMaxFinish[i] = maxFinish;
        }
    }

    public List<ValueWithRange<T>> findByRange(double start, double finish) {
        var toExclusive = firstIndexWithStartGreaterThan(finish);
        if (toExclusive == 0) {
            return List.of();
        }

        var fromInclusive = firstIndexWithPrefixMaxFinishAtLeast(start);
        if (fromInclusive >= toExclusive) {
            return List.of();
        }

        var result = new ArrayList<ValueWithRange<T>>();
        for (var i = fromInclusive; i < toExclusive; i++) {
            var value = valuesByStart.get(i);
            if (TimeUtils.match(start, finish, value.start(), value.finish())) {
                result.add(value);
            }
        }
        return result;
    }

    private int firstIndexWithStartGreaterThan(double finish) {
        var low = 0;
        var high = valuesByStart.size();
        while (low < high) {
            var mid = (low + high) >>> 1;
            if (valuesByStart.get(mid).start() > finish) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private int firstIndexWithPrefixMaxFinishAtLeast(double start) {
        var low = 0;
        var high = prefixMaxFinish.length;
        while (low < high) {
            var mid = (low + high) >>> 1;
            if (prefixMaxFinish[mid] >= start) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }
}
