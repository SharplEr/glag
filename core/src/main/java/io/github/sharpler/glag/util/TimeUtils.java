package io.github.sharpler.glag.util;

import java.util.Locale;

/// Small helpers for time-based comparisons and formatting.
public final class TimeUtils {
    /// Sentinel for an unavailable optional timing value.
    public static final long NO_TIME = -1L;

    private static final long MICROSECOND_IN_NS = 1_000;
    private static final long MILLISECOND_IN_NS = 1_000_000;
    private static final long SECOND_IN_NS = 1_000_000_000;

    private TimeUtils() {
        // No-op.
    }

    /// Returns whether a numeric value can be used as a timestamp or duration.
    ///
    /// @param value time-like value to validate
    /// @return `true` if `value` is finite and non-negative
    public static boolean isTime(double value) {
        return Double.isFinite(value) && value >= 0d;
    }

    /// Returns whether a nanosecond duration is either defined or marked as `NO_TIME`.
    ///
    /// @param timeNs duration in nanoseconds or `NO_TIME`
    /// @return `true` if `timeNs` is non-negative or equal to `NO_TIME`
    public static boolean isOptionalTime(long timeNs) {
        return timeNs >= NO_TIME;
    }

    /// Returns whether two time ranges overlap or touch at their boundaries.
    ///
    /// @param firstStart   start of the first range in seconds
    /// @param firstFinish  finish of the first range in seconds
    /// @param secondStart  start of the second range in seconds
    /// @param secondFinish finish of the second range in seconds
    /// @return `true` if the ranges match or overlap
    public static boolean match(double firstStart, double firstFinish, double secondStart, double secondFinish) {
        return firstStart == secondStart
            || firstStart == secondFinish
            || secondStart == secondFinish
            || secondFinish == firstFinish
            || (firstStart < secondFinish && secondStart < firstFinish);
    }

    /// Formats a duration in nanoseconds using the largest human-readable unit.
    ///
    /// @param nanoseconds duration in nanoseconds
    /// @return formatted duration such as `12.34 (ms)`
    public static String formatDuration(long nanoseconds) {
        assert nanoseconds >= 0 : nanoseconds;

        if (nanoseconds >= SECOND_IN_NS) {
            return formatDuration(nanoseconds, SECOND_IN_NS, "s");
        }
        if (nanoseconds >= MILLISECOND_IN_NS) {
            return formatDuration(nanoseconds, MILLISECOND_IN_NS, "ms");
        }
        if (nanoseconds >= MICROSECOND_IN_NS) {
            return formatDuration(nanoseconds, MICROSECOND_IN_NS, "µs");
        }

        return formatDuration(nanoseconds, 1, "ns");
    }

    private static String formatDuration(long nanoseconds, long unitSizeInNanoseconds, String unitName) {
        var value = (double) nanoseconds / unitSizeInNanoseconds;
        return String.format(Locale.US, "%.2f (%s)", value, unitName);
    }
}
