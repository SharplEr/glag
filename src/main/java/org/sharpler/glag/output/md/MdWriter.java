package org.sharpler.glag.output.md;

import static java.nio.file.StandardOpenOption.APPEND;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.output.OutputUtils;

final class MdWriter {
    private final Path output;

    MdWriter(Path output) {
        this.output = output;
    }

    void recreateFile() throws IOException {
        Files.deleteIfExists(output);
        Files.createFile(output);
    }

    @FormatMethod
    void writef(@FormatString String format, Object... args) throws IOException {
        Files.writeString(output, String.format(format, args), APPEND);
    }

    void writeDoc(Class<?> owner, Path docPath) throws IOException {
        Files.writeString(output, OutputUtils.readDoc(owner, docPath), APPEND);
    }

    void writeAggregateSection(
        String title,
        List<CumulativeDistributionPoint> points,
        int thresholdMs,
        int headingLevel
    ) throws IOException {
        if (points.isEmpty()) {
            return;
        }

        writef("%s %s%n%n", "#".repeat(headingLevel), title);
        writef("| Timing (ms) | Probability (%%)|%n");
        writef("| ----------- | -------------- |%n");
        for (var point : points) {
            var timingMs = point.value() / 1E6;
            writef(
                timingMs > thresholdMs ? "| **%.3f** | **%.2f** |%n" : "| %.3f | %.2f |%n",
                timingMs,
                point.prob() * 100d
            );
        }
        writef("%n");
    }
}
