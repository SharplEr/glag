package org.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.lang.reflect.Field;
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
        .filter(x -> x != SafepointValueType.TOTAL)
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
            }
        }

        assertEquals(validCase.expectedRecord(), builder.build());
    }

    @Property
    void buildRejectsMissingFinishTime(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addOperationName(validCase.operationName());
        addAllValuesExceptTotal(builder, validCase);
        assertThrows(
            AssertionError.class,
            () -> builder.addTotalTimeNs(validCase.values().getLong(SafepointValueType.TOTAL))
        );
    }

    @Property
    void buildRejectsMissingOperationName(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        addAllValues(builder, validCase);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Property
    void buildRejectsMissingTotalTime(@ForAll("validCases") ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        builder.addOperationName(validCase.operationName());
        addAllValuesExceptTotal(builder, validCase);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Property
    void buildRejectsInvalidOptionalTime(@ForAll("validCases") ValidCase validCase,
                                         @ForAll("invalidOptionalField") String fieldName,
                                         @ForAll("invalidOptionalTime") long invalidTimeNs)
        throws ReflectiveOperationException {
        var builder = populatedBuilder(validCase);
        setLongField(builder, fieldName, invalidTimeNs);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Property
    void buildRejectsInvalidTotalTime(@ForAll("validCases") ValidCase validCase,
                                      @ForAll("invalidOptionalTime") long invalidTimeNs)
        throws ReflectiveOperationException {
        var builder = populatedBuilder(validCase);
        setLongField(builder, "totalTimeNs", invalidTimeNs);

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Property
    void buildRejectsInvalidTimeRange(@ForAll("validCases") ValidCase validCase,
                                      @ForAll("invalidTimeField") String fieldName,
                                      @ForAll("invalidDoubleTime") double invalidTime)
        throws ReflectiveOperationException {
        var builder = populatedBuilder(validCase);
        setDoubleField(builder, fieldName, invalidTime);

        assertThrows(IllegalStateException.class, builder::build);
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
                .array(SafepointValueType[].class)
                .uniqueElements()
        ).as(BaseValidCase::new).flatMap(base ->
            Arbitraries.longs().between(0L, 1_000_000_000L).array(long[].class).ofSize(base.types.length)
                .map(times -> {
                    var finishTimeNs = base.startTimeNs() + base.totalTimeNs();
                    var finishTimeSec = finishTimeNs / 1E9;
                    var values = new Object2LongOpenHashMap<SafepointValueType>(times.length + 1);
                    values.defaultReturnValue(TimeUtils.NO_TIME);
                    for (var i = 0; i < times.length; i++) {
                        values.put(base.types[i], times[i]);
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

    @Provide
    Arbitrary<String> invalidOptionalField() {
        return Arbitraries.of("reachingTimeNs", "cleanupTimeNs", "insideTimeNs", "leavingTimeNs");
    }

    @Provide
    Arbitrary<String> invalidTimeField() {
        return Arbitraries.of("startTimeSec", "finishTimeSec");
    }

    @Provide
    Arbitrary<Long> invalidOptionalTime() {
        return Arbitraries.longs().lessOrEqual(TimeUtils.NO_TIME - 1L);
    }

    @Provide
    Arbitrary<Double> invalidDoubleTime() {
        return Arbitraries.of(-1d, -0.1d, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    private static SafepointRecordBuilder populatedBuilder(ValidCase validCase) {
        var builder = new SafepointRecordBuilder(ORIGIN);
        builder.addFinishTimeSec(validCase.finishTimeSec());
        builder.addOperationName(validCase.operationName());
        addAllValues(builder, validCase);
        return builder;
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
        }
    }

    private static void setLongField(SafepointRecordBuilder builder, String fieldName, long value)
        throws ReflectiveOperationException {
        var field = declaredField(fieldName);
        field.setLong(builder, value);
    }

    private static void setDoubleField(SafepointRecordBuilder builder, String fieldName, double value)
        throws ReflectiveOperationException {
        var field = declaredField(fieldName);
        field.setDouble(builder, value);
    }

    private static Field declaredField(String fieldName) throws NoSuchFieldException {
        var field = SafepointRecordBuilder.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
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
        SafepointValueType[] types
    ) {
    }
}
