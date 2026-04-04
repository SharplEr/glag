package org.sharpler.glag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.aggregations.Aggregates;
import org.sharpler.glag.aggregations.GcLog;
import org.sharpler.glag.aggregations.RuntimeEvents;
import org.sharpler.glag.aggregations.SafepointLog;
import org.sharpler.glag.output.ConsoleOutput;
import org.sharpler.glag.output.HtmlOutput;
import org.sharpler.glag.output.MdOutput;
import org.sharpler.glag.parsing.SafepointParser;
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
        var safepointRecords = SafepointParser.parseAll(Files.readAllLines(safepointsPath));

        if (output == null) {
            ConsoleOutput.print(Aggregates.from(safepointRecords), thresholdMs);
        } else {
            var safepoints = SafepointLog.from(safepointRecords);
            var gclog = GcLog.parse(Files.readAllLines(gcPath));
            var runtimeEvents = RuntimeEvents.create(gclog, safepoints, thresholdMs);
            if (output.toString().toLowerCase(Locale.ROOT).endsWith(".html")) {
                new HtmlOutput(output).print(runtimeEvents, examples);
            } else {
                new MdOutput(output).print(runtimeEvents, examples);
            }
        }

        return 0;
    }
}
