package org.sharpler.glag.aggregations;

import java.util.List;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointLog(
    RangeIndex<SafepointLogRecord> events,
    Aggregates aggregates
) {
    public static SafepointLog parse(List<String> lines) {
        return from(SafepointParser.parseAll(lines));
    }

    public static SafepointLog from(List<SafepointLogRecord> events) {
        return new SafepointLog(new RangeIndex<>(events), Aggregates.from(events));
    }

    public SafepointAggregate aggregate() {
        return aggregates.aggregate();
    }

    public java.util.Map<String, SafepointAggregate> aggregatesByType() {
        return aggregates.aggregatesByType();
    }
}
