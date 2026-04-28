package io.github.sharpler.glag.aggregations;

import io.github.sharpler.glag.index.RangeIndex;
import io.github.sharpler.glag.parsing.SafepointParser;
import io.github.sharpler.glag.records.SafepointLogRecord;
import java.util.List;

/// Parsed safepoint log with both raw events and precomputed aggregates.
///
/// @param events safepoint events indexed by time range
/// @param aggregates overall and per-operation aggregates
public record SafepointLog(
    RangeIndex<SafepointLogRecord> events,
    Aggregates aggregates
) {
    /// Parses raw safepoint log lines.
    ///
    /// @param lines raw safepoint log lines
    /// @return parsed safepoint log
    public static SafepointLog parse(List<String> lines) {
        return from(SafepointParser.parseAll(lines));
    }

    /// Builds a safepoint log view from already parsed events.
    ///
    /// @param events parsed safepoint events
    /// @return indexed and aggregated safepoint log
    public static SafepointLog from(List<SafepointLogRecord> events) {
        return new SafepointLog(new RangeIndex<>(events), Aggregates.from(events));
    }

    /// Returns the aggregate over all safepoint events.
    ///
    /// @return overall safepoint aggregate
    public SafepointAggregate aggregate() {
        return aggregates.aggregate();
    }

    /// Returns per-operation aggregates keyed by operation name.
    ///
    /// @return per-operation aggregates
    public java.util.Map<String, SafepointAggregate> aggregatesByType() {
        return aggregates.aggregatesByType();
    }
}
