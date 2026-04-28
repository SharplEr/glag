package io.github.sharpler.glag.output.console;

import static org.fusesource.jansi.Ansi.Color.DEFAULT;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import io.github.sharpler.glag.aggregations.Aggregates;
import io.github.sharpler.glag.aggregations.SafepointAggregate;
import io.github.sharpler.glag.distribution.CumulativeDistributionPoint;
import java.util.List;
import java.util.Objects;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

/// Renders safepoint aggregates to the terminal.
public final class ConsoleOutput {
    private ConsoleOutput() {
        // No-op.
    }

    /// Prints a console report built only from safepoint aggregates.
    ///
    /// @param safepoints overall and per-operation aggregates
    /// @param thresholdMs threshold used to color timing values
    public static void print(Aggregates safepoints, int thresholdMs) {
        var aggregate = safepoints.aggregate();
        if (aggregate.hasInsideTimeNs()) {
            printLn(
                DEFAULT,
                "Throughput lost due to pauses: %.3f (%%) - %.3f (%%)%n",
                aggregate.insideSafepointThroughputLoss(),
                aggregate.totalPauseThroughputLoss()
            );
        } else {
            printLn(
                DEFAULT,
                "Throughput lost due to total pauses: %.3f (%%)%n",
                aggregate.totalPauseThroughputLoss()
            );
        }
        AnsiConsole.out().printf("Average pause period: %.3f sec/op%n%n", aggregate.averagePausePeriodSec());

        printAggregateDistributions(aggregate, thresholdMs, 0);

        AnsiConsole.out().println();
        for (var e : safepoints.aggregatesByType().entrySet().stream()
            .sorted((left, right) -> Long.compare(right.getValue().totalTimeNsSum(), left.getValue().totalTimeNsSum()))
            .toList()) {
            var operationAggregate = Objects.requireNonNull(safepoints.aggregatesByType().get(e.getKey()));

            AnsiConsole.out().println(ansi().a("Operation: ").fg(GREEN).a(e.getKey()).reset());
            AnsiConsole.out().printf("\tPeriod: %.3f (sec/op)%n", operationAggregate.averagePausePeriodSec());
            if (operationAggregate.hasInsideTimeNs()) {
                AnsiConsole.out().printf(
                    "\tThroughput lost due to pauses: %.3f (%%) - %.3f (%%)%n",
                    operationAggregate.insideSafepointThroughputLoss(),
                    operationAggregate.totalPauseThroughputLoss()
                );
            } else {
                AnsiConsole.out().printf(
                    "\tThroughput lost due to total pauses: %.3f (%%)%n",
                    operationAggregate.totalPauseThroughputLoss()
                );
            }
            printAggregateDistributions(operationAggregate, thresholdMs, 1);
        }
    }

    private static void printAggregateDistributions(SafepointAggregate aggregate, int thresholdMs, int tabCount) {
        var indent = "\t".repeat(tabCount);
        AnsiConsole.out().println(indent + "Cumulative distribution of total time:");
        var distributionTabCount = tabCount + 1;
        printDistribution(aggregate.totalTimeDistribution(), thresholdMs, distributionTabCount);

        if (aggregate.hasInsideTimeNs()) {
            AnsiConsole.out().println(indent + "Cumulative distribution of time inside a safepoint:");
            printDistribution(aggregate.insideTimeDistribution(), thresholdMs, distributionTabCount);
        }

        if (aggregate.hasReachingTimeNs()) {
            AnsiConsole.out().println(indent + "Cumulative distribution of time to safepoint:");
            printDistribution(aggregate.reachingTimeDistribution(), thresholdMs, distributionTabCount);
        }

        if (aggregate.hasCleanupTimeNs()) {
            AnsiConsole.out().println(indent + "Cumulative distribution of cleanup time:");
            printDistribution(aggregate.cleanupTimeDistribution(), thresholdMs, distributionTabCount);
        }

        if (aggregate.hasLeavingTimeNs()) {
            AnsiConsole.out().println(indent + "Cumulative distribution of time to leave safepoint:");
            printDistribution(aggregate.leavingTimeDistribution(), thresholdMs, distributionTabCount);
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
