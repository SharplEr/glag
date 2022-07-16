package org.sharpler.glag.output;

import static org.fusesource.jansi.Ansi.Color.DEFAULT;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.ansi;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.sharpler.glag.aggregations.SafepointLog;
import org.sharpler.glag.distribution.CumulativeDistributionBuilder;

public final class ConsoleOutput {
    public static void print(SafepointLog safepoints, int thresholdMs) {
        for (var e : safepoints.distributions().entrySet()) {
            var events = safepoints.byTypes().get(e.getKey());

            AnsiConsole.out().println(ansi().a("Operation: ").fg(GREEN).a(e.getKey()).reset());

            AnsiConsole.out().printf("\tPeriod: %.3f (sec/op)%n", safepoints.totalLogTimeSec() / events.size());

            AnsiConsole.out().println("\tCumulative distribution:");

            for (var point : e.getValue()) {
                var timingMs = point.value() / 1E6;

                printLn(
                    timingMs > thresholdMs ? RED : DEFAULT,
                    "\t\ttiming = %.3f ms, probability = %.2f %%",
                    timingMs,
                    point.prob() * 100d
                );
            }
        }
        AnsiConsole.out().println("Time to safepoint cumulative distribution: ");

        for (var point : CumulativeDistributionBuilder.reachingDistribution(safepoints)) {
            var timingMs = point.value() / 1E6;
            printLn(
                timingMs > thresholdMs ? RED : DEFAULT,
                "\ttiming = %.3f ms, probability = %.2f %%",
                timingMs,
                point.prob() * 100d
            );
        }
    }

    private static void printLn(Ansi.Color color, String format, Object... args) {
        var line = String.format(format, args);
        AnsiConsole.out().println(ansi().fg(color).a(line).reset());
    }
}
