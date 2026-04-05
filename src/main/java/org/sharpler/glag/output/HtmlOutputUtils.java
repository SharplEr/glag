package org.sharpler.glag.output;

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

final class HtmlOutputUtils {
    static final String FULL_PAGE_STYLES = """
        :root {
          --bg: #f4f1ea;
          --paper: #fffaf2;
          --paper-2: #fdf6eb;
          --ink: #1f241f;
          --muted: #5f675d;
          --line: #d8cdbb;
          --accent: #174c3c;
          --accent-soft: #dceee6;
          --warn: #a83c32;
          --warn-soft: #fbe2de;
          --shadow: 0 10px 35px rgba(35, 34, 29, 0.08);
        }
        * { box-sizing: border-box; }
        body {
          margin: 0;
          background:
            radial-gradient(circle at top left, rgba(23, 76, 60, 0.10), transparent 26%),
            linear-gradient(180deg, #f8f5ef 0%, var(--bg) 100%);
          color: var(--ink);
          font: 16px/1.55 Georgia, "Iowan Old Style", "Palatino Linotype", serif;
        }
        main {
          width: min(1180px, calc(100vw - 32px));
          margin: 24px auto 48px;
        }
        section, .hero {
          background: color-mix(in srgb, var(--paper) 94%, white);
          border: 1px solid var(--line);
          border-radius: 20px;
          box-shadow: var(--shadow);
          padding: 22px 24px;
          margin-bottom: 18px;
        }
        .hero {
          padding: 32px 24px;
          background:
            linear-gradient(135deg, rgba(23, 76, 60, 0.95), rgba(31, 36, 31, 0.92)),
            var(--paper);
          color: #fdf8f1;
          position: relative;
          overflow: hidden;
        }
        .hero::after {
          content: "";
          position: absolute;
          inset: auto -120px -120px auto;
          width: 280px;
          height: 280px;
          background: radial-gradient(circle, rgba(255,255,255,0.18), transparent 72%);
          pointer-events: none;
        }
        h1, h2, h3, h4 {
          margin: 0 0 12px;
          line-height: 1.15;
          font-family: "Avenir Next Condensed", "Franklin Gothic Medium", "Arial Narrow", sans-serif;
          letter-spacing: 0.02em;
        }
        h1 { font-size: clamp(2rem, 4vw, 3.4rem); }
        h2 { font-size: 2rem; }
        h3 { font-size: 1.45rem; }
        h4 { font-size: 1.1rem; margin-top: 16px; }
        p { margin: 0 0 12px; color: var(--muted); }
        code, pre { font-family: "SFMono-Regular", "Menlo", "Consolas", monospace; }
        .metrics {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
          gap: 12px;
          margin-top: 18px;
        }
        .metric {
          padding: 14px 16px;
          border-radius: 16px;
          background: rgba(255,255,255,0.1);
          border: 1px solid rgba(255,255,255,0.16);
        }
        .metric-name {
          display: block;
          color: rgba(253, 248, 241, 0.82);
          font-size: 0.88rem;
          margin-bottom: 6px;
        }
        details.doc {
          margin-top: 14px;
          border: 1px solid var(--line);
          border-radius: 14px;
          background: rgba(255, 255, 255, 0.5);
          overflow: hidden;
        }
        details.doc > summary {
          cursor: pointer;
          list-style: none;
          padding: 12px 14px;
          font-weight: 700;
          background: var(--paper-2);
        }
        details.doc[open] > summary {
          border-bottom: 1px solid var(--line);
        }
        .doc-body {
          padding: 14px;
        }
        .doc-body > :first-child { margin-top: 0; }
        .distribution-grid {
          display: grid;
          grid-template-columns: minmax(360px, 1.5fr) minmax(260px, 1fr);
          gap: 16px;
          align-items: start;
          margin-top: 12px;
        }
        .chart-card {
          margin: 0;
          padding: 10px;
          border-radius: 16px;
          background: var(--paper-2);
          border: 1px solid var(--line);
        }
        .chart-card figcaption {
          color: var(--muted);
          font-size: 0.9rem;
          padding: 0 6px 4px;
        }
        .chart-bg { fill: rgba(255,255,255,0.65); }
        .axis { stroke: #b39d80; stroke-width: 1; }
        .axis-label { fill: #756954; font-size: 12px; }
        .distribution-line {
          fill: none;
          stroke: var(--accent);
          stroke-width: 3;
          stroke-linecap: round;
          stroke-linejoin: round;
        }
        .threshold-line {
          stroke: var(--warn);
          stroke-width: 1.5;
          stroke-dasharray: 5 5;
        }
        .threshold-point {
          fill: var(--warn);
        }
        .threshold-label {
          fill: var(--warn);
          font-weight: 700;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          background: rgba(255,255,255,0.55);
          border: 1px solid var(--line);
          border-radius: 14px;
          overflow: hidden;
        }
        th, td {
          padding: 9px 12px;
          border-bottom: 1px solid rgba(216, 205, 187, 0.75);
          text-align: left;
          vertical-align: top;
        }
        th {
          background: var(--paper-2);
          font-size: 0.9rem;
        }
        tr:last-child td { border-bottom: none; }
        .over-threshold td {
          color: var(--warn);
          font-weight: 700;
          background: color-mix(in srgb, var(--warn-soft) 45%, white);
        }
        .operation, .gc-block {
          margin-top: 14px;
          padding-top: 12px;
          border-top: 1px solid rgba(216, 205, 187, 0.85);
        }
        .safepoint-entry {
          margin: 0 0 14px;
          padding: 12px 14px;
          border: 1px solid var(--line);
          border-radius: 14px;
          background: rgba(255, 255, 255, 0.45);
        }
        .safepoint-entry p {
          margin-bottom: 10px;
        }
        .log-block {
          margin: 0;
          padding: 14px;
          overflow-x: auto;
          background: #1f241f;
          color: #f3efe6;
        }
        @media (max-width: 900px) {
          main { width: min(100vw - 18px, 100%); margin-top: 10px; }
          section, .hero { padding: 16px; border-radius: 16px; }
          .distribution-grid { grid-template-columns: 1fr; }
        }
        """;

