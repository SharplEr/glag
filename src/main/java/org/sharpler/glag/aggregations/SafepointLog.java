package org.sharpler.glag.aggregations;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.sharpler.glag.index.RangeIndex;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.records.SafepointLogRecord;

public record SafepointLog(
    RangeIndex<SafepointLogRecord> events,
    SafepointAggregate aggregate,
    Map<String, SafepointAggregate> aggregatesByType
) {
    public static SafepointLog parse(List<String> lines) {
        var parsedEvents = lines.stream().map(SafepointParser::parse).toList();
        var totalLogTimeSec = parsedEvents.getLast().finishTimeSec() - parsedEvents.getFirst().startTimeSec();

        var operations2events = parsedEvents.stream()
            .collect(Collectors.groupingBy(SafepointLogRecord::operationName));

        for (var entry : operations2events.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(SafepointLogRecord::totalTimeNs));
        }

        var events = new RangeIndex<>(parsedEvents);
        var aggregate = SafepointAggregate.from(totalLogTimeSec, parsedEvents);
        var aggregatesByType = operations2events.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> SafepointAggregate.from(totalLogTimeSec, x.getValue())));

        return new SafepointLog(
            events,
            aggregate,
            aggregatesByType
        );
    }
}
