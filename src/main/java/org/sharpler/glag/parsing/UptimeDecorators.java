package org.sharpler.glag.parsing;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.util.TimeUtils;

final class UptimeDecorators {
    private UptimeDecorators() {
        // No-op.
    }

    static double parseMostPreciseTimestampSec(String line) {
        var seconds = Double.NaN;
        var millis = Double.NaN;
        var nanos = Double.NaN;
        var start = 0;
        while (start < line.length() && line.charAt(start) == '[') {
            var end = line.indexOf(']', start + 1);
            if (end < 0) {
                return Double.NaN;
            }

            var valueStart = start + 1;
            var unit = parseTimeunit(line, valueStart, end);

            if (unit != null) {
                switch (unit) {
                    case SECONDS -> seconds = JavaDoubleParser.parseDouble(line, valueStart, end - valueStart - 1);
                    case MILLIS -> millis = Long.parseLong(line, valueStart, end - 2, 10) / 1E3;
                    case NANOS -> nanos = Long.parseLong(line, valueStart, end - 2, 10) / 1E9;
                }
            }
            start = end + 1;
        }

        if (TimeUtils.isTime(nanos)) {
            return nanos;
        } else if (TimeUtils.isTime(millis)) {
            return millis;
        } else if (TimeUtils.isTime(seconds)) {
            return seconds;
        }

        return Double.NaN;
    }

    static int skipLeadingDecorators(String line) {
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

    private static @Nullable TimestampUnit parseTimeunit(String line, int start, int end) {
        var length = end - start;
        if (length < 2 || line.charAt(end - 1) != 's') {
            return null;
        }
        if (length >= 3) {
            var unit = line.charAt(end - 2);
            if (unit == 'm') {
                return TimestampUnit.MILLIS;
            }
            if (unit == 'n') {
                return TimestampUnit.NANOS;
            }
        }
        return TimestampUnit.SECONDS;
    }

    private enum TimestampUnit {
        SECONDS,
        MILLIS,
        NANOS
    }
}
