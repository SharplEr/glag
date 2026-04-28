package io.github.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.sharpler.glag.index.RangeIndex;
import io.github.sharpler.glag.records.GcLogRecord;
import io.github.sharpler.glag.records.GcLogRecords;
import io.github.sharpler.glag.records.GcName;
import io.github.sharpler.glag.records.SafepointLogRecord;
import io.github.sharpler.glag.util.TimeUtils;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuntimeEventsTest {
    @Test
    void createIgnoresGcIterationsThatAreTooShortForSafepoint() {
        var safepoint = new SafepointLogRecord(
            10.000,
            10.200,
            "safepoint",
            "Cleanup",
            TimeUtils.NO_TIME,
            TimeUtils.NO_TIME,
            TimeUtils.NO_TIME,
            TimeUtils.NO_TIME,
            200_000_000L
        );
        var safepointLog = new SafepointLog(
            new RangeIndex<>(List.of(safepoint)),
            new Aggregates(
                SafepointAggregate.from(0.2, List.of(safepoint)),
                java.util.Map.of("Cleanup", SafepointAggregate.from(0.2, List.of(safepoint)))
            )
        );
        var shortGc = new GcLogRecords(
            List.of(
                new GcLogRecord("start", 10.000, 1),
                new GcLogRecord("finish", 10.100, 1)
            ),
            1
        );
        var gcLog = new GcLog(GcName.G1, new RangeIndex<>(List.of(shortGc)));

        var runtimeEvents = RuntimeEvents.create(gcLog, safepointLog, 100);

        assertEquals(1, runtimeEvents.slowSingleVmOperations().getOrDefault("Cleanup", List.of()).size());
        assertEquals(List.of(), runtimeEvents.slowGcs());
        assertEquals(List.of(), runtimeEvents.slowSimultaneousGcs());
    }
}
