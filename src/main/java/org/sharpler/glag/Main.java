package org.sharpler.glag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.aggregations.GcLog;
import org.sharpler.glag.aggregations.RuntimeEvents;
import org.sharpler.glag.aggregations.SafepointLog;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.output.ConsoleOutput;
import org.sharpler.glag.output.HtmlOutput;
import org.sharpler.glag.output.MdOutput;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.records.SafepointLogRecord;
import picocli.CommandLine;

final class Main implements Callable<Integer> {
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(names = {"-s", "--safepoints"}, paramLabel = "SAFEPOINTS", description = "safepoints log", required = true)
    private Path safepointsPath = Paths.get(".");

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(names = {"-g", "--gc"}, paramLabel = "GC", description = "gc log", required = true)
    private Path gcPath = Paths.get(".");

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(
        names = {"-t", "--threshold"},
        paramLabel = "THRESHOLD",
        description = "slow safepoint threshold in ms",
        required = false,
        defaultValue = "50"
    )
    private int thresholdMs = 50;

    @CommandLine.Option(
        names = {"--examples"},
        paramLabel = "EXAMPLES",
        description = "count of slow operation examples",
        required = false,
        defaultValue = "5"
    )
    private int examples = 5;

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "OUTPUT", description = "Report output path", required = false)
    @Nullable
    private Path output = null;

    static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public Integer call() throws Exception {
        var safepoints = readSafepoints(safepointsPath);
        var gclog = GcLog.parse(Files.readAllLines(gcPath));

        if (output == null) {
            ConsoleOutput.print(safepoints, thresholdMs);
        } else {
            var runtimeEvents = RuntimeEvents.create(gclog, safepoints, thresholdMs);
            if (output.toString().toLowerCase(Locale.ROOT).endsWith(".html")) {
                new HtmlOutput(output).print(runtimeEvents, examples);
            } else {
                new MdOutput(output).print(runtimeEvents, examples);
            }
        }

        return 0;
    }

    private static SafepointLog readSafepoints(Path path) throws IOException {
        var lines = Files.readAllLines(path);

        var events = new ArrayList<SafepointLogRecord>(lines.size());
        for (var i = 0; i < lines.size(); i++) {
            events.add(SafepointParser.parse(lines.get(i), i));
        }

        var operations2events = events.stream()
            .collect(Collectors.groupingBy(SafepointLogRecord::operationName));

        for (var entry : operations2events.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(SafepointLogRecord::insideTimeNs));
        }

        var operations2stat = operations2events.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> CumulativeDistributionBuilder.operationTimeDistribution(x.getValue())));

        return new SafepointLog(
            events,
            operations2events,
            operations2stat,
            SafepointLog.buildIndex(events),
            events.getLast().finishTimeSec() - events.getFirst().startTimeSec()
        );
    }
}
