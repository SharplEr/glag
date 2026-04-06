package org.sharpler.glag.output;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.sharpler.glag.aggregations.SafepointAggregate;

final class OutputUtils {
    static final Path DOCS_PATH = Path.of("org", "sharpler", "glag", "docs");

    private OutputUtils() {
    }

    static boolean docExists(Class<?> owner, Path docPath) throws IOException {
        try (var stream = owner.getClassLoader().getResourceAsStream(docPath.toString())) {
            return stream != null;
        }
    }

    static String readDoc(Class<?> owner, Path docPath) throws IOException {
        var strPath = docPath.toString();
        try (var stream = owner.getClassLoader().getResourceAsStream(strPath)) {
            if (stream == null) {
                throw new IllegalStateException("Can't find document in resources: '%s'".formatted(strPath));
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    static List<Map.Entry<String, SafepointAggregate>> sortedOperationAggregates(
        Map<String, SafepointAggregate> aggregatesByType
    ) {
        return aggregatesByType.entrySet().stream()
            .sorted((left, right) -> Long.compare(right.getValue().totalTimeNsSum(), left.getValue().totalTimeNsSum()))
            .toList();
    }
}
