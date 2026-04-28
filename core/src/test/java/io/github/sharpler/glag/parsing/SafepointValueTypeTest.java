package io.github.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import org.junit.jupiter.api.Test;

class SafepointValueTypeTest {
    @Property
    void isMatchWithPrefixAndSuffix(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        assertTrue(type.isMatch(prefix + type.prefix + value + type.suffix, start));
    }

    @Property
    void doesNotMatchWithoutPrefix(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value
    ) {
        assertFalse(type.isMatch(type.prefix + value + type.suffix, 1));
    }

    @Property
    void doesNotMatchWithoutSuffix(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        assertFalse(type.isMatch(prefix + type.prefix + value, start));
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
    void parseNanos(
        @ForAll("nanosTypes") SafepointValueType type,
        @ForAll @LongRange(min = 0, max = Long.MAX_VALUE) long value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var suffix = "_".repeat(start + 1);
        var str = prefix + type.prefix + value + type.suffix + suffix;
        assertEquals(value, type.parseNanos(str, start, str.length() - suffix.length()));
    }

    @Property
    void parseNanosRejectsNegativeValues(
        @ForAll("nanosTypes") SafepointValueType type,
        @ForAll @LongRange(min = Long.MIN_VALUE, max = -1) long value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var suffix = "_".repeat(start + 1);
        var str = prefix + type.prefix + value + type.suffix + suffix;

        assertThrows(IllegalArgumentException.class, () -> type.parseNanos(str, start, str.length() - suffix.length()));
    }

    @Property
    void resolveType(
        @ForAll("types") SafepointValueType type,
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        var suffix = "_".repeat(start + 1);
        assertEquals(type, SafepointValueType.resolveType(prefix + type.prefix + value + type.suffix + suffix, start));
    }

    @Property
    void resolveTypeNoMatch(
        @ForAll("payloads") String value,
        @ForAll @IntRange(min = 1, max = 8) int start
    ) {
        var prefix = "_".repeat(start);
        assertNull(SafepointValueType.resolveType(prefix + value, start));
    }

    @Test
    void resolveTypeReturnsNullForNegativeStart() {
        assertNull(SafepointValueType.resolveType("anything", -1));
    }

    @Test
    void resolveTypeReturnsNullWhenStartDoesNotLeaveEnoughCharactersForLookupKey() {
        assertNull(SafepointValueType.resolveType("__", 0));
        assertNull(SafepointValueType.resolveType("abcd", 2));
    }

    @Test
    void resolveTypeReturnsNullWhenLookupKeyMatchesButPrefixDoesNot() {
        var type = SafepointValueType.SAFEPOINT_NAME;
        var line = "_Sa definitely not " + type.suffix;

        assertNull(SafepointValueType.resolveType(line, 0));
    }

    @Provide
    Arbitrary<SafepointValueType> types() {
        return Arbitraries.of(SafepointValueType.VALUES);
    }

    @Provide
    Arbitrary<SafepointValueType> nanosTypes() {
        return Arbitraries.of(SafepointValueType.VALUES.stream()
            .filter(type -> type != SafepointValueType.SAFEPOINT_NAME)
            .toList());
    }

    @Provide
    Arbitrary<String> payloads() {
        return Arbitraries.strings()
            .withChars("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:._-")
            .ofMinLength(1)
            .ofMaxLength(32);
    }
}
