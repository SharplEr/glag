package io.github.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.sharpler.glag.parsing.SafepointParser;
import io.github.sharpler.glag.records.SafepointLogRecord;
import io.github.sharpler.glag.util.TimeUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SafepointLogTest {
    @ParameterizedTest
    @MethodSource("fixtures")
    void parseAggregatesRealSafepointLogs(SafepointLogFixture fixture) throws IOException {
        var lines = readLines(fixture.resourcePath());
        var expectedEvents = lines.stream().map(SafepointParser::parse).toList();
        var safepointLog = SafepointLog.parse(lines);

        assertAll(
            () -> assertEquals(expectedEvents, safepointLog.events().values()),
            () -> assertEquals(fixture.expectedOperationCounts(), countsByOperation(safepointLog.events().values())),
            () -> assertEquals(
                expectedEvents.stream().allMatch(SafepointLogRecord::hasReachingTimeNs),
                safepointLog.aggregate().hasReachingTimeNs()
            ),
            () -> assertEquals(
                expectedEvents.stream().allMatch(SafepointLogRecord::hasCleanupTimeNs),
                safepointLog.aggregate().hasCleanupTimeNs()
            ),
            () -> assertEquals(
                expectedEvents.stream().allMatch(SafepointLogRecord::hasInsideTimeNs),
                safepointLog.aggregate().hasInsideTimeNs()
            ),
            () -> assertEquals(
                expectedEvents.stream().allMatch(SafepointLogRecord::hasLeavingTimeNs),
                safepointLog.aggregate().hasLeavingTimeNs()
            ),
            () -> assertFalse(safepointLog.aggregate().totalTimeDistribution().isEmpty()),
            () -> assertEquals(fixture.expectedOperationCounts().keySet(), safepointLog.aggregatesByType().keySet()),
            () -> assertEquals(
                expectedEvents.getLast().finishTimeSec() - expectedEvents.getFirst().startTimeSec(),
                safepointLog.aggregate().totalLogTimeSec()
            )
        );

        var eventsByOperation = safepointLog.events().values().stream()
            .collect(Collectors.groupingBy(SafepointLogRecord::operationName));
        for (var entry : eventsByOperation.entrySet()) {
            var operationEvents = entry.getValue().stream()
                .sorted(java.util.Comparator.comparingLong(SafepointLogRecord::totalTimeNs))
                .toList();
            assertEquals(
                operationEvents.stream().map(SafepointLogRecord::totalTimeNs).sorted().toList(),
                operationEvents.stream().map(SafepointLogRecord::totalTimeNs).toList()
            );

            var aggregate = Objects.requireNonNull(safepointLog.aggregatesByType().get(entry.getKey()));
            assertFalse(aggregate.totalTimeDistribution().isEmpty());
            assertEquals(1d, aggregate.totalTimeDistribution().getLast().prob());
        }

        for (var event : expectedEvents) {
            var indexed = safepointLog.events()
                .findByRange(event.startTimeSec(), event.finishTimeSec())
                .stream()
                .toList();
            assertTrue(indexed.contains(event));
        }
    }

    @org.junit.jupiter.api.Test
    void parseMarksMissingReachingAndInsideTimesAsUnavailable() {
        var lines = List.of(
            "[3.412s][info][safepoint] Safepoint \"Cleanup\", Time since last: 177611286 ns, Total: 207779 ns"
        );

        var safepointLog = SafepointLog.parse(lines);
        var event = safepointLog.events().values().getFirst();

        assertAll(
            () -> assertEquals(TimeUtils.NO_TIME, event.reachingTimeNs()),
            () -> assertEquals(TimeUtils.NO_TIME, event.cleanupTimeNs()),
            () -> assertEquals(TimeUtils.NO_TIME, event.insideTimeNs()),
            () -> assertEquals(TimeUtils.NO_TIME, event.leavingTimeNs()),
            () -> assertFalse(safepointLog.aggregate().hasReachingTimeNs()),
            () -> assertFalse(safepointLog.aggregate().hasCleanupTimeNs()),
            () -> assertFalse(safepointLog.aggregate().hasInsideTimeNs()),
            () -> assertFalse(safepointLog.aggregate().hasLeavingTimeNs()),
            () -> assertFalse(safepointLog.aggregate().totalTimeDistribution().isEmpty())
        );
    }

    private static Stream<SafepointLogFixture> fixtures() {
        return Stream.of(
            new SafepointLogFixture(
                "/io/github/sharpler/glag/aggregations/safepoint-log/legacy-cleanup.log",
                Map.of("Cleanup", 2)
            ),
            new SafepointLogFixture(
                "/io/github/sharpler/glag/aggregations/safepoint-log/legacy-icbufferfull.log",
                Map.of("ICBufferFull", 3)
            ),
            new SafepointLogFixture(
                "/io/github/sharpler/glag/aggregations/safepoint-log/modern.log",
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
        return events.stream().collect(java.util.stream.Collectors.toMap(
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
