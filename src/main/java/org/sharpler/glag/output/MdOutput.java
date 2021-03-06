package org.sharpler.glag.output;

import static java.nio.file.StandardOpenOption.APPEND;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.sharpler.glag.aggregations.RuntimeEvents;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.records.SafepointLogRecord;

public final class MdOutput {
    private static final Path DOCS_PATH = Paths.get("src", "main", "resources", "docs");

    private final Path output;

    public MdOutput(Path output) {
        this.output = output;
    }

    public void print(RuntimeEvents runtimeEvents) throws IOException {
        var thresholdMs = runtimeEvents.thresholdMs();
        var safepoints = runtimeEvents.safepointLog();

        Files.deleteIfExists(output);
        Files.createFile(output);
        writef("# Report%n%n");
        writef("This report of gc and safepoint log analysis has been generated by **glag** tool version 1.0-SNAPSHOT.%n%n");

        writef(
            "Throughput lost based on pauses: %.3f (%%) - %.3f(%%)%n%n",
            safepoints.events().stream().mapToLong(SafepointLogRecord::insideTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7,
            safepoints.events().stream().mapToLong(SafepointLogRecord::totalTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7
        );

        if (runtimeEvents.gcName() != null) {
            var name = runtimeEvents.gcName().getName();
            writef("%s has been detected.%n%n", name);
            var gcDescription = DOCS_PATH.resolve("gc").resolve(name + ".md");
            if (Files.exists(gcDescription)) {
                writef("## %s%n%n", name);
                writeDoc(gcDescription);
                writef("%n%n");
            }
        }

        writef("## Safepoints%n%n");
        writeDoc(DOCS_PATH.resolve("safepoint").resolve("safepoint.md"));
        writef("%n%n");
        writef("## JVM operations in safepoint%n%n");

        for (var e : safepoints.distributions().entrySet()) {
            var events = safepoints.byTypes().get(e.getKey());

            writef("### Operation '%s'%n%n", e.getKey());
            var description = DOCS_PATH.resolve("operation").resolve(e.getKey() + ".md");
            if (Files.exists(description)) {
                writef("#### Description%n%n");
                writeDoc(description);
                writef("%n%n");
            }

            writef("Period: %.3f (sec/op)%n%n", safepoints.totalLogTimeSec() / events.size());

            writef("#### Cumulative distribution:%n%n");

            writef("| Timing (ms) | Probability (%%)|%n");
            writef("| ----------- | -------------- |%n");
            for (var point : e.getValue()) {
                var timingMs = point.value() / 1E6;

                writef(
                    timingMs > thresholdMs ? "| **%.3f** | **%.2f** |%n" : "| %.3f | %.2f |%n",
                    timingMs,
                    point.prob() * 100d
                );
            }

            var slowSingleVmOperations = runtimeEvents.slowSingleVmOperations().getOrDefault(e.getKey(), List.of());

            if (slowSingleVmOperations.isEmpty()) {
                continue;
            }

            writef("#### Slow single safepoints: threshold = %d (ms)%n%n", thresholdMs);

            writef("| line in safepoint log | operation time (ns)| time to safepoint (ns) |%n");
            writef("| --------------------- | ------------------ | ---------------------- |%n");

            for (var event : slowSingleVmOperations) {
                writef(
                    "| %d | %d | %d |%n",
                    event.safepointLog().line(),
                    event.safepointLog().insideTimeNs(),
                    event.safepointLog().reachingTimeNs()
                );
            }
        }

        writef("## Time to safepoint%n%n");

        writeDoc(DOCS_PATH.resolve("safepoint").resolve("time_to_safepoint.md"));

        writef("%n%n");

        writef("### Cumulative distribution%n%n");

        writef("| Timing (ms) | Probability (%%)|%n");
        writef("| ----------- | -------------- |%n");

        for (var point : CumulativeDistributionBuilder.reachingDistribution(safepoints)) {
            var timingMs = point.value() / 1E6;
            writef(
                timingMs > thresholdMs ?
                    "| **%.3f** | **%.2f** |%n" :
                    "| %.3f | %.2f |%n",
                timingMs,
                point.prob() * 100d
            );
        }

        var slowGcs = runtimeEvents.slowGcs();
        if (slowGcs.isEmpty()) {
            return;
        }

        writef("## GC iteration with long pauses: threshold = %d (ms)%n%n", thresholdMs);

        for (var slowGc : slowGcs) {
            writef("### GC iteration %d%n%n", slowGc.gcNum());

            writef("#### Slow safepoints%n%n");
            writef("| line in safepoint log | operation | operation time (ns)| time to safepoint (ns) |%n");
            writef("| --------------------- | ----------| ------------------ | ---------------------- |%n");
            for (var safepoint : slowGc.safepointLog()) {
                writef(
                    "| %d | %s | %d | %d |%n",
                    safepoint.line(),
                    safepoint.operationName(),
                    safepoint.insideTimeNs(),
                    safepoint.reachingTimeNs()
                );
            }

            writef("#### Gc logs%n%n");
            writef("```%n");
            for (var gcEvent : slowGc.gcLog()) {
                writef("%s%n", gcEvent.origin());
            }
            writef("```%n%n");
        }
    }

    private void writef(String format, Object... args) throws IOException {
        Files.writeString(output, String.format(format, args), APPEND);
    }

    private void writeDoc(Path docPath) throws IOException {
        Files.write(output, Files.readAllBytes(docPath), APPEND);
    }
}
