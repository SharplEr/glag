package org.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.sharpler.glag.records.SafepointLogRecord;
import org.sharpler.glag.util.TimeUtils;

class AggregatesTest {
    @Test
    void fromRejectsEmptyEventList() {
        assertThrows(IllegalArgumentException.class, () -> Aggregates.from(List.of()));
    }

    @Test
    void fromBuildsOverallAndPerOperationAggregates() {
        var events = List.of(
            new SafepointLogRecord(1.0, 1.1, "a", "Cleanup", 10, 20, 30, 40, 100),
            new SafepointLogRecord(2.0, 2.15, "b", "Cleanup", 12, 22, 32, 42, 150),
            new SafepointLogRecord(
                3.0,
                3.05,
                "c",
                "ICBufferFull",
                TimeUtils.NO_TIME,
                TimeUtils.NO_TIME,
                TimeUtils.NO_TIME,
                TimeUtils.NO_TIME,
                50
            )
        );

        var aggregates = Aggregates.from(events);
        var cleanupAggregate = Objects.requireNonNull(aggregates.aggregatesByType().get("Cleanup"));
        var icBufferFullAggregate = Objects.requireNonNull(aggregates.aggregatesByType().get("ICBufferFull"));

        assertEquals(3L, aggregates.aggregate().eventsCount());
        assertEquals(300L, aggregates.aggregate().totalTimeNsSum());
        assertEquals(2, aggregates.aggregatesByType().size());
        assertEquals(250L, cleanupAggregate.totalTimeNsSum());
        assertEquals(50L, icBufferFullAggregate.totalTimeNsSum());
    }
}
