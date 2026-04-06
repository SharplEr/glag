package org.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sharpler.glag.records.SafepointLogRecord;

class SafepointParserTest {
    private static final double TOTAL_TIME_SEC = 207779 / 1E9;

    @Test
    void parse() {
        var line =
            "[2024-12-16T07:05:07.368+0000][3.412s][info][safepoint] " +
                "Safepoint \"ICBufferFull\", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, " +
                "At safepoint: 8449 ns, Total: 207779 ns";

        var result = SafepointParser.parse(line);

        assertEquals(
            new SafepointLogRecord(
                3.412 - 207779 / 1E9,
                3.412,
                line,
                "ICBufferFull",
                69282L,
                130048L,
                8449L,
                SafepointLogRecord.NO_TIME,
                207779L
            ),
            result
        );
    }

    @Test
    void parseWithoutWallClockDecorator() {
        var line =
            "[3.412s][info][safepoint] " +
                "Safepoint \"ICBufferFull\", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, " +
                "At safepoint: 8449 ns, Total: 207779 ns";

        var result = SafepointParser.parse(line);

        assertEquals(
            new SafepointLogRecord(
                3.412 - 207779 / 1E9,
                3.412,
                line,
                "ICBufferFull",
                69282L,
                130048L,
                8449L,
                SafepointLogRecord.NO_TIME,
                207779L
            ),
            result
        );
    }

    @Test
    void parseWithUptimeMillisDecorator() {
        var line =
            "[3412ms][info][safepoint] " +
                "Safepoint \"ICBufferFull\", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, " +
                "At safepoint: 8449 ns, Total: 207779 ns";

        var result = SafepointParser.parse(line);

        assertEquals(
            new SafepointLogRecord(
                3.412 - 207779 / 1E9,
                3.412,
                line,
                "ICBufferFull",
                69282L,
                130048L,
                8449L,
                SafepointLogRecord.NO_TIME,
                207779L
            ),
            result
        );
    }

    @Test
    void parseWithUptimeNanosDecorator() {
        var line =
            "[3412000000ns][info][safepoint] " +
                "Safepoint \"ICBufferFull\", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, " +
                "At safepoint: 8449 ns, Total: 207779 ns";

        var result = SafepointParser.parse(line);

        assertEquals(
            new SafepointLogRecord(
                3.412 - 207779 / 1E9,
                3.412,
                line,
                "ICBufferFull",
                69282L,
                130048L,
                8449L,
                SafepointLogRecord.NO_TIME,
                207779L
            ),
            result
        );
    }

    @Property
    void parsePrefersMostPreciseTimestampDecorator(
        @ForAll("validFinishTimesNs") long finishTimeNs,
        @ForAll("decoratorCombinations") List<String> decoratorKinds
    ) {
        var seconds = finishTimeNs / 1_000_000_000d;
        var decorators = decoratorKinds.stream()
            .map(kind -> switch (kind) {
                case "s" -> "[%.3fs]".formatted(seconds);
                case "ms" -> "[%dms]".formatted(Math.round(seconds * 1_000d));
                case "ns" -> "[%dns]".formatted(Math.round(seconds * 1_000_000_000d));
                default -> throw new IllegalArgumentException(kind);
            })
            .reduce("", String::concat);
        var line = decorators
            + "[info][safepoint] Safepoint \"ICBufferFull\", Time since last: 177611286 ns, "
            + "Reaching safepoint: 69282 ns, Cleanup: 130048 ns, "
            + "At safepoint: 8449 ns, Total: 207779 ns";

        var expectedFinishTime = decoratorKinds.contains("ns")
            ? Math.round(seconds * 1_000_000_000d) / 1_000_000_000d
            : Math.round(seconds * 1_000d) / 1_000d;

        assertEquals(
            new SafepointLogRecord(
                expectedFinishTime - TOTAL_TIME_SEC,
                expectedFinishTime,
                line,
                "ICBufferFull",
                69282L,
                130048L,
                8449L,
                SafepointLogRecord.NO_TIME,
                207779L
            ),
            SafepointParser.parse(line)
        );
    }

    @Test
    void parseWithLeavingSafepoint() {
        var line =
            "[3.412s][info][safepoint] " +
                "Safepoint \"Cleanup\", Time since last: 177611286 ns, Reaching safepoint: 69282 ns, Cleanup: 130048 ns, " +
                "At safepoint: 8449 ns, Leaving safepoint: 20000 ns, Total: 227779 ns";

        var result = SafepointParser.parse(line);

        assertEquals(
            new SafepointLogRecord(
                3.412 - 227779 / 1E9,
                3.412,
                line,
                "Cleanup",
                69282L,
                130048L,
                8449L,
                20000L,
                227779L
            ),
            result
        );
    }

    @Test
    void parseIgnoresUnknownValueTypes() {
        var line =
            "[3.412s][info][safepoint] " +
                "Safepoint \"ICBufferFull\", Time since last: 177611286 ns, Unknown metric: 42 ns, " +
                "Reaching safepoint: 69282 ns, Cleanup: 130048 ns, At safepoint: 8449 ns, Total: 207779 ns";

        var result = SafepointParser.parse(line);

        assertEquals(
            new SafepointLogRecord(
                3.412 - 207779 / 1E9,
                3.412,
                line,
                "ICBufferFull",
                69282L,
                130048L,
                8449L,
                SafepointLogRecord.NO_TIME,
                207779L
            ),
            result
        );
    }

    @Provide
    Arbitrary<Long> validFinishTimesNs() {
        return Arbitraries.longs().between(1_000_000L, 1_000_000_000_000_000L);
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