    static final String AGGREGATES_PAGE_STYLES = """
        :root {
          --bg: #f4f1ea;
          --paper: #fffaf2;
          --paper-2: #fdf6eb;
          --ink: #1f241f;
          --muted: #5f675d;
          --line: #d8cdbb;
          --accent: #174c3c;
          --accent-soft: #dceee6;
          --warn: #a83c32;
          --warn-soft: #fbe2de;
          --shadow: 0 10px 35px rgba(35, 34, 29, 0.08);
        }
        * { box-sizing: border-box; }
        body {
          margin: 0;
          font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
          color: var(--ink);
          background:
            radial-gradient(circle at top left, rgba(23, 76, 60, 0.12), transparent 30%),
            linear-gradient(180deg, #f8f4ec 0%, var(--bg) 100%);
        }
        main {
          width: min(1200px, calc(100vw - 32px));
          margin: 0 auto;
          padding: 24px 0 56px;
        }
        section {
          background: rgba(255, 250, 242, 0.92);
          border: 1px solid var(--line);
          border-radius: 24px;
          padding: 24px;
          margin-bottom: 24px;
          box-shadow: var(--shadow);
          backdrop-filter: blur(6px);
        }
        .hero {
          padding: 32px 24px;
          background:
            linear-gradient(135deg, rgba(23, 76, 60, 0.95), rgba(31, 36, 31, 0.92)),
            var(--paper);
          color: #fdf8f1;
          position: relative;
          overflow: hidden;
        }
        .hero::after {
          content: "";
          position: absolute;
          inset: auto -120px -120px auto;
          width: 280px;
          height: 280px;
          background: radial-gradient(circle, rgba(255,255,255,0.18), transparent 72%);
          pointer-events: none;
        }
        h1, h2, h3, h4, summary {
          font-family: "IBM Plex Serif", "Georgia", serif;
          letter-spacing: 0.01em;
        }
        h1 { margin: 0 0 12px; font-size: clamp(2.2rem, 4vw, 3.6rem); }
        h2 { margin-top: 0; font-size: clamp(1.6rem, 2.5vw, 2.2rem); }
        h3 { margin-top: 0; font-size: 1.35rem; }
        h4 { margin-bottom: 8px; font-size: 1.05rem; }
        p, li, summary, table { font-size: 0.97rem; line-height: 1.6; }
        .metrics {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
          gap: 12px;
          margin-top: 24px;
        }
        .metric {
          background: rgba(255,255,255,0.1);
          border: 1px solid rgba(255,255,255,0.16);
          border-radius: 18px;
          padding: 16px 18px;
        }
        .metric-name {
          display: block;
          color: rgba(253, 248, 241, 0.82);
          font-size: 0.85rem;
          text-transform: uppercase;
          letter-spacing: 0.08em;
          margin-bottom: 6px;
        }
        code {
          font-family: "IBM Plex Mono", "SFMono-Regular", monospace;
          background: rgba(23, 76, 60, 0.08);
          padding: 0.15em 0.35em;
          border-radius: 0.45em;
        }
        .distribution-grid {
          display: grid;
          grid-template-columns: 1.35fr minmax(250px, 0.85fr);
          gap: 16px;
          align-items: start;
        }
        .chart-card {
          margin: 0;
          background: var(--paper);
          border: 1px solid var(--line);
          border-radius: 18px;
          padding: 14px;
        }
        .chart-card figcaption {
          color: var(--muted);
          margin-top: 10px;
          font-size: 0.87rem;
        }
        .chart-bg { fill: #fffdf8; }
        .axis { stroke: #b7aa96; stroke-width: 1.2; }
        .axis-label {
          fill: #726854;
          font-size: 12px;
          font-family: "IBM Plex Mono", monospace;
        }
        .distribution-line {
          fill: none;
          stroke: var(--accent);
          stroke-width: 3;
          stroke-linejoin: round;
          stroke-linecap: round;
        }
        .threshold-line {
          stroke: var(--warn);
          stroke-width: 1.5;
          stroke-dasharray: 6 5;
        }
        .threshold-point { fill: var(--warn); }
        .threshold-label { fill: var(--warn); }
        details.doc, .table-details {
          background: var(--paper-2);
          border: 1px solid var(--line);
          border-radius: 18px;
          padding: 0 16px;
        }
        details.doc summary, .table-details summary {
          cursor: pointer;
          padding: 14px 0;
          list-style: none;
        }
        details.doc[open] summary, .table-details[open] summary {
          border-bottom: 1px solid var(--line);
          margin-bottom: 14px;
        }
        .doc-body { padding-bottom: 16px; }
        table {
          width: 100%;
          border-collapse: collapse;
          background: white;
          border-radius: 14px;
          overflow: hidden;
        }
        th, td {
          padding: 10px 12px;
          text-align: left;
          border-bottom: 1px solid #eadfce;
        }
        th {
          background: #f7f0e4;
          font-weight: 600;
        }
        tr.over-threshold td {
          background: var(--warn-soft);
          color: #7c231a;
          font-weight: 600;
        }
        .operation {
          border-style: dashed;
        }
        @media (max-width: 900px) {
          .distribution-grid { grid-template-columns: 1fr; }
          section { padding: 18px; border-radius: 18px; }
          main { width: min(100vw - 20px, 1200px); }
        }
        """;

    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer HTML_RENDERER = HtmlRenderer.builder().build();

