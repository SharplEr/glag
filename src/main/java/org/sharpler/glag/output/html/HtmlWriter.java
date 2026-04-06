package org.sharpler.glag.output.html;

import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.fusesource.jansi.AnsiConsole;
import org.jspecify.annotations.Nullable;
import org.sharpler.glag.aggregations.SafepointAggregate;
import org.sharpler.glag.distribution.CumulativeDistributionPoint;
import org.sharpler.glag.output.OutputUtils;

final class HtmlWriter {
    static final Path FULL_PAGE_STYLES_PATH = Path.of("org", "sharpler", "glag", "output", "full-page.css");
    static final Path AGGREGATES_PAGE_STYLES_PATH = Path.of("org", "sharpler", "glag", "output", "aggregates-page.css");

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    private final StringBuilder html;

    HtmlWriter(StringBuilder html) {
        this.html = html;
    }

    HtmlWriter append(String value) {
        html.append(value);
        return this;
    }

    HtmlWriter append(char value) {
        html.append(value);
        return this;
    }

    HtmlWriter append(int value) {
        html.append(value);
        return this;
    }

    void appendOverview(SafepointAggregate aggregate, String description) {
        html.append("<section class='hero'>");
        html.append("<h1>glag report</h1>");
        html.append("<p>").append(description).append("</p>");
        html.append("<div class='metrics'>");
        if (aggregate.hasInsideTimeNs()) {
            appendMetric(
                "Throughput lost inside safepoint",
                format("%.3f %%", aggregate.insideSafepointThroughputLoss())
            );
        }
        appendMetric(
            "Throughput lost due to total pauses",
            format("%.3f %%", aggregate.totalPauseThroughputLoss())
        );
        appendMetric(
            "Average pause period",
            format("%.3f sec/op", aggregate.averagePausePeriodSec())
        );
        html.append("</div>");
        html.append("</section>");
    }

    void appendOptionalAggregateDistributions(
        SafepointAggregate aggregate,
        int thresholdMs,
        int headingLevel
    ) {
        appendDistributionSection(
            "Cumulative distribution of time inside a safepoint",
            aggregate.insideTimeDistribution(),
            thresholdMs,
            headingLevel
        );
        appendDistributionSection(
            "Cumulative distribution of time to safepoint",
            aggregate.reachingTimeDistribution(),
            thresholdMs,
            headingLevel
        );
        appendDistributionSection(
            "Cumulative distribution of cleanup time",
            aggregate.cleanupTimeDistribution(),
            thresholdMs,
            headingLevel
        );
        appendDistributionSection(
            "Cumulative distribution of time to leave safepoint",
            aggregate.leavingTimeDistribution(),
            thresholdMs,
            headingLevel
        );
    }

    void appendDistributionSection(
        String title,
        List<CumulativeDistributionPoint> points,
        double thresholdMs,
        int headingLevel
    ) {
        if (points.isEmpty()) {
            return;
        }
        html.append('<').append('h').append(headingLevel).append('>')
            .append(escapeHtml(title))
            .append("</h").append(headingLevel).append('>');
        html.append("<div class='distribution-grid'>");
        appendDistributionChart(points, thresholdMs);
        appendDistributionTable(points, thresholdMs);
        html.append("</div>");
    }

    void appendDocDetails(
        Class<?> owner,
        String summary,
        Path docPath,
        @Nullable String missingMessage
    ) throws IOException {
        if (!OutputUtils.docExists(owner, docPath)) {
            if (missingMessage != null) {
                AnsiConsole.err().println(missingMessage);
            }
            return;
        }

        html.append("<details class='doc'><summary>").append(escapeHtml(summary)).append("</summary>");
        html.append("<div class='doc-body'>").append(renderMarkdown(OutputUtils.readDoc(owner, docPath))).append("</div>");
        html.append("</details>");
    }

    void appendPageStart(Class<?> owner, Path stylesPath) throws IOException {
        html.append("""
            <!DOCTYPE html>
            <html lang='en'>
            <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1'>
            <title>glag report</title>
            <style>
            """);
        html.append(OutputUtils.readDoc(owner, stylesPath));
        html.append("""
            </style>
            </head>
            <body>
            <main>
            """);
    }

    void appendPageEnd() {
        html.append("</main></body></html>");
    }

