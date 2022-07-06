package org.sharpler.glag.parsing;

import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;
import org.sharpler.glag.pojo.GcEvent;

public final class GcParser {
    private static final Pattern PATTERN =
        Pattern.compile("^\\[([^\\]]*)\\]\\[([^\\]]*)s\\]\\[([^\\]]*)\\]\\[([^\\]]*)\\] GC\\((\\d*)\\).*$");

    private GcParser() {
        // No-op.
    }


    @Nullable
    public static GcEvent parse(String line) {
        var matcher = PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }
        var result = matcher.toMatchResult();

        return new GcEvent(
            line,
            Double.parseDouble(result.group(2)),
            Integer.parseInt(result.group(5))
        );
    }
}
