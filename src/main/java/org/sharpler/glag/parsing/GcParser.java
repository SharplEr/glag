package org.sharpler.glag.parsing;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sharpler.glag.records.GcLogRecord;

public final class GcParser {
    private static final Pattern PATTERN =
        Pattern.compile("^\\[([^\\]]*)\\]\\[([^\\]]*)s\\]\\[([^\\]]*)\\]\\[([^\\]]*)\\] GC\\((\\d*)\\).*$");

    private GcParser() {
        // No-op.
    }

    @Nullable
    public static GcLogRecord parse(String line) {
        var matcher = PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        var result = matcher.toMatchResult();

        return new GcLogRecord(
            line,
            Double.parseDouble(result.group(2)),
            Integer.parseInt(result.group(5))
        );
    }
}
