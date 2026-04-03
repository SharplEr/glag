package org.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.records.SafepointLogRecord;

class SafepointLogTest {
    @ParameterizedTest
    @MethodSource("fixtures")
    void parseAggregatesRealSafepointLogs(SafepointLogFixture fixture) throws IOException {
        var lines = readLines(fixture.resourcePath());
        var expectedEvents = lines.stream().map(SafepointParser::parse).toList();
        var safepointLog = SafepointLog.parse(lines);

        assertAll(
            () -> assertEquals(expectedEvents, safepointLog.events()),
            () -> assertEquals(fixture.expectedOperationCounts(), countsByOperation(safepointLog.events())),
            () -> assertEquals(fixture.expectedOperationCounts().keySet(), safepointLog.byTypes().keySet()),
            () -> assertEquals(fixture.expectedOperationCounts().keySet(), safepointLog.distributions().keySet()),
            () -> assertEquals(
                expectedEvents.getLast().finishTimeSec() - expectedEvents.getFirst().startTimeSec(),
                safepointLog.totalLogTimeSec()
            )
        );

        for (var entry : safepointLog.byTypes().entrySet()) {
            var operationEvents = entry.getValue();
            assertEquals(
                operationEvents.stream().map(SafepointLogRecord::insideTimeNs).sorted().toList(),
                operationEvents.stream().map(SafepointLogRecord::insideTimeNs).toList()
            );

            var distribution = Objects.requireNonNull(safepointLog.distributions().get(entry.getKey()));
            assertFalse(distribution.isEmpty());
            assertEquals(1d, distribution.getLast().prob());
        }

        for (var event : expectedEvents) {
            var indexed = safepointLog.timeIndex()
                .findByRange(event.startTimeSec(), event.finishTimeSec())
                .stream()
                .toList();
            assertTrue(indexed.contains(event));
        }
    }

    private static Stream<SafepointLogFixture> fixtures() {
        return Stream.of(
            new SafepointLogFixture(
                "/org/sharpler/glag/aggregations/safepoint-log/legacy-cleanup.log",
                Map.of("Cleanup", 2)
            ),
            new SafepointLogFixture(
                "/org/sharpler/glag/aggregations/safepoint-log/legacy-icbufferfull.log",
                Map.of("ICBufferFull", 3)
            ),
            new SafepointLogFixture(
                "/org/sharpler/glag/aggregations/safepoint-log/modern.log",
                Map.of(
                    "Cleanup", 1,
                    "G1CollectForAllocation", 1
                )
            )
        );
    }

    private static List<String> readLines(String resourcePath) throws IOException {
        try (var stream = Objects.requireNonNull(SafepointLogTest.class.getResourceAsStream(resourcePath))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private static Map<String, Integer> countsByOperation(List<SafepointLogRecord> events) {
        return events.stream()
            .collect(java.util.stream.Collectors.toMap(
                SafepointLogRecord::operationName,
                event -> 1,
                Integer::sum
            ));
    }

    private record SafepointLogFixture(
        String resourcePath,
        Map<String, Integer> expectedOperationCounts
    ) {
    }
}
