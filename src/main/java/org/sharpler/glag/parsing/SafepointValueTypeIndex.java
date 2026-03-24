package org.sharpler.glag.parsing;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.jspecify.annotations.Nullable;

final class SafepointValueTypeIndex {
    private final Int2ReferenceOpenHashMap<SafepointValueType> key2type;

    SafepointValueTypeIndex(SafepointValueType[] types) {
        key2type = new Int2ReferenceOpenHashMap<>(types.length);
        for (var type : types) {
            if (type.prefix.length() < 3) {
                throw new IllegalArgumentException("Prefix must have at least 3 characters: '%s'".formatted(type.prefix));
            }

            var key = key(type.prefix.charAt(1), type.prefix.charAt(2));

            var old = key2type.putIfAbsent(key, type);
            if (old != null) {
                throw new IllegalArgumentException(
                    "Prefixes must be distinguishable by second and third characters: '%s' and '%s'"
                        .formatted(old.prefix, type.prefix)
                );
            }
        }
    }

    @Nullable SafepointValueType parseType(String line, int start) {
        if (start < 0 || start + 2 >= line.length()) {
            return null;
        }

        var type = key2type.get(key(line.charAt(start + 1), line.charAt(start + 2)));
        if (type == null || !type.isMatch(line, start)) {
            return null;
        }
        return type;
    }

    private static int key(char second, char third) {
        return (second << 16) | third;
    }
}
