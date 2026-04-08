package org.sharpler.glag.parsing;

import static org.sharpler.glag.parsing.SafepointValueType.AT_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.CLEANUP;
import static org.sharpler.glag.parsing.SafepointValueType.LEAVING_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.REACHING_SAFEPOINT;
import static org.sharpler.glag.parsing.SafepointValueType.SAFEPOINT_NAME;
import static org.sharpler.glag.parsing.SafepointValueType.TOTAL;

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
                addValue(type, builder, start, commaIndex);
            }
            start = commaIndex + 1;
        }

        return builder.build();
    }

    private static void addValue(SafepointValueType type, SafepointRecordBuilder builder, int start, int end) {
        var origin = builder.origin();
        switch (type) {
            case SAFEPOINT_NAME -> builder.addOperationName(SAFEPOINT_NAME.parseString(origin, start, end));
            case REACHING_SAFEPOINT -> builder.addReachingTimeNs(REACHING_SAFEPOINT.parseNanos(origin, start, end));
            case CLEANUP -> builder.addCleanupTimeNs(CLEANUP.parseNanos(origin, start, end));
            case AT_SAFEPOINT -> builder.addInsideTimeNs(AT_SAFEPOINT.parseNanos(origin, start, end));
            case LEAVING_SAFEPOINT -> builder.addLeavingTimeNs(LEAVING_SAFEPOINT.parseNanos(origin, start, end));
            case TOTAL -> builder.addTotalTimeNs(TOTAL.parseNanos(origin, start, end));
        }
    }
}
