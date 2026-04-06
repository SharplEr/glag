package org.sharpler.glag.util;

import java.util.Locale;

public final class TimeUtils {
    private static final long MICROSECOND_IN_NS = 1_000;
    private static final long MILLISECOND_IN_NS = 1_000_000;
    private static final long SECOND_IN_NS = 1_000_000_000;

    private TimeUtils() {
        // No-op.
    }

    public static boolean match(double firstStart, double firstFinish, double secondStart, double secondFinish) {
        return firstStart == secondStart
            || firstStart == secondFinish
            || secondStart == secondFinish
            || secondFinish == firstFinish
            || (firstStart < secondFinish && secondStart < firstFinish);
    }

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
