package org.sharpler.glag.parsing;

import java.util.List;
import org.sharpler.glag.records.SafepointLogRecord;

public final class SafepointParser {
    private SafepointParser() {
        // No-op
    }

    public static List<SafepointLogRecord> parseAll(List<String> lines) {
        return lines.stream().map(SafepointParser::parse).toList();
    }

    public static SafepointLogRecord parse(String line) {
        var builder = new SafepointRecordBuilder(line);

        var finishTimeSec = UptimeDecorators.parseMostPreciseTimestampSec(line);
        assert !Double.isNaN(finishTimeSec);
        builder.addFinishTimeSec(finishTimeSec);

        var start = UptimeDecorators.skipLeadingDecorators(line);
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
}
