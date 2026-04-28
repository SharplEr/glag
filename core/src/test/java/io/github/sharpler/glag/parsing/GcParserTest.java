package io.github.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.sharpler.glag.records.GcLogRecord;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.DoubleRange;
import org.junit.jupiter.api.Test;

class GcParserTest {
    @Test
    void parseWithUptimeDecorator() {
        var line = "[2.419s][info][gc] GC(0) Pause Young (G1 Evacuation Pause) 10240M->26M(12288M) 37.309ms";

        assertEquals(new GcLogRecord(line, 2.419, 0), GcParser.parse(line));
    }

    @Test
    void parseWithExtraDecorators() {
        var line = "[2023-09-16T10:27:34.880+0800][0.485s][1234][5678][info][gc,start] GC(0) Garbage Collection (System.gc())";

        assertEquals(new GcLogRecord(line, 0.485, 0), GcParser.parse(line));
    }

    @Test
    void parseWithUptimeMillisDecorator() {
        var line = "[2419ms][1234][info][gc,heap] GC(0) Eden regions: 1->0(5)";

        assertEquals(new GcLogRecord(line, 2.419, 0), GcParser.parse(line));
    }

    @Test
    void parseWithUptimeNanosDecorator() {
        var line = "[2419000000ns][debug][gc,heap] GC(0) Heap after GC invocations=1";

        assertEquals(new GcLogRecord(line, 2.419, 0), GcParser.parse(line));
    }

    @Test
    void parseWithNonTimestampDecoratorEndingInS() {
        var line = "[2026-04-07T15:37:47.079+0000][8.869s][debug][gc,refine,stats] "
            + "GC(0) Mutator refinement: 0.00ms, refined: 0, precleaned: 0, dirtied: 777";

        assertEquals(new GcLogRecord(line, 8.869, 0), GcParser.parse(line));
    }

    @Test
    void parseInterleavedGcOperations() {
        var first = "[0.068s][info][gc] GC(0) Pause Young (Concurrent Start) (G1 Humongous Allocation) 7M->6M(20M) 7.230ms";
        var second = "[0.069s][info][gc] GC(1) Concurrent Cycle";

        assertEquals(new GcLogRecord(first, 0.068, 0), GcParser.parse(first));
        assertEquals(new GcLogRecord(second, 0.069, 1), GcParser.parse(second));
    }

    @Test
    void ignoreLinesWithoutGcId() {
        var line = "[0.068s][info][gc,cpu] User=0.01s Sys=0.01s Real=0.00s";

        assertNull(GcParser.parse(line));
    }

    @Test
    void ignoreLinesWithoutTimestampDecorator() {
        var line = "GC(0) Pause Young (G1 Evacuation Pause) 10240M->26M(12288M) 37.309ms";

        assertNull(GcParser.parse(line));
    }

    @Property
    void parsePrefersMostPreciseTimestampDecorator(
        @ForAll @DoubleRange(min = 0.0, max = 1_000_000.0) double seconds,
        @ForAll("gcNum") int gcNum,
        @ForAll("decoratorCombinations") List<String> decoratorKinds
    ) {
        var decorators = decoratorKinds.stream()
            .map(kind -> switch (kind) {
                case "s" -> "[%.3fs]".formatted(seconds);
                case "ms" -> "[%dms]".formatted(Math.round(seconds * 1_000d));
                case "ns" -> "[%dns]".formatted(Math.round(seconds * 1_000_000_000d));
                default -> throw new IllegalArgumentException(kind);
            })
            .reduce("", String::concat);
        var line = decorators + "[info][gc] GC(%d) Concurrent Mark".formatted(gcNum);

        var expectedTimestamp = decoratorKinds.contains("ns")
            ? Math.round(seconds * 1_000_000_000d) / 1_000_000_000d
            : decoratorKinds.contains("ms")
            ? Math.round(seconds * 1_000d) / 1_000d
            : seconds;

        assertEquals(new GcLogRecord(line, expectedTimestamp, gcNum), GcParser.parse(line));
    }

    @Provide
    Arbitrary<Integer> gcNum() {
        return Arbitraries.integers().between(0, 1_000_000);
    }

    @Provide
    Arbitrary<List<String>> decoratorCombinations() {
        return Combinators.combine(
                Arbitraries.of(false, true),
                Arbitraries.of(false, true),
                Arbitraries.of(false, true)
            )
            .as((seconds, millis, nanos) -> {
                var result = new ArrayList<String>(3);
                if (seconds) {
                    result.add("s");
                }
                if (millis) {
                    result.add("ms");
                }
                if (nanos) {
                    result.add("ns");
                }
                return result;
            })
            .filter(list -> !list.isEmpty())
            .flatMap(Arbitraries::shuffle);
    }
}
