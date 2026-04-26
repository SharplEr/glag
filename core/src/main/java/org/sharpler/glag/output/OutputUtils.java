package org.sharpler.glag.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.sharpler.glag.aggregations.SafepointAggregate;

/// Shared output helpers for built-in docs and aggregate ordering.
public final class OutputUtils {
    /// Classpath root for built-in report documentation.
    public static final Path DOCS_PATH = Path.of("org", "sharpler", "glag", "docs");

    private OutputUtils() {
        // No-op.
    }

    /// Checks whether a built-in documentation resource exists.
    ///
    /// @param owner class whose classloader should be used
    /// @param docPath classpath-relative resource path
    /// @return `true` if the resource exists
    /// @throws IOException if the underlying stream fails while being opened
    public static boolean docExists(Class<?> owner, Path docPath) throws IOException {
        try (var stream = owner.getClassLoader().getResourceAsStream(docPath.toString())) {
            return stream != null;
        }
    }

    /// Reads a built-in documentation resource as UTF-8 text.
    ///
    /// @param owner class whose classloader should be used
    /// @param docPath classpath-relative resource path
    /// @return resource contents as UTF-8 text
    /// @throws IOException if the resource stream fails while being read
    public static String readDoc(Class<?> owner, Path docPath) throws IOException {
        var strPath = docPath.toString();
        try (var stream = owner.getClassLoader().getResourceAsStream(strPath)) {
            if (stream == null) {
                throw new IllegalStateException("Can't find document in resources: '%s'".formatted(strPath));
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /// Returns per-operation aggregates sorted by descending total pause time.
    ///
    /// @param aggregatesByType per-operation aggregates
    /// @return sorted aggregate entries
    public static List<Map.Entry<String, SafepointAggregate>> sortedOperationAggregates(
        Map<String, SafepointAggregate> aggregatesByType
    ) {
        return aggregatesByType.entrySet().stream()
            .sorted((left, right) -> Long.compare(right.getValue().totalTimeNsSum(), left.getValue().totalTimeNsSum()))
            .toList();
    }
}
