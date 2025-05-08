package org.sharpler.glag.parsing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class SafepointValueTypeTest {
    @Test
    void isMatch() {
        var value = "foo:12.34";
        for (var type : SafepointValueType.VALUES) {
            var ok = type.prefix + value + type.suffix;
            var wrong = '_' + type.prefix + value + type.suffix;

            Assertions.assertTrue(type.isMatch(ok, 0));
            Assertions.assertFalse(type.isMatch(ok, ok.length()));

            Assertions.assertTrue(type.isMatch(wrong, 1));
            Assertions.assertFalse(type.isMatch(wrong, 0));
        }
    }

    @Test
    void parseString() {
        var value = "foo:12.34";
        for (var type : SafepointValueType.VALUES) {
            var str = '_' + type.prefix + value + type.suffix + '_';
            Assertions.assertEquals(value, type.parseString(str, 1, str.length() - 1));
        }
    }

    @Test
    void parseLong() {
        var value = Long.MAX_VALUE;
        for (var type : SafepointValueType.VALUES) {
            var str = '_' + type.prefix + value + type.suffix + '_';
            Assertions.assertEquals(value, type.parseLong(str, 1, str.length() - 1));
        }
    }

    @Test
    void parseDouble() {
        var value = 3.1415;
        for (var type : SafepointValueType.VALUES) {
            var str = '_' + type.prefix + value + type.suffix + '_';
            Assertions.assertEquals(value, type.parseDouble(str, 1, str.length() - 1));
        }
    }

    @Test
    void resolveType() {
        var value = "foo:12.34";
        for (var type : SafepointValueType.VALUES) {
            var str = '_' + type.prefix + value + type.suffix + '_';
            Assertions.assertEquals(type, SafepointValueType.resolveType(str, 1));
        }
    }

    @Test
    void resolveTypeNoMatch() {
        Assertions.assertNull(SafepointValueType.resolveType("", 0));
    }
}
