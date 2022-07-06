package org.sharpler.glag;

import java.util.regex.Pattern;

public final class SafepointParser {
    private SafepointParser() {
        // No-op
    }

    private final static Pattern PATTERN = Pattern.compile(
        "^\\[([^\\[\\]]*)\\]\\[([^\\[\\]]*)s\\]\\[([^\\[\\]]*)\\]\\[([^\\[\\]]*)\\] Safepoint \\\"([^\\\"]*)\\\", Time since last: (\\d*) ns, Reaching safepoint: (\\d*) ns, At safepoint: (\\d*) ns, Total: (\\d*) ns$"
    );

    public static SafepointEvent parse(String line, int lineNum) {
        var matcher = PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Can't parse: line = " + lineNum);
        }
        var result = matcher.toMatchResult();
        return new SafepointEvent(
            result.group(1),
            Double.parseDouble(result.group(2)),
            result.group(3),
            result.group(4),
            result.group(5),
            Long.parseLong(result.group(7)),
            Long.parseLong(result.group(8)),
            Long.parseLong(result.group(9)),
            lineNum
        );
    }
}
