package io.github.sharpler.glag.parsing;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import io.github.sharpler.glag.util.TimeUtils;
import org.jspecify.annotations.Nullable;

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
                    case SECONDS -> seconds = parseDouble(line, valueStart, end - valueStart - 1);
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

    private static double parseDouble(CharSequence line, int offset, int length) {
        try {
            return JavaDoubleParser.parseDouble(line, offset, length);
        } catch (NumberFormatException e) {
            var verboseException = new NumberFormatException(
                "Failed to parse subSequence '%s' as double in line %s"
                    .formatted(line.subSequence(offset, offset + length), line)
            );
            verboseException.addSuppressed(e);
            throw verboseException;
        }
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
            if (unit == 'm' && isAsciiDigits(line, start, end - 2)) {
                return TimestampUnit.MILLIS;
            }
            if (unit == 'n' && isAsciiDigits(line, start, end - 2)) {
                return TimestampUnit.NANOS;
            }
        }
        return isAsciiDecimal(line, start, end - 1) ? TimestampUnit.SECONDS : null;
    }

    private static boolean isAsciiDigits(String line, int start, int end) {
        if (start >= end) {
            return false;
        }
        for (var i = start; i < end; i++) {
            if (!Character.isDigit(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAsciiDecimal(String line, int start, int end) {
        if (start >= end) {
            return false;
        }

        var sawDigit = false;
        var sawDot = false;
        for (var i = start; i < end; i++) {
            var current = line.charAt(i);
            if (Character.isDigit(current)) {
                sawDigit = true;
                continue;
            }
            if (current == '.' && !sawDot) {
                sawDot = true;
                continue;
            }
            return false;
        }
        return sawDigit;
    }

    private enum TimestampUnit {
        SECONDS,
        MILLIS,
        NANOS
    }
}
