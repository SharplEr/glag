package org.sharpler.glag.output;

import static org.fusesource.jansi.Ansi.Color.DEFAULT;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.List;
import java.util.Objects;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.sharpler.glag.aggregations.SafepointLog;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.records.SafepointLogRecord;

public final class ConsoleOutput {
    public static void print(SafepointLog safepoints, int thresholdMs) {
        if (safepoints.hasInsideTimeNs()) {
            printLn(
                DEFAULT,
                "Throughput lost due to pauses: %.3f (%%) - %.3f (%%)%n",
                safepoints.events().stream().mapToLong(SafepointLogRecord::insideTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7,
                safepoints.events().stream().mapToLong(SafepointLogRecord::totalTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7
            );
        } else {
            printLn(
                DEFAULT,
                "Throughput lost due to total pauses: %.3f (%%)%n",
                safepoints.events().stream().mapToLong(SafepointLogRecord::totalTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7
            );
        }
        AnsiConsole.out().printf("Average pause period: %.3f sec/op%n%n", safepoints.totalLogTimeSec() / safepoints.events().size());

        if (safepoints.hasInsideTimeNs()) {
            AnsiConsole.out().println("Cumulative distribution of time inside a safepoint:");
            printDistribution(CumulativeDistributionBuilder.insideDistribution(safepoints), thresholdMs, 1);
        }

        if (safepoints.hasReachingTimeNs()) {
            AnsiConsole.out().println("Cumulative distribution of time to safepoint:");
            printDistribution(CumulativeDistributionBuilder.reachingDistribution(safepoints), thresholdMs, 1);
        }

        if (safepoints.hasCleanupTimeNs()) {
            AnsiConsole.out().println("Cumulative distribution of cleanup time:");
            printDistribution(CumulativeDistributionBuilder.cleanupDistribution(safepoints), thresholdMs, 1);
        }

        if (safepoints.hasLeavingTimeNs()) {
            AnsiConsole.out().println("Cumulative distribution of time to leave safepoint:");
            printDistribution(CumulativeDistributionBuilder.leavingDistribution(safepoints), thresholdMs, 1);
        }

        AnsiConsole.out().println();
        for (var e : safepoints.byTypes().entrySet()) {
            var events = e.getValue();

            AnsiConsole.out().println(ansi().a("Operation: ").fg(GREEN).a(e.getKey()).reset());

            AnsiConsole.out().printf("\tPeriod: %.3f (sec/op)%n", safepoints.totalLogTimeSec() / events.size());

            if (safepoints.hasInsideTimeNs()) {
                AnsiConsole.out().println("\tCumulative distribution:");
                printDistribution(Objects.requireNonNull(safepoints.distributions().get(e.getKey())), thresholdMs, 2);
            }
        }
    }

    private static void printDistribution(List<CumulativeDistributionPoint> points, double thresholdMs, int tabCount) {
        var indent = "\t".repeat(tabCount);
        var maxTimingMs = points.getLast().value() / 1E6;
        var timingIntegerWidth = Long.toString((long) maxTimingMs).length();
        var timingWidth = timingIntegerWidth + 4;
        for (var point : points) {
            var timingMs = point.value() / 1E6;
            var timing = String.format("%" + timingWidth + ".3f", timingMs);
            printLn(
                timingMs > thresholdMs ? RED : DEFAULT,
                "%stiming = %s ms, probability = %6.2f %%",
                indent,
                timing,
                point.prob() * 100d
            );
        }
    }

    @FormatMethod
    private static void printLn(Ansi.Color color, @FormatString String format, Object... args) {
        var line = String.format(format, args);
        AnsiConsole.out().println(ansi().fg(color).a(line).reset());
    }
}