    private HtmlOutputUtils() {
    }

    static void appendOverview(StringBuilder html, SafepointAggregate aggregate, String description) {
        html.append("<section class='hero'>");
        html.append("<h1>glag report</h1>");
        html.append("<p>").append(description).append("</p>");
        html.append("<div class='metrics'>");
        if (aggregate.hasInsideTimeNs()) {
            appendMetric(
                html,
                "Throughput lost inside safepoint",
                format("%.3f %%", aggregate.insideSafepointThroughputLoss())
            );
        }
        appendMetric(
            html,
            "Throughput lost due to total pauses",
            format("%.3f %%", aggregate.totalPauseThroughputLoss())
        );
        appendMetric(
            html,
            "Average pause period",
            format("%.3f sec/op", aggregate.averagePausePeriodSec())
        );
        html.append("</div>");
        html.append("</section>");
    }

    static void appendOptionalAggregateDistributions(
        StringBuilder html,
        SafepointAggregate aggregate,
        int thresholdMs,
        int headingLevel
    ) {
        appendDistributionSection(html, "Cumulative distribution of time inside a safepoint", aggregate.insideTimeDistribution(), thresholdMs, headingLevel);
        appendDistributionSection(html, "Cumulative distribution of time to safepoint", aggregate.reachingTimeDistribution(), thresholdMs, headingLevel);
        appendDistributionSection(html, "Cumulative distribution of cleanup time", aggregate.cleanupTimeDistribution(), thresholdMs, headingLevel);
        appendDistributionSection(html, "Cumulative distribution of time to leave safepoint", aggregate.leavingTimeDistribution(), thresholdMs, headingLevel);
    }

    static void appendDistributionSection(
        StringBuilder html,
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
        appendDistributionChart(html, points, thresholdMs);
        appendDistributionTable(html, points, thresholdMs);
        html.append("</div>");
    }

    static void appendDocDetails(
        StringBuilder html,
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

    static void appendPageStart(StringBuilder html, String styles) {
        html.append("""
            <!DOCTYPE html>
            <html lang='en'>
            <head>
            <meta charset='UTF-8'>
            <meta name='viewport' content='width=device-width, initial-scale=1'>
            <title>glag report</title>
            <style>
            """);
        html.append(styles);
        html.append("""
            </style>
            </head>
            <body>
            <main>
            """);
    }

    static void appendPageEnd(StringBuilder html) {
        html.append("</main></body></html>");
    }

    static String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    @FormatMethod
    static String format(@FormatString String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }

    private static void appendDistributionChart(StringBuilder html, List<CumulativeDistributionPoint> points, double thresholdMs) {
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

    private static double findThresholdIntersection(List<CumulativeDistributionPoint> points, double thresholdMs) {
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

    private static void appendDistributionTable(StringBuilder html, List<CumulativeDistributionPoint> points, double thresholdMs) {
        html.append("<details class='doc table-details'><summary>Show data points</summary>");
        appendDistributionTableContent(html, points, thresholdMs);
        html.append("</details>");
    }

    private static void appendDistributionTableContent(StringBuilder html, List<CumulativeDistributionPoint> points, double thresholdMs) {
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

    private static void appendMetric(StringBuilder html, String name, String value) {
        html.append("<div class='metric'><span class='metric-name'>")
            .append(escapeHtml(name))
            .append("</span><strong>")
            .append(escapeHtml(value))
            .append("</strong></div>");
    }

    private static String renderMarkdown(String markdown) {
        return HTML_RENDERER.render(MARKDOWN_PARSER.parse(markdown));
    }
}
