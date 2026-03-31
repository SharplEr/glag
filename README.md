# glag

`glag` is a Java GC and safepoint log aggregator.

Its purpose is narrow on purpose:

- find long pauses,
- group safepoints and GC iterations,
- show simple distributions,
- provide enough context to investigate the problem further.

It is not a diagnostic expert system and it does not try to guess fixes for you.

## What it analyzes

`glag` reads two logs:

- a safepoint log,
- a GC log.

It correlates them in time and builds a report around:

- safepoint pause distributions,
- time-to-safepoint distributions,
- JVM operations executed at safepoints,
- slow individual safepoints,
- GC iterations that overlap long pauses,
- simultaneous GC iterations when multiple GC operations overlap in time.

The project targets modern HotSpot unified logging and currently assumes Java 11+ logs.

## Output modes

`glag` has three output modes.

### Console output

If you do not pass `-o` or `--output`, the report is printed to the console.

This mode is useful for quick inspection on the command line. It includes:

- throughput lost due to pauses,
- average pause period,
- cumulative distribution of time inside a safepoint,
- cumulative distribution of time to safepoint,
- per-operation cumulative distributions.

### Markdown report

If `--output` points to a non-HTML file, `glag` writes a Markdown report.

This is the most text-heavy format. In addition to the summary data, it includes:

- built-in documentation for detected GC and safepoint topics,
- built-in documentation for known JVM operations,
- tables for slow safepoints and slow GC iterations,
- raw GC log excerpts for slow GC iterations.

Example:

```bash
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -s /path/to/safepoint.log \
  -g /path/to/gc.log \
  -o /path/to/report.md
```

### HTML report

If `--output` ends with `.html`, `glag` writes a self-contained HTML report.

This format contains the same core content as the Markdown report, but presents it differently:

- built-in documentation is hidden behind collapsed spoilers,
- summary sections are shown as cards,
- cumulative distributions are rendered as charts,
- data points are available under expandable `Show data points` sections.

Example:

```bash
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -s /path/to/safepoint.log \
  -g /path/to/gc.log \
  -o /path/to/report.html
```

## Build and run

Build the application:

```bash
mvn package
```

Run with console output:

```bash
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -s /path/to/safepoint.log \
  -g /path/to/gc.log
```

Run with Markdown output:

```bash
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -s /path/to/safepoint.log \
  -g /path/to/gc.log \
  -o /path/to/report.md
```

Run with HTML output:

```bash
java -jar ./target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -s /path/to/safepoint.log \
  -g /path/to/gc.log \
  -o /path/to/report.html
```

## Command-line options

Current CLI options:

- `-s`, `--safepoints=SAFEPOINTS`
  Path to the safepoint log. Required.
- `-g`, `--gc=GC`
  Path to the GC log. Required.
- `-o`, `--output=OUTPUT`
  Output report path. Optional.
  If omitted, output goes to the console.
  If the path ends with `.html`, an HTML report is generated.
  Otherwise, a Markdown report is generated.
- `-t`, `--threshold=THRESHOLD`
  Slow safepoint threshold in milliseconds. Optional. Default: `50`.
- `--examples=EXAMPLES`
  Number of slow-operation examples to include in the report. Optional. Default: `5`.

## Notes

- Unknown JVM operations are still included in the report, but they may not have built-in descriptions.
- The HTML report is fully static. It does not require external JavaScript or a web server.
- The Markdown report is convenient if you want to convert it to PDF later.
