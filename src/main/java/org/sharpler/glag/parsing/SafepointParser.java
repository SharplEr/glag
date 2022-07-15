package org.sharpler.glag.parsing;

import java.util.regex.Pattern;
import org.sharpler.glag.records.SafepointLogRecord;

public final class SafepointParser {
    private SafepointParser() {
        // No-op
    }

    private static final Pattern PATTERN = Pattern.compile(
        "^\\[([^\\[\\]]*)\\]\\[([^\\[\\]]*)s\\]\\[([^\\[\\]]*)\\]\\[([^\\[\\]]*)\\] Safepoint \\\"([^\\\"]*)\\\", Time since last: (\\d*) ns, Reaching safepoint: (\\d*) ns, At safepoint: (\\d*) ns, Total: (\\d*) ns$"
    );

    public static SafepointLogRecord parse(String line, int lineNum) {
        var matcher = PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Can't parse: line = " + lineNum);
        }
        var result = matcher.toMatchResult();
        var timestampSec = Double.parseDouble(result.group(2));
        var totalTimeNs = Long.parseLong(result.group(9));
        return new SafepointLogRecord(
            result.group(1),
            timestampSec - totalTimeNs / 1E9,
            timestampSec,
            result.group(3),
            result.group(4),
            result.group(5),
            Long.parseLong(result.group(7)),
            Long.parseLong(result.group(8)),
            totalTimeNs,
            lineNum
        );
    }
}
