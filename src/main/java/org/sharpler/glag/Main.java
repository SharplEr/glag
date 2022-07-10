package org.sharpler.glag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.sharpler.glag.aggregations.GcLog;
import org.sharpler.glag.aggregations.SafapointLog;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.output.ConsoleOutput;
import org.sharpler.glag.output.MdOutput;
import org.sharpler.glag.parsing.GcParser;
import org.sharpler.glag.parsing.SafepointParser;
import org.sharpler.glag.pojo.GcEvent;
import org.sharpler.glag.pojo.GcName;
import org.sharpler.glag.pojo.GcTime;
import org.sharpler.glag.pojo.SafepointEvent;
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

    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "OUTPUT", description = "Report output path", required = false)
    private Path output = null;

    public static void main(String... args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        var safepoints = readSafepoints(safepointsPath);
        var gclog = readGcLog(gcPath);

        if (output == null) {
            ConsoleOutput.print(safepoints, gclog, thresholdMs);
        } else {
            new MdOutput(output).print(safepoints, gclog, thresholdMs);
        }

        return 0;
    }

    private static GcLog readGcLog(Path path) throws IOException {
        var events = Files.readAllLines(path).stream()
            .map(GcParser::parse)
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(GcEvent::gcNum));

        var times = new ArrayList<GcTime>(events.size());
        for (var e : events.entrySet()) {
            var stats = e.getValue().stream().mapToDouble(GcEvent::timestampSec).summaryStatistics();
            times.add(new GcTime(e.getKey(), stats.getMin(), stats.getMax()));
        }

        var min = Double.MAX_VALUE;
        var max = Double.MIN_VALUE;
        GcName gcName = null;
        for (var ev : events.values()) {
            for (var e : ev) {
                min = Math.min(min, e.timestampSec());
                max = Math.max(max, e.timestampSec());
                if (gcName == null) {
                    for (var gc : GcName.VALUES) {
                        if (e.origin().contains(gc.getName())) {
                            gcName = gc;
                            break;
                        }
                    }
                }
            }
        }

        return new GcLog(gcName, events, times, min, max);
    }

    private static SafapointLog readSafepoints(Path path) throws IOException {
        var lines = Files.readAllLines(path);

        var events = new ArrayList<SafepointEvent>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            events.add(SafepointParser.parse(lines.get(i), i));
        }

        var operations2events = events.stream()
            .collect(Collectors.groupingBy(SafepointEvent::operationName));

        for (var entry : operations2events.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(SafepointEvent::insideTimeNs));
        }

        var operations2stat = operations2events.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, x -> CumulativeDistributionBuilder.operationTimeDistribution(x.getValue())));

        return new SafapointLog(
            operations2events,
            operations2stat,
            events.get(0).timestampSec(),
            events.get(events.size() - 1).timestampSec()
        );
    }
}