package org.sharpler.glag.output;

import static java.nio.file.StandardOpenOption.APPEND;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;

final class MdOutputUtils {
    private MdOutputUtils() {
    }

    @FormatMethod
    static void writef(Path output, @FormatString String format, Object... args) throws IOException {
        Files.writeString(output, String.format(format, args), APPEND);
    }

    static void writeDoc(Path output, Class<?> owner, Path docPath) throws IOException {
        Files.writeString(output, OutputUtils.readDoc(owner, docPath), APPEND);
    }

    static void writeAggregateSection(
        Path output,
        String title,
        List<CumulativeDistributionPoint> points,
        int thresholdMs,
        int headingLevel
    ) throws IOException {
        if (points.isEmpty()) {
            return;
        }

        writef(output, "%s %s%n%n", "#".repeat(headingLevel), title);
        writef(output, "| Timing (ms) | Probability (%%)|%n");
        writef(output, "| ----------- | -------------- |%n");
        for (var point : points) {
            var timingMs = point.value() / 1E6;
            writef(
                output,
                timingMs > thresholdMs ? "| **%.3f** | **%.2f** |%n" : "| %.3f | %.2f |%n",
                timingMs,
                point.prob() * 100d
            );
        }
        writef(output, "%n");
    }
}
