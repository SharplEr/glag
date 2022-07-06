package org.sharpler.glag.output;

import static org.fusesource.jansi.Ansi.Color.DEFAULT;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.concurrent.TimeUnit;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.sharpler.glag.aggregations.GcLog;
import org.sharpler.glag.aggregations.SafapointLog;

public final class ConsoleOutput {
    public static void print(SafapointLog safepoints, GcLog gcLog, int thresholdMs) {
        for (var e : safepoints.distributions().entrySet()) {
            AnsiConsole.out().println(ansi().a("Operation: ").fg(GREEN).a(e.getKey()).reset());

            AnsiConsole.out().println("\tCumulative distributions:");

            for (var point : e.getValue()) {
                var timingMs = point.value() / 1E6;

                printLn(
                    timingMs > thresholdMs ? RED : DEFAULT,
                    "\t\ttiming = %.3f ms, probability = %.2f %%",
                    timingMs,
                    point.prob() * 100d
                );
            }

            if (e.getValue().get(e.getValue().size() - 1).value() / 1E6 < thresholdMs) {
                continue;
            }

            printLn(YELLOW, "\tSlow events: threshold = %d (ms)", thresholdMs);

            for (var event : safepoints.events().get(e.getKey())) {
                if (event.totalTimeNs() > TimeUnit.MILLISECONDS.toNanos(thresholdMs)) {

                    var line = ansi().a("\t\t").a(event).a(", ");
                    if (event.timestampSec() < gcLog.startLogSec() || event.timestampSec() > gcLog.finishLogSec()) {
                        line = line.fg(RED).a("OUT OF GC LOG");
                    } else {
                        var gcNums = gcLog.findGcByTime(event.timestampSec(), 0.1d);
                        if (gcNums.isEmpty()) {
                            line = line.fg(YELLOW).a("miss in GC log");
                        } else {
                            line = line.a("GC number = ").a(gcNums);
                        }
                    }
                    AnsiConsole.out().println(line.reset());
                }
            }
        }
    }

    private static void printLn(Ansi.Color color, String format, Object... args) {
        var line = String.format(format, args);
        AnsiConsole.out().println(ansi().fg(color).a(line).reset());
    }
}