    String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    @FormatMethod
    String format(@FormatString String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    @Override
    public String toString() {
        return html.toString();
    }

    private void appendDistributionChart(List<CumulativeDistributionPoint> points, double thresholdMs) {
        var width = 760d;
        var height = 288d;
        var left = 56d;
        var right = 84d;
        var top = 28d;
        var bottom = 32d;
        var plotWidth = width - left - right;
        var plotHeight = height - top - bottom;
        var maxTimingMs = points.getLast().value() / 1E6;
        var normalizedMaxTimingMs = maxTimingMs == 0d ? 1d : maxTimingMs;

        html.append("<figure class='chart-card'>");
        html.append("<svg viewBox='0 0 760 288' role='img' aria-label='Cumulative distribution chart'>");
        html.append("<rect x='0' y='0' width='760' height='288' rx='14' class='chart-bg'/>");
        html.append("<line x1='").append(format("%.2f", left)).append("' y1='").append(format("%.2f", top)).append("' x2='")
            .append(format("%.2f", left)).append("' y2='").append(format("%.2f", top + plotHeight)).append("' class='axis'/>");
        html.append("<line x1='").append(format("%.2f", left)).append("' y1='").append(format("%.2f", top + plotHeight)).append("' x2='")
            .append(format("%.2f", left + plotWidth)).append("' y2='").append(format("%.2f", top + plotHeight)).append("' class='axis'/>");

        if (thresholdMs > 0d && thresholdMs <= normalizedMaxTimingMs) {
            var thresholdY = top + plotHeight - (thresholdMs / normalizedMaxTimingMs) * plotHeight;
            var thresholdIntersection = findThresholdIntersection(points, thresholdMs);
            html.append("<line x1='").append(format("%.2f", left)).append("' y1='").append(format("%.2f", thresholdY)).append("' x2='")
                .append(format("%.2f", left + plotWidth)).append("' y2='")
                .append(format("%.2f", thresholdY)).append("' class='threshold-line'/>");
            html.append("<text x='16' y='").append(format("%.2f", thresholdY - 6d)).append("' class='axis-label threshold-label'>")
                .append(escapeHtml(format("%.3f ms", thresholdMs)))
                .append("</text>");
            if (!Double.isNaN(thresholdIntersection)) {
                var thresholdX = left + thresholdIntersection * plotWidth;
                var thresholdProbabilityText = format("%.2f %%", thresholdIntersection * 100d);
                var thresholdLabelWidth = 44d;
                var thresholdLabelX = Math.min(thresholdX + 8d, left + plotWidth - thresholdLabelWidth);
                html.append("<circle cx='").append(format("%.2f", thresholdX)).append("' cy='")
                    .append(format("%.2f", thresholdY)).append("' r='4' class='threshold-point'/>");
                html.append("<text x='").append(format("%.2f", thresholdLabelX)).append("' y='")
                    .append(format("%.2f", thresholdY - 8d)).append("' class='axis-label threshold-label'>")
                    .append(escapeHtml(thresholdProbabilityText))
                    .append("</text>");
            }
        }

        html.append("<polyline class='distribution-line' points='");
        if (points.getFirst().prob() > 0d) {
            html.append(format("%.2f,%.2f ", left, top + plotHeight));
        }
        for (var point : points) {
            var timingMs = point.value() / 1E6;
            var x = left + point.prob() * plotWidth;
            var y = top + plotHeight - (timingMs / normalizedMaxTimingMs) * plotHeight;
            html.append(format("%.2f,%.2f ", x, y));
        }
        html.append("'/>");

        html.append("<text x='").append(format("%.2f", left)).append("' y='").append(format("%.2f", top + plotHeight + 16d))
            .append("' class='axis-label'>0%</text>");
        html.append("<text x='").append(format("%.2f", left + plotWidth - 26d)).append("' y='")
            .append(format("%.2f", top + plotHeight + 16d)).append("' class='axis-label'>100%</text>");
        html.append("<text x='12' y='").append(format("%.2f", top - 6d)).append("' class='axis-label'>")
            .append(escapeHtml(format("%.3f ms", normalizedMaxTimingMs))).append("</text>");
        html.append("<text x='32' y='").append(format("%.2f", top + plotHeight + 4d)).append("' class='axis-label'>0</text>");
        html.append("</svg>");
        html.append("<figcaption>Probability on the X axis, timing in ms on the Y axis.</figcaption>");
        html.append("</figure>");
    }

    private double findThresholdIntersection(List<CumulativeDistributionPoint> points, double thresholdMs) {
        var previousProbability = 0d;
        var previousTimingMs = 0d;
        for (var point : points) {
            var currentProbability = point.prob();
            var currentTimingMs = point.value() / 1E6;
            if (thresholdMs <= currentTimingMs) {
                if (currentTimingMs == previousTimingMs) {
                    return currentProbability;
                }
                var ratio = (thresholdMs - previousTimingMs) / (currentTimingMs - previousTimingMs);
                return previousProbability + ratio * (currentProbability - previousProbability);
            }
            previousProbability = currentProbability;
            previousTimingMs = currentTimingMs;
        }
        return Double.NaN;
    }

    private void appendDistributionTable(List<CumulativeDistributionPoint> points, double thresholdMs) {
        html.append("<details class='doc table-details'><summary>Show data points</summary>");
        appendDistributionTableContent(points, thresholdMs);
        html.append("</details>");
    }

    private void appendDistributionTableContent(List<CumulativeDistributionPoint> points, double thresholdMs) {
        html.append("<table><thead><tr><th>Timing (ms)</th><th>Probability (%)</th></tr></thead><tbody>");
        for (var point : points) {
            var timingMs = point.value() / 1E6;
            html.append("<tr");
            if (timingMs > thresholdMs) {
                html.append(" class='over-threshold'");
            }
            html.append("><td>")
                .append(escapeHtml(format("%.3f", timingMs)))
                .append("</td><td>")
                .append(escapeHtml(format("%.2f", point.prob() * 100d)))
                .append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendMetric(String name, String value) {
        html.append("<div class='metric'><span class='metric-name'>")
            .append(escapeHtml(name))
            .append("</span><strong>")
            .append(escapeHtml(value))
            .append("</strong></div>");
    }

    private String renderMarkdown(String markdown) {
        return HTML_RENDERER.render(MARKDOWN_PARSER.parse(markdown));
    }
}
