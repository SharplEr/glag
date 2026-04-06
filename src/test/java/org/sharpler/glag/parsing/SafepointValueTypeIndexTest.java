package org.sharpler.glag.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

class SafepointValueTypeIndexTest {
    @Property
    void parseType(
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
    void parseTypeNoMatch(
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
