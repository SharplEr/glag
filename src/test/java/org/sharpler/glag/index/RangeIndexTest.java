package org.sharpler.glag.index;

import java.util.Comparator;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Assertions;
import org.sharpler.glag.util.TimeUtils;

class RangeIndexTest {
    @Property
    void findByRangeMatchesSortedFullScan(
        @ForAll("ranges") List<ValueWithRange<Integer>> values,
        @ForAll("queries") Query query
    ) {
        var index = new RangeIndex<>(values);

        var expected = values.stream()
            .sorted(Comparator.comparingDouble(ValueWithRange::start))
            .filter(value -> TimeUtils.match(query.start(), query.finish(), value.start(), value.finish()))
            .toList();

        Assertions.assertEquals(expected, index.findByRange(query.start(), query.finish()));
    }

    @Provide
    Arbitrary<List<ValueWithRange<Integer>>> ranges() {
        return range().list().ofMaxSize(32);
    }

    @Provide
    Arbitrary<Query> queries() {
        return Combinators.combine(
            Arbitraries.doubles().between(-1000.0, 1000.0),
            Arbitraries.doubles().between(0.01, 1000.0)
        ).as((start, length) -> new Query(start, start + length));
    }

    private Arbitrary<ValueWithRange<Integer>> range() {
        return Combinators.combine(
            Arbitraries.integers().between(Integer.MIN_VALUE, Integer.MAX_VALUE),
            Arbitraries.doubles().between(-1000.0, 1000.0),
            Arbitraries.doubles().between(0.01, 1000.0)
        ).as((value, start, length) -> new ValueWithRange<>(value, start, start + length));
    }

    private record Query(double start, double finish) {
    }
}
