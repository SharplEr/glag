package org.sharpler.glag.parsing;

import org.jspecify.annotations.Nullable;
import org.sharpler.glag.records.GcLogRecord;

/// Parser for raw HotSpot GC log lines.
public final class GcParser {
    private GcParser() {
        // No-op.
    }

    /// Parses a single GC log line.
    ///
    /// @param line raw GC log line
    /// @return parsed GC record, or `null` if the line is not a GC event
    @Nullable
    public static GcLogRecord parse(String line) {
        var timestampSec = UptimeDecorators.parseMostPreciseTimestampSec(line);
        if (Double.isNaN(timestampSec)) {
            return null;
        }

        var start = UptimeDecorators.skipLeadingDecorators(line);
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
}
