package io.github.sharpler.glag.index;

import io.github.sharpler.glag.util.TimeUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/// Immutable index for values that occupy a time range.
///
/// @param <T> indexed value type
public final class RangeIndex<T extends WithTimeRange> {
    private final List<T> valuesByStart;
    private final double[] prefixMaxFinish;

    /// Builds an index over the provided values.
    ///
    /// @param values values to index by their time range
    public RangeIndex(Collection<T> values) {
        valuesByStart = values.stream()
            .sorted(Comparator.comparingDouble(WithTimeRange::startTimeSec))
            .toList();

        prefixMaxFinish = new double[valuesByStart.size()];
        var maxFinish = Double.NEGATIVE_INFINITY;
        for (var i = 0; i < valuesByStart.size(); i++) {
            maxFinish = Math.max(maxFinish, valuesByStart.get(i).finishTimeSec());
            prefixMaxFinish[i] = maxFinish;
        }
    }

    /// Returns indexed values sorted by `startTimeSec`.
    ///
    /// @return indexed values sorted by start time
    public List<T> values() {
        return valuesByStart;
    }

    /// Returns all indexed values whose time range overlaps `[start, finish]`.
    ///
    /// @param start query start time in seconds
    /// @param finish query finish time in seconds
    /// @return overlapping indexed values
    public List<T> findByRange(double start, double finish) {
        var toExclusive = firstIndexWithStartGreaterThan(finish);
        if (toExclusive == 0) {
            return List.of();
        }

        var fromInclusive = firstIndexWithPrefixMaxFinishAtLeast(start);
        if (fromInclusive >= toExclusive) {
            return List.of();
        }

        var result = new ArrayList<T>();
        for (var i = fromInclusive; i < toExclusive; i++) {
            var value = valuesByStart.get(i);
            if (TimeUtils.match(start, finish, value.startTimeSec(), value.finishTimeSec())) {
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
            if (valuesByStart.get(mid).startTimeSec() > finish) {
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
