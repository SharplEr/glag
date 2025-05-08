package org.sharpler.glag.parsing;

import javax.annotation.Nullable;
import org.sharpler.glag.records.SafepointLogRecord;

public final class SafepointParser {
    private SafepointParser() {
        // No-op
    }

    public static SafepointLogRecord parse(String line, int lineNum) {
        var builder = new SafepointRecordBuilder(lineNum);

        var tagOpen = line.indexOf('[');
        var tagClose = line.indexOf(']', tagOpen);
        builder.addTime(line.substring(tagOpen + 1, tagClose));

        tagOpen = line.indexOf('[', tagClose);
        tagClose = line.indexOf(']', tagOpen);
        var timestampSec = Double.parseDouble(line.substring(tagOpen + 1, tagClose - 1));
        builder.addFinishTimeSec(timestampSec);

        // Ignore level and type, jump to last tag
        var lastComma = line.lastIndexOf(',');
        var lastTagClose = line.lastIndexOf(']', lastComma);

        var start = lastTagClose + 1;
        while (start < line.length()) {
            var commaIndex = line.indexOf(',', start);
            if (commaIndex < 0) {
                commaIndex = line.length();
            }
            @Nullable
            var type = SafepointValueType.resolveType(line, start);
            if (type != null) {
                builder.addValue(type, line, start, commaIndex);
            }
            start = commaIndex + 1;
        }

        return builder.build();
    }
}
