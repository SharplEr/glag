package org.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.List;
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
    private static final List<SafepointValueType> VALUES = SafepointValueType.VALUES.stream()
        .filter(x -> x != SafepointValueType.TOTAL && x != SafepointValueType.SAFEPOINT_NAME)
        .toList();

    @Property
    void buildCreatesExpectedRecordForValidInput(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        builder.addOperationName(validCase.operationName());

        for (var entry : validCase.values().object2LongEntrySet()) {
            var valueNs = entry.getLongValue();
            switch (entry.getKey()) {
                case REACHING_SAFEPOINT -> builder.addReachingTimeNs(valueNs);
                case CLEANUP -> builder.addCleanupTimeNs(valueNs);
                case AT_SAFEPOINT -> builder.addInsideTimeNs(valueNs);
                case LEAVING_SAFEPOINT -> builder.addLeavingTimeNs(valueNs);
                case TOTAL -> builder.addTotalTimeNs(valueNs);
                case SAFEPOINT_NAME -> throw new IllegalArgumentException(entry.getKey().name());
            }
        }

        assertEquals(validCase.expectedRecord(), builder.buildOrNull());
    }

    @Property
    void buildRejectsMissingOperationName(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        addAllValues(builder, validCase);

        assertNull(builder.buildOrNull());
    }

    @Property
    void buildRejectsMissingTotalTime(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        builder.addOperationName(validCase.operationName());
        addAllValuesExceptTotal(builder, validCase);

        assertNull(builder.buildOrNull());
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
            Arbitraries.of(VALUES)
                .list()
                .uniqueElements()
        ).as(BaseValidCase::new).flatMap(base ->
            timeArray(base.types().size())
                .map(times -> {
                    var finishTimeNs = base.startTimeNs() + base.totalTimeNs();
                    var finishTimeSec = finishTimeNs / 1E9;
                    var values = new Object2LongOpenHashMap<SafepointValueType>(times.length + 1);
                    values.defaultReturnValue(TimeUtils.NO_TIME);
                    for (var i = 0; i < times.length; i++) {
                        values.put(base.types().get(i), times[i]);
                    }
                    values.put(SafepointValueType.TOTAL, base.totalTimeNs());
                    return new ValidCase(
                        finishTimeSec - base.totalTimeNs() / 1E9,
                        finishTimeSec,
                        base.operationName(),
                        values
                    );
                }));
    }

    private static Arbitrary<long[]> timeArray(int size) {
        return Arbitraries.longs()
            .between(0L, 1_000_000_000L)
            .array(long[].class)
            .ofSize(size);
    }

    private static void addAllValues(SafepointRecordBuilder builder, ValidCase validCase) {
        for (var entry : validCase.values().object2LongEntrySet()) {
            addValue(builder, entry.getKey(), entry.getLongValue());
        }
    }

    private static void addAllValuesExceptTotal(SafepointRecordBuilder builder, ValidCase validCase) {
        for (var entry : validCase.values().object2LongEntrySet()) {
            if (entry.getKey() != SafepointValueType.TOTAL) {
                addValue(builder, entry.getKey(), entry.getLongValue());
            }
        }
    }

    private static void addValue(SafepointRecordBuilder builder, SafepointValueType type, long valueNs) {
        switch (type) {
            case REACHING_SAFEPOINT -> builder.addReachingTimeNs(valueNs);
            case CLEANUP -> builder.addCleanupTimeNs(valueNs);
            case AT_SAFEPOINT -> builder.addInsideTimeNs(valueNs);
            case LEAVING_SAFEPOINT -> builder.addLeavingTimeNs(valueNs);
            case TOTAL -> builder.addTotalTimeNs(valueNs);
            case SAFEPOINT_NAME -> throw new IllegalArgumentException(type.name());
        }
    }

    private record ValidCase(
        double startTimeSec,
        double finishTimeSec,
        String operationName,
        Object2LongOpenHashMap<SafepointValueType> values
    ) {
        SafepointLogRecord expectedRecord() {
            return new SafepointLogRecord(
                startTimeSec,
                finishTimeSec,
                ORIGIN,
                operationName,
                values.getLong(SafepointValueType.REACHING_SAFEPOINT),
                values.getLong(SafepointValueType.CLEANUP),
                values.getLong(SafepointValueType.AT_SAFEPOINT),
                values.getLong(SafepointValueType.LEAVING_SAFEPOINT),
                values.getLong(SafepointValueType.TOTAL)
            );
        }
    }

    private record BaseValidCase(
        String operationName,
        long startTimeNs,
        long totalTimeNs,
        List<SafepointValueType> types
    ) {
    }
}
