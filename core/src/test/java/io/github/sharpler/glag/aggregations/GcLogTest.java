package io.github.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.sharpler.glag.records.GcLogRecords;
import io.github.sharpler.glag.records.GcName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GcLogTest {
    @ParameterizedTest
    @MethodSource("fixtures")
    void parseReadsRealGcLogs(GcLogFixture fixture) throws IOException {
        var lines = readLines(fixture.resourcePath());
        var gcLog = GcLog.parse(lines);
        var gcIterations = gcLog.timeIndex()
            .findByRange(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
            .stream()
            .sorted(Comparator.comparingInt(GcLogRecords::gcNum))
            .toList();

        assertAll(
            () -> assertEquals(fixture.expectedGcName(), gcLog.gcName()),
            () -> assertEquals(fixture.expectedGcIterations(), gcIterations.size()),
            () -> assertEquals(
                fixture.expectedParsedLines(),
                gcIterations.stream().mapToInt(gcIteration -> gcIteration.records().size()).sum()
            )
        );
    }

    private static Stream<GcLogFixture> fixtures() {
        return Stream.of(
            new GcLogFixture("/io/github/sharpler/glag/aggregations/gc-log/serial.log", GcName.Serial, 3, 3),
            new GcLogFixture("/io/github/sharpler/glag/aggregations/gc-log/parallel.log", GcName.Parallel, 4, 4),
            new GcLogFixture("/io/github/sharpler/glag/aggregations/gc-log/g1.log", GcName.G1, 4, 4),
            new GcLogFixture("/io/github/sharpler/glag/aggregations/gc-log/shenandoah.log", GcName.Shenandoah, 1, 6),
            new GcLogFixture("/io/github/sharpler/glag/aggregations/gc-log/zgc.log", GcName.Z, 1, 8)
        );
    }

    private static java.util.List<String> readLines(String resourcePath) throws IOException {
        try (var stream = Objects.requireNonNull(GcLogTest.class.getResourceAsStream(resourcePath))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
        }
    }

    private record GcLogFixture(
        String resourcePath,
        GcName expectedGcName,
        int expectedGcIterations,
        int expectedParsedLines
    ) {
    }
}
