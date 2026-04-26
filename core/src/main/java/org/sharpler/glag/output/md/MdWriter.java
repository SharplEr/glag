package org.sharpler.glag.output.md;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.output.OutputUtils;

final class MdWriter {
    private final StringBuilder markdown;

    MdWriter(StringBuilder markdown) {
        this.markdown = markdown;
    }

    @FormatMethod
    void writef(@FormatString String format, Object... args) {
        markdown.append(String.format(format, args));
    }

    void writeDoc(Class<?> owner, Path docPath) throws IOException {
        markdown.append(OutputUtils.readDoc(owner, docPath));
    }

    void writeAggregateSection(
        String title,
        List<CumulativeDistributionPoint> points,
        int thresholdMs,
        int headingLevel
    ) {
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

    @Override
    public String toString() {
        return markdown.toString();
    }
}
