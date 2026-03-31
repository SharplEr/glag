package org.sharpler.glag.util;

import java.util.Locale;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Assertions;

class TimeUtilsTest {
    @Property
    void formatDurationUsesNanosecondsForSubMicrosecondValues(@ForAll @LongRange(min = 0L, max = 999L) long ns) {
        Assertions.assertEquals(expected(ns, 1, "ns"), TimeUtils.formatDuration(ns));
    }

    @Property
    void formatDurationUsesMicrosecondsForSubMillisecondValues(@ForAll @LongRange(min = 1_000L, max = 999_999L) long ns) {
        Assertions.assertEquals(expected(ns, 1_000, "µs"), TimeUtils.formatDuration(ns));
    }

    @Property
    void formatDurationUsesMillisecondsForSubSecondValues(@ForAll @LongRange(min = 1_000_000L, max = 999_999_999L) long ns) {
        Assertions.assertEquals(expected(ns, 1_000_000, "ms"), TimeUtils.formatDuration(ns));
    }

    @Property
    void formatDurationUsesSecondsForSecondAndAboveValues(@ForAll @LongRange(min = 1_000_000_000L, max = Long.MAX_VALUE) long ns) {
        Assertions.assertEquals(expected(ns, 1_000_000_000, "s"), TimeUtils.formatDuration(ns));
    }

    private static String expected(long nanoseconds, long unitSizeInNanoseconds, String unitName) {
        var value = (double) nanoseconds / unitSizeInNanoseconds;
        return String.format(Locale.US, "%.2f (%s)", value, unitName);
    }
}
