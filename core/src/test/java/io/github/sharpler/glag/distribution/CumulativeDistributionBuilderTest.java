package io.github.sharpler.glag.distribution;

import static io.github.sharpler.glag.distribution.CumulativeDistributionBuilder.round;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.sharpler.glag.records.SafepointLogRecord;
import io.github.sharpler.glag.util.TimeUtils;
import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

class CumulativeDistributionBuilderTest {
    @Property
    void distributionSatisfiesContract(@ForAll("valueArrays") long[] values) {
        var distribution = CumulativeDistributionBuilder.distribution(events(values), SafepointLogRecord::totalTimeNs);

        assertInvariants(values, distribution);
    }

    @Property
    void distributionCollapsesRoundedEquivalentValues(@ForAll("closeValueArrays") long[] values) {
        var distribution = CumulativeDistributionBuilder.distribution(events(values), SafepointLogRecord::totalTimeNs);

        assertEquals(List.of(new CumulativeDistributionPoint(Arrays.stream(values).max().orElseThrow(), 1d)), distribution);
    }

    @Property
    void distributionAddsPointOnSharpGrowthBeforeTenPercentBoundary(
        @ForAll @LongRange(min = 1_000, max = 1_000_000) long baseline,
        @ForAll @LongRange(min = 0, max = 1_000_000) long tail
    ) {
        var spiked = 2 * baseline + 1 + tail;
        var values = new long[21];
        values[0] = baseline;
        values[1] = spiked;
        for (var i = 0; i < 19; i++) {
            values[i + 2] = spiked;
        }

        var distribution = CumulativeDistributionBuilder.distribution(events(values), SafepointLogRecord::totalTimeNs);

        assertEquals(2, distribution.size());
        assertEquals(new CumulativeDistributionPoint(baseline, 1d / 21d), distribution.getFirst());
        assertEquals(new CumulativeDistributionPoint(spiked, 1d), distribution.getLast());
    }

    @Test
    void distributionIsEmptyForNoEvents() {
        assertEquals(List.of(), CumulativeDistributionBuilder.distribution(List.of(), SafepointLogRecord::totalTimeNs));
    }

    @Provide
    Arbitrary<long[]> valueArrays() {
        return Arbitraries.longs()
            .between(0L, 1_000_000_000_000L)
            .list()
            .ofMinSize(1)
            .ofMaxSize(200)
            .map(list -> list.stream().mapToLong(Long::longValue).toArray());
    }

    @Provide
    Arbitrary<long[]> closeValueArrays() {
        return Arbitraries.longs()
            .between(0L, 499L)
            .list()
            .ofMinSize(2)
            .ofMaxSize(50)
            .map(list -> list.stream().mapToLong(Long::longValue).toArray());
    }

    private static List<SafepointLogRecord> events(long[] values) {
        return Arrays.stream(values).mapToObj(CumulativeDistributionBuilderTest::event).toList();
    }

    private static SafepointLogRecord event(long totalTimeNs) {
        var finishTimeSec = totalTimeNs / 1E9;
        return new SafepointLogRecord(
            0d,
            finishTimeSec,
            "origin",
            "operation",
            TimeUtils.NO_TIME,
            TimeUtils.NO_TIME,
            TimeUtils.NO_TIME,
            TimeUtils.NO_TIME,
            totalTimeNs
        );
    }

    private static void assertInvariants(long[] values, List<CumulativeDistributionPoint> distribution) {
        assertTrue(!distribution.isEmpty());
        assertEquals(1d, distribution.getLast().prob());

        var sorted = Arrays.stream(values).sorted().toArray();
        assertEquals(sorted[sorted.length - 1], distribution.getLast().value());
        assertEquals(sorted[selectedIndex(distribution.getFirst(), sorted.length)], distribution.getFirst().value());

        for (var i = 1; i < distribution.size(); i++) {
            var previous = distribution.get(i - 1);
            var current = distribution.get(i);
            var previousIndex = selectedIndex(previous, sorted.length);
            var currentIndex = selectedIndex(current, sorted.length);

            assertEquals(sorted[currentIndex], current.value());
            assertTrue(previous.value() < current.value());
            assertTrue(previous.prob() < current.prob());
            assertTrue(round(previous.value()) != round(current.value()));

            assertTrue(
                currentIndex + 1 == sorted.length
                    || current.prob() - previous.prob() >= 0.1
                    || current.value() > 2L * previous.value()
            );

            for (var skippedIndex = previousIndex + 1; skippedIndex < currentIndex; skippedIndex++) {
                var skippedValue = sorted[skippedIndex];
                var skippedProb = (double) (skippedIndex + 1) / sorted.length;
                var shouldAppear = round(skippedValue) !=
                    round(previous.value())
                    && round(skippedValue) != round(current.value())
                    && (skippedIndex + 1 == sorted.length
                    || skippedProb - previous.prob() >= 0.1
                    || skippedValue > 2L * previous.value());
                if (shouldAppear) {
                    throw new AssertionError("Skipped required point: value=" + skippedValue + ", prob=" + skippedProb);
                }
            }
        }
    }

    private static int selectedIndex(CumulativeDistributionPoint point, int size) {
        return (int) Math.round(point.prob() * size) - 1;
    }
}
