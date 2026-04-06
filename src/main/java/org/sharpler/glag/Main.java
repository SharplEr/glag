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
import org.sharpler.glag.output.console.ConsoleOutput;
import org.sharpler.glag.output.html.HtmlAggregatesOutput;
import org.sharpler.glag.output.html.HtmlFullOutput;
import org.sharpler.glag.output.md.MdAggregatesOutput;
import org.sharpler.glag.output.md.MdFullOutput;
import org.sharpler.glag.parsing.SafepointParser;
import picocli.CommandLine;

final class Main implements Callable<Integer> {
    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(names = {"-s", "--safepoints"}, paramLabel = "SAFEPOINTS", description = "safepoints log", required = true)
    private Path safepointsPath = Paths.get(".");

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(names = {"-g", "--gc"}, paramLabel = "GC", description = "gc log", required = false)
    @Nullable
    private Path gcPath = null;

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(
        names = {"-t", "--threshold"},
        paramLabel = "THRESHOLD",
        description = "slow safepoint threshold in ms",
        required = false,
        defaultValue = "50"
    )
    private int thresholdMs = 50;

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(
        names = {"--examples"},
        paramLabel = "EXAMPLES",
        description = "count of slow operation examples",
        required = false,
        defaultValue = "5"
    )
    private int examples = 5;

    @SuppressWarnings({"FieldMayBeFinal", "CanBeFinal"})
    @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "OUTPUT", description = "Report output path", required = false)
    @Nullable
    private Path output = null;

    static void main(String... args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    /// Runs the CLI command using the configured input and output paths.
    ///
    /// @return process exit code
    /// @throws Exception if reading logs or writing a report fails
    @Override
    public Integer call() throws Exception {
        var safepointRecords = SafepointParser.parseAll(Files.readAllLines(safepointsPath));

        if (output == null) {
            ConsoleOutput.print(Aggregates.from(safepointRecords), thresholdMs);
        } else {
            var useHtml = output.toString().toLowerCase(Locale.ROOT).endsWith(".html");
            if (gcPath == null) {
                if (useHtml) {
                    new HtmlAggregatesOutput(output).print(Aggregates.from(safepointRecords), thresholdMs);
                } else {
                    new MdAggregatesOutput(output).print(Aggregates.from(safepointRecords), thresholdMs);
                }
            } else {
                var safepoints = SafepointLog.from(safepointRecords);
                var gclog = GcLog.parse(Files.readAllLines(gcPath));
                var runtimeEvents = RuntimeEvents.create(gclog, safepoints, thresholdMs);
                if (useHtml) {
                    new HtmlFullOutput(output).print(runtimeEvents, examples);
                } else {
                    new MdFullOutput(output).print(runtimeEvents, examples);
                }
            }
        }

        return 0;
    }
}
