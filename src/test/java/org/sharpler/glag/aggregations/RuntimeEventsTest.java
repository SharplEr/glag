package org.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.records.GcLogRecord;
import org.sharpler.glag.records.GcLogRecords;
import org.sharpler.glag.records.GcName;
import org.sharpler.glag.records.SafepointLogRecord;

final class RuntimeEventsTest {
    @Test
    void createIgnoresGcIterationsThatAreTooShortForSafepoint() {
        var safepoint = new SafepointLogRecord(
            10.000,
            10.200,
            "safepoint",
            "Cleanup",
            SafepointLogRecord.NO_TIME,
            SafepointLogRecord.NO_TIME,
            SafepointLogRecord.NO_TIME,
            SafepointLogRecord.NO_TIME,
            200_000_000L
        );
        var safepointLog = new SafepointLog(
            new RangeIndex<>(List.of(safepoint)),
            java.util.Map.of("Cleanup", List.of(safepoint)),
            java.util.Map.of("Cleanup", List.of()),
            false,
            false,
            false,
            false,
            0.2
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
