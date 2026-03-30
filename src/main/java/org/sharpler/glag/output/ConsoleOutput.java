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
        printLn(
            DEFAULT,
            "Throughput lost based on pauses: %.3f (%%) - %.3f(%%)%n",
            safepoints.events().stream().mapToLong(SafepointLogRecord::insideTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7,
            safepoints.events().stream().mapToLong(SafepointLogRecord::totalTimeNs).sum() / safepoints.totalLogTimeSec() / 1E7
        );
        AnsiConsole.out().printf("Pauses period: %.3f (sec/op)%n%n", safepoints.totalLogTimeSec() / safepoints.events().size());

        AnsiConsole.out().println("Time inside safepoint cumulative distribution: ");
        printDistribution(CumulativeDistributionBuilder.insideDistribution(safepoints), thresholdMs, 1);

        AnsiConsole.out().println("Time to safepoint cumulative distribution: ");
        printDistribution(CumulativeDistributionBuilder.reachingDistribution(safepoints), thresholdMs, 1);

        AnsiConsole.out().println();
        for (var e : safepoints.distributions().entrySet()) {
            var events = Objects.requireNonNull(safepoints.byTypes().get(e.getKey()));

            AnsiConsole.out().println(ansi().a("Operation: ").fg(GREEN).a(e.getKey()).reset());

            AnsiConsole.out().printf("\tPeriod: %.3f (sec/op)%n", safepoints.totalLogTimeSec() / events.size());

            AnsiConsole.out().println("\tCumulative distribution:");

            printDistribution(e.getValue(), thresholdMs, 2);
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
