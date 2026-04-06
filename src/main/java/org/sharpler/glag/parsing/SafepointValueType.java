package org.sharpler.glag.parsing;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import java.util.List;
import org.jspecify.annotations.Nullable;

enum SafepointValueType {
    SAFEPOINT_NAME(" Safepoint \"", "\""),
    REACHING_SAFEPOINT(" Reaching safepoint: ", " ns"),
    CLEANUP(" Cleanup: ", " ns"),
    AT_SAFEPOINT(" At safepoint: ", " ns"),
    LEAVING_SAFEPOINT(" Leaving safepoint: ", " ns"),
    TOTAL(" Total: ", " ns"),
    ;
    static final List<SafepointValueType> VALUES = List.of(values());
    private static final Int2ReferenceOpenHashMap<SafepointValueType> index;

    static {
        var types = values();
        index = new Int2ReferenceOpenHashMap<>(types.length);
        for (var type : types) {
            assert type.prefix.length() >= 3 : "Prefix must have at least 3 characters: '%s'".formatted(type.prefix);
            var key = key(type.prefix.charAt(1), type.prefix.charAt(2));

            var old = index.putIfAbsent(key, type);
            if (old != null) {
                throw new IllegalArgumentException(
                    "Prefixes must be distinguishable by second and third characters: '%s' and '%s'"
                        .formatted(old.prefix, type.prefix)
                );
            }
        }

    }

    final String prefix;
    final String suffix;

    SafepointValueType(String prefix, String suffix) {
        if (prefix.length() < 3) {
            throw new IllegalArgumentException("Prefix must have at least 3 characters: '%s'".formatted(prefix));
        }
        this.prefix = prefix;
        this.suffix = suffix;
    }

    static @Nullable SafepointValueType resolveType(String line, int start) {
        if (start < 0 || start + 2 >= line.length()) {
            return null;
        }

        var type = index.get(key(line.charAt(start + 1), line.charAt(start + 2)));
        if (type == null || !type.isMatch(line, start)) {
            return null;
        }
        return type;
    }

    boolean isMatch(String line, int start) {
        return line.startsWith(prefix, start) && (line.indexOf(suffix, start + prefix.length()) >= 0);
    }

    String parseString(String line, int start, int end) {
        return line.substring(start + prefix.length(), end - suffix.length());
    }

    long parseLong(String line, int start, int end) {
        return Long.parseLong(line, start + prefix.length(), end - suffix.length(), 10);
    }

    private static int key(char second, char third) {
        return (second << 16) | third;
    }
}
