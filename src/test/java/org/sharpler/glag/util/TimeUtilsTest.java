package org.sharpler.glag.util;

import java.util.Locale;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
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

    @Property
    void matchReturnsTrueForOverlappingIntervals(@ForAll("overlappingIntervals") IntervalPair intervals) {
        Assertions.assertTrue(
            TimeUtils.match(
                intervals.xStart(),
                intervals.xFinish(),
                intervals.yStart(),
                intervals.yFinish()
            )
        );
    }

    @Property
    void matchReturnsFalseForDisjointIntervals(@ForAll("disjointIntervals") IntervalPair intervals) {
        Assertions.assertFalse(
            TimeUtils.match(
                intervals.xStart(),
                intervals.xFinish(),
                intervals.yStart(),
                intervals.yFinish()
            )
        );
    }

    private static String expected(long nanoseconds, long unitSizeInNanoseconds, String unitName) {
        var value = (double) nanoseconds / unitSizeInNanoseconds;
        return String.format(Locale.US, "%.2f (%s)", value, unitName);
    }

    @Provide
    Arbitrary<IntervalPair> overlappingIntervals() {
        return Combinators.combine(
            Arbitraries.integers().between(-1_000_000, 1_000_000),
            Arbitraries.integers().between(2, 10_000),
            Arbitraries.integers().between(1, 9_999),
            Arbitraries.integers().between(1, 10_000)
        ).as((xStart, xLength, overlapOffset, yTailLength) -> {
            var xFinish = xStart + xLength;
            var boundedOverlapOffset = Math.min(overlapOffset, xLength - 1);
            var yStart = xStart + boundedOverlapOffset;
            var yFinish = xFinish + yTailLength;
            return new IntervalPair(xStart, xFinish, yStart, yFinish);
        });
    }

    @Provide
    Arbitrary<IntervalPair> disjointIntervals() {
        return Combinators.combine(
            Arbitraries.integers().between(-1_000_000, 1_000_000),
            Arbitraries.integers().between(1, 10_000),
            Arbitraries.integers().between(1, 10_000),
            Arbitraries.integers().between(1, 10_000)
        ).as((xStart, xLength, gap, yLength) -> {
            var xFinish = xStart + xLength;
            var yStart = xFinish + gap;
            var yFinish = yStart + yLength;
            return new IntervalPair(xStart, xFinish, yStart, yFinish);
        });
    }

    private record IntervalPair(double xStart, double xFinish, double yStart, double yFinish) {
    }
}
