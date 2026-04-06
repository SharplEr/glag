package org.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.sharpler.glag.records.SafepointLogRecord;
import org.sharpler.glag.util.TimeUtils;

class SafepointRecordBuilderTest {
    private static final String ORIGIN = "origin";

    @Property
    void buildCreatesExpectedRecordForValidInput(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        builder.addOperationName(validCase.operationName());

        for (var value : validCase.values()) {
            switch (value.type()) {
                case REACHING_SAFEPOINT -> builder.addReachingTimeNs(value.valueNs());
                case CLEANUP -> builder.addCleanupTimeNs(value.valueNs());
                case AT_SAFEPOINT -> builder.addInsideTimeNs(value.valueNs());
                case LEAVING_SAFEPOINT -> builder.addLeavingTimeNs(value.valueNs());
                case TOTAL -> builder.addTotalTimeNs(value.valueNs());
                default -> throw new IllegalArgumentException(value.type().name());
            }
        }

        assertEquals(validCase.expectedRecord(), builder.build());
    }

    @Provide
    Arbitrary<ValidCase> validCases() {
        return Combinators.combine(
            Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:_-")
                .ofMinLength(1)
                .ofMaxLength(32),
            Arbitraries.longs().between(0L, 1_000_000_000_000_000L),
            Arbitraries.longs().between(0L, 1_000_000_000L),
            Arbitraries.of(false, true),
            Arbitraries.of(false, true),
            Arbitraries.of(false, true),
            Arbitraries.of(false, true)
        ).as(BaseValidCase::new).flatMap(base -> Combinators.combine(
            Arbitraries.longs().between(0L, 1_000_000_000L),
            Arbitraries.longs().between(0L, 1_000_000_000L),
            Arbitraries.longs().between(0L, 1_000_000_000L),
            Arbitraries.longs().between(0L, 1_000_000_000L)
        ).as((reachingTimeNs, cleanupTimeNs, insideTimeNs, leavingTimeNs) -> {
            var finishTimeNs = base.startTimeNs() + base.totalTimeNs();
            var finishTimeSec = finishTimeNs / 1E9;
            var values = new ArrayList<TypeValue>(5);
            if (base.hasReaching()) {
                values.add(new TypeValue(SafepointValueType.REACHING_SAFEPOINT, reachingTimeNs));
            }
            if (base.hasCleanup()) {
                values.add(new TypeValue(SafepointValueType.CLEANUP, cleanupTimeNs));
            }
            if (base.hasInside()) {
                values.add(new TypeValue(SafepointValueType.AT_SAFEPOINT, insideTimeNs));
            }
            if (base.hasLeaving()) {
                values.add(new TypeValue(SafepointValueType.LEAVING_SAFEPOINT, leavingTimeNs));
            }
            values.add(new TypeValue(SafepointValueType.TOTAL, base.totalTimeNs()));
            return new ValidCase(
                finishTimeSec - base.totalTimeNs() / 1E9,
                finishTimeSec,
                base.operationName(),
                values.toArray(TypeValue[]::new)
            );
        }));
    }

    private record ValidCase(
        double startTimeSec,
        double finishTimeSec,
        String operationName,
        TypeValue[] values
    ) {
        SafepointLogRecord expectedRecord() {
            return new SafepointLogRecord(
                startTimeSec,
                finishTimeSec,
                ORIGIN,
                operationName,
                optionalTime(SafepointValueType.REACHING_SAFEPOINT),
                optionalTime(SafepointValueType.CLEANUP),
                optionalTime(SafepointValueType.AT_SAFEPOINT),
                optionalTime(SafepointValueType.LEAVING_SAFEPOINT),
                requiredTime(SafepointValueType.TOTAL)
            );
        }

        private long optionalTime(SafepointValueType type) {
            return findTime(type, TimeUtils.NO_TIME);
        }

        private long requiredTime(SafepointValueType type) {
            return findTime(type, TimeUtils.NO_TIME);
        }

        private long findTime(SafepointValueType type, long defaultValue) {
            for (var value : values) {
                if (value.type() == type) {
                    return value.valueNs();
                }
            }
            return defaultValue;
        }
    }

    private record BaseValidCase(
        String operationName,
        long startTimeNs,
        long totalTimeNs,
        boolean hasReaching,
        boolean hasCleanup,
        boolean hasInside,
        boolean hasLeaving
    ) {
    }

    private record TypeValue(SafepointValueType type, long valueNs) {
    }
}
