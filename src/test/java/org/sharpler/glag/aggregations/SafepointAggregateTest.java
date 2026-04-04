package org.sharpler.glag.aggregations;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sharpler.glag.records.SafepointLogRecord;

final class SafepointAggregateTest {
    @Test
    void fromBuildsAllDistributionsWhenAllTimesAreAvailable() {
        var events = List.of(
            new SafepointLogRecord(1.0, 1.1, "a", "Cleanup", 10, 20, 30, 40, 90),
            new SafepointLogRecord(2.0, 2.2, "b", "Cleanup", 15, 25, 35, 45, 120)
        );

        var aggregate = SafepointAggregate.from(1.2, events);

        assertAll(
            () -> assertEquals(1.2, aggregate.totalLogTimeSec(), 1E-9),
            () -> assertEquals(2, aggregate.eventsCount()),
            () -> assertEquals(210, aggregate.totalTimeNsSum()),
            () -> assertEquals(65, aggregate.insideTimeNsSum()),
            () -> assertTrue(aggregate.hasReachingTimeNs()),
            () -> assertTrue(aggregate.hasCleanupTimeNs()),
            () -> assertTrue(aggregate.hasInsideTimeNs()),
            () -> assertTrue(aggregate.hasLeavingTimeNs()),
            () -> assertEquals(1.75E-5, aggregate.totalPauseThroughputLoss(), 1E-12),
            () -> assertEquals(5.416666666666666E-6, aggregate.insideSafepointThroughputLoss(), 1E-12),
            () -> assertEquals(0.6, aggregate.averagePausePeriodSec(), 1E-9),
            () -> assertFalse(aggregate.totalTimeDistribution().isEmpty()),
            () -> assertFalse(aggregate.reachingTimeDistribution().isEmpty()),
            () -> assertFalse(aggregate.cleanupTimeDistribution().isEmpty()),
            () -> assertFalse(aggregate.insideTimeDistribution().isEmpty()),
            () -> assertFalse(aggregate.leavingTimeDistribution().isEmpty()),
            () -> assertEquals(1d, aggregate.totalTimeDistribution().getLast().prob()),
            () -> assertEquals(120, aggregate.totalTimeDistribution().getLast().value()),
            () -> assertEquals(15, aggregate.reachingTimeDistribution().getLast().value()),
            () -> assertEquals(25, aggregate.cleanupTimeDistribution().getLast().value()),
            () -> assertEquals(35, aggregate.insideTimeDistribution().getLast().value()),
            () -> assertEquals(45, aggregate.leavingTimeDistribution().getLast().value())
        );
    }

    @Test
    void fromUsesEmptyDistributionForOptionalMissingTimes() {
        var events = List.of(
            new SafepointLogRecord(
                1.0,
                1.1,
                "a",
                "Cleanup",
                SafepointLogRecord.NO_TIME,
                20,
                30,
                40,
                90
            )
        );

        var aggregate = SafepointAggregate.from(0.1, events);

        assertAll(
            () -> assertFalse(aggregate.totalTimeDistribution().isEmpty()),
            () -> assertEquals(1, aggregate.eventsCount()),
            () -> assertEquals(90, aggregate.totalTimeNsSum()),
            () -> assertEquals(30, aggregate.insideTimeNsSum()),
            () -> assertTrue(aggregate.reachingTimeDistribution().isEmpty()),
            () -> assertFalse(aggregate.hasReachingTimeNs()),
            () -> assertFalse(aggregate.cleanupTimeDistribution().isEmpty()),
            () -> assertTrue(aggregate.hasCleanupTimeNs()),
            () -> assertFalse(aggregate.insideTimeDistribution().isEmpty()),
            () -> assertTrue(aggregate.hasInsideTimeNs()),
            () -> assertFalse(aggregate.leavingTimeDistribution().isEmpty())
        );
    }

    @Test
    void fromMarksInsideTimeSumAsUnavailableWhenAnyValueIsMissing() {
        var events = List.of(
            new SafepointLogRecord(1.0, 1.1, "a", "Cleanup", 10, 20, 30, 40, 90),
            new SafepointLogRecord(2.0, 2.2, "b", "Cleanup", 15, 25, SafepointLogRecord.NO_TIME, 45, 120)
        );

        var aggregate = SafepointAggregate.from(1.2, events);

        assertAll(
            () -> assertEquals(SafepointAggregate.NO_TIME, aggregate.insideTimeNsSum()),
            () -> assertFalse(aggregate.hasInsideTimeNs()),
            () -> assertTrue(aggregate.insideTimeDistribution().isEmpty())
        );
    }

    @Test
    void fromRejectsEmptyEvents() {
        assertThrows(IllegalArgumentException.class, () -> SafepointAggregate.from(1.0, List.of()));
    }
}
