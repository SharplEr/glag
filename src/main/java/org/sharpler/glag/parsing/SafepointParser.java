package org.sharpler.glag.parsing;

import org.sharpler.glag.records.SafepointLogRecord;

public final class SafepointParser {
    private SafepointParser() {
        // No-op
    }

    public static SafepointLogRecord parse(String line, int lineNum) {
        var builder = new SafepointRecordBuilder(lineNum);

        var start = parseDecorators(line, builder);
        while (start < line.length()) {
            var commaIndex = line.indexOf(',', start);
            if (commaIndex < 0) {
                commaIndex = line.length();
            }
            var type = SafepointValueType.resolveType(line, start);
            if (type != null) {
                builder.addValue(type, line, start, commaIndex);
            }
            start = commaIndex + 1;
        }

        return builder.build();
    }

    private static int parseDecorators(String line, SafepointRecordBuilder builder) {
        var start = 0;
        while (start < line.length() && line.charAt(start) == '[') {
            var end = line.indexOf(']', start + 1);
            assert end >= 0;
            if (isFinishTimeSecDecorator(line, start + 1, end)) {
                builder.addFinishTimeSec(Double.parseDouble(line.substring(start + 1, end - 1)));
            }
            start = end + 1;
        }
        return start;
    }

    private static boolean isFinishTimeSecDecorator(String line, int start, int end) {
        var length = end - start;
        if (length < 2 || line.charAt(end - 1) != 's') {
            return false;
        }
        if (length == 2 && !Character.isDigit(line.charAt(start))) {
            return false;
        }

        for (var i = start; i < end - 1; i++) {
            var ch = line.charAt(i);
            if (!Character.isDigit(ch) && ch != '.') {
                return false;
            }
        }
        return true;
    }
}
