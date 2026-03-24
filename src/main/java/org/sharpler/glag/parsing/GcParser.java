package org.sharpler.glag.parsing;

import org.jspecify.annotations.Nullable;
import org.sharpler.glag.records.GcLogRecord;

public final class GcParser {
    private GcParser() {
        // No-op.
    }

    @Nullable
    public static GcLogRecord parse(String line) {
        var timestampSec = parseTimestampSec(line);
        if (Double.isNaN(timestampSec)) {
            return null;
        }

        var start = skipDecorators(line);
        while (start < line.length() && line.charAt(start) == ' ') {
            start++;
        }
        if (!line.startsWith("GC(", start)) {
            return null;
        }

        var gcNumStart = start + 3;
        var gcNumEnd = line.indexOf(')', gcNumStart);
        if (gcNumEnd < 0 || gcNumEnd == gcNumStart) {
            return null;
        }
        for (var i = gcNumStart; i < gcNumEnd; i++) {
            if (!Character.isDigit(line.charAt(i))) {
                return null;
            }
        }

        return new GcLogRecord(
            line,
            timestampSec,
            Integer.parseInt(line, gcNumStart, gcNumEnd, 10)
        );
    }

    private static double parseTimestampSec(String line) {
        var seconds = Double.NaN;
        var millis = Double.NaN;
        var nanos = Double.NaN;
        var start = 0;
        while (start < line.length() && line.charAt(start) == '[') {
            var end = line.indexOf(']', start + 1);
            if (end < 0) {
                return Double.NaN;
            }

            var parsed = parseTimestampDecorator(line, start + 1, end);
            if (parsed != null) {
                switch (parsed.unit()) {
                    case SECONDS -> seconds = parsed.seconds();
                    case MILLIS -> millis = parsed.seconds();
                    case NANOS -> nanos = parsed.seconds();
                }
            }
            start = end + 1;
        }
        if (!Double.isNaN(nanos)) {
            return nanos;
        }
        if (!Double.isNaN(millis)) {
            return millis;
        }
        return seconds;
    }

    private static @Nullable TimestampDecorator parseTimestampDecorator(String line, int start, int end) {
        var length = end - start;
        if (length < 2) {
            return null;
        }
        if (line.charAt(end - 1) == 's') {
            if (length >= 3) {
                var c = line.charAt(end - 2);
                if (c == 'm') {
                    var l = Long.parseLong(line, start, end - 2, 10);
                    return new TimestampDecorator(l / 1_000d, TimestampUnit.MILLIS);
                }
                if (c == 'n') {
                    var l = Long.parseLong(line, start, end - 2, 10);
                    return new TimestampDecorator(l / 1_000_000_000d, TimestampUnit.NANOS);
                }
            }
            return new TimestampDecorator(Double.parseDouble(line.substring(start, end - 1)), TimestampUnit.SECONDS);
        }
        return null;
    }

    private static int skipDecorators(String line) {
        var start = 0;
        while (start < line.length() && line.charAt(start) == '[') {
            var end = line.indexOf(']', start + 1);
            if (end < 0) {
                return line.length();
            }
            start = end + 1;
        }
        return start;
    }

    private record TimestampDecorator(double seconds, TimestampUnit unit) {
    }

    private enum TimestampUnit {
        SECONDS,
        MILLIS,
        NANOS
    }
}
