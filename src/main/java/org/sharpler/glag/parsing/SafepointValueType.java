package org.sharpler.glag.parsing;

import java.util.List;
import javax.annotation.Nullable;

enum SafepointValueType {
    SAFEPOINT_NAME(" Safepoint \"", "\""),
    TIME_SINCE_LAST(" Time since last: ", " ns"),
    REACHING_SAFEPOINT(" Reaching safepoint: ", " ns"),
    CLEANUP(" Cleanup: ", " ns"),
    AT_SAFEPOINT(" At safepoint: ", " ns"),
    TOTAL(" Total: ", " ns"),
    ;
    static final List<SafepointValueType> VALUES = List.of(values());

    final String prefix;
    final String suffix;

    SafepointValueType(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Nullable
    static SafepointValueType resolveType(String line, int start) {
        for (var type : VALUES) {
            if (type.isMatch(line, start)) {
                return type;
            }
        }

        return null;
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

    double parseDouble(String line, int start, int end) {
        return Double.parseDouble(parseString(line, start, end));
    }
}
