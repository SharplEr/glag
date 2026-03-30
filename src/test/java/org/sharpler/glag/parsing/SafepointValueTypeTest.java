package org.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;

class SafepointValueTypeTest {
    @Property
    void isMatchWithPrefixAndSuffix(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var str = prefix + type.prefix + value + type.suffix;

        assertTrue(type.isMatch(str, start));
    }

    @Property
    void doesNotMatchWithoutPrefix(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value
    ) {
        var str = type.prefix + value + type.suffix;

        assertFalse(type.isMatch(str, 1));
    }

    @Property
    void doesNotMatchWithoutSuffix(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var str = prefix + type.prefix + value;

        assertFalse(type.isMatch(str, start));
    }

    @Property
    void parseString(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var suffix = "_".repeat(start + 1);
        var str = prefix + type.prefix + value + type.suffix + suffix;

        assertEquals(value, type.parseString(str, start, str.length() - suffix.length()));
    }

    @Property
    void parseLong(
        @ForAll("types") SafepointValueType type,
        @ForAll @LongRange(min = Long.MIN_VALUE, max = Long.MAX_VALUE) long value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var suffix = "_".repeat(start + 1);
        var str = prefix + type.prefix + value + type.suffix + suffix;

        assertEquals(value, type.parseLong(str, start, str.length() - suffix.length()));
    }

    @Property
    void resolveType(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var suffix = "_".repeat(start + 1);
        var str = prefix + type.prefix + value + type.suffix + suffix;

        assertEquals(type, SafepointValueType.resolveType(str, start));
    }

    @Property
    void resolveTypeNoMatch(
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var str = prefix + value;

        assertNull(SafepointValueType.resolveType(str, start));
    }

    @Provide
    Arbitrary<SafepointValueType> types() {
        return Arbitraries.of(SafepointValueType.VALUES);
    }

    @Provide
    Arbitrary<String> payloads() {
        return Arbitraries.strings()
            .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:._-")
            .ofMinLength(1)
            .ofMaxLength(32);
    }
}
