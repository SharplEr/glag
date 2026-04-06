package org.sharpler.glag.parsing;

import java.util.List;
import org.sharpler.glag.records.SafepointLogRecord;

/// Parser for raw safepoint log lines.
public final class SafepointParser {
    private SafepointParser() {
        // No-op
    }

    /// Parses all safepoint log lines in order.
    ///
    /// @param lines raw safepoint log lines
    /// @return parsed safepoint records
    public static List<SafepointLogRecord> parseAll(List<String> lines) {
        return lines.stream().map(SafepointParser::parse).toList();
    }

    /// Parses a single safepoint log line.
    ///
    /// @param line raw safepoint log line
    /// @return parsed safepoint record
    public static SafepointLogRecord parse(String line) {
        var builder = new SafepointRecordBuilder(line);

        builder.addFinishTimeSec(UptimeDecorators.parseMostPreciseTimestampSec(line));

        var start = UptimeDecorators.skipLeadingDecorators(line);
        while (start < line.length()) {
            var commaIndex = line.indexOf(',', start);
            if (commaIndex < 0) {
                commaIndex = line.length();
            }
            var type = SafepointValueType.resolveType(line, start);
            if (type != null) {
                builder.addValue(type, start, commaIndex);
            }
            start = commaIndex + 1;
        }

        return builder.build();
    }
}
