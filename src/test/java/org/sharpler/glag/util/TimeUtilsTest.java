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
                intervals.firstStart(),
                intervals.firstFinish(),
                intervals.secondStart(),
                intervals.secondFinish()
            )
        );
    }

    @Property
    void matchReturnsFalseForDisjointIntervals(@ForAll("disjointIntervals") IntervalPair intervals) {
        Assertions.assertFalse(
            TimeUtils.match(
                intervals.firstStart(),
                intervals.firstFinish(),
                intervals.secondStart(),
                intervals.secondFinish()
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
        ).as((firstStart, firstLength, overlapOffset, secondTailLength) -> {
            var firstFinish = firstStart + firstLength;
            var boundedOverlapOffset = Math.min(overlapOffset, firstLength - 1);
            var secondStart = firstStart + boundedOverlapOffset;
            var secondFinish = firstFinish + secondTailLength;
            return new IntervalPair(firstStart, firstFinish, secondStart, secondFinish);
        });
    }

    @Provide
    Arbitrary<IntervalPair> disjointIntervals() {
        return Combinators.combine(
            Arbitraries.integers().between(-1_000_000, 1_000_000),
            Arbitraries.integers().between(1, 10_000),
            Arbitraries.integers().between(1, 10_000),
            Arbitraries.integers().between(1, 10_000)
        ).as((firstStart, firstLength, gap, secondLength) -> {
            var firstFinish = firstStart + firstLength;
            var secondStart = firstFinish + gap;
            var secondFinish = secondStart + secondLength;
            return new IntervalPair(firstStart, firstFinish, secondStart, secondFinish);
        });
    }

    private record IntervalPair(double firstStart, double firstFinish, double secondStart, double secondFinish) {
    }
}
