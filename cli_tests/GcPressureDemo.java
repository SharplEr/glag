import java.time.Duration;
import java.util.Locale;
import java.util.TreeMap;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

/// Small standalone GC pressure workload for collecting JVM logs under different collectors.
///
/// Run directly with:
///
/// `java -Xms256m -Xmx256m cli_tests/GcPressureDemo.java`
///
/// The workload keeps a bounded `TreeMap<Double, byte[]>` and on each iteration:
///
/// 1. removes a prefix of entries with a random threshold derived from current size,
/// 2. adds a batch of fresh `byte[]` objects under random keys.
///
/// This creates a mix of short-lived and longer-lived arrays and keeps the live set near `maxSize`.
final class GcPressureDemo {
    private static final int DEFAULT_DURATION_SEC = 60;
    private static final int DEFAULT_MAX_SIZE = 4_000;
    private static final int DEFAULT_ADD_COUNT = 1_000;
    private static final int DEFAULT_MIN_BLOCK_SIZE = 8 * 1024;
    private static final int DEFAULT_MAX_BLOCK_SIZE = 128 * 1024;
    private static final long DEFAULT_SEED = 42L;
    private static final int DEFAULT_REPORT_EVERY = 1_000;

    private GcPressureDemo() {
        // No-op
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--help".equals(args[0])) {
            printUsage();
            return;
        }

        var durationSec = getIntArg(args, "--duration-sec", DEFAULT_DURATION_SEC);
        var maxSize = getIntArg(args, "--max-size", DEFAULT_MAX_SIZE);
        var addCount = getIntArg(args, "--add-count", DEFAULT_ADD_COUNT);
        var minBlockSize = getIntArg(args, "--min-block-size", DEFAULT_MIN_BLOCK_SIZE);
        var maxBlockSize = getIntArg(args, "--max-block-size", DEFAULT_MAX_BLOCK_SIZE);
        var seed = getLongArg(args, "--seed", DEFAULT_SEED);
        var reportEvery = getIntArg(args, "--report-every", DEFAULT_REPORT_EVERY);

        if (durationSec <= 0) {
            throw new IllegalArgumentException("--duration-sec must be positive");
        }
        if (maxSize <= 0) {
            throw new IllegalArgumentException("--max-size must be positive");
        }
        if (addCount <= 0) {
            throw new IllegalArgumentException("--add-count must be positive");
        }
        if (addCount >= maxSize) {
            throw new IllegalArgumentException("--add-count must be smaller than --max-size");
        }
        if (minBlockSize <= 0) {
            throw new IllegalArgumentException("--min-block-size must be positive");
        }
        if (maxBlockSize < minBlockSize) {
            throw new IllegalArgumentException("--max-block-size must be >= --min-block-size");
        }
        if (reportEvery <= 0) {
            throw new IllegalArgumentException("--report-every must be positive");
        }

        var random = RandomGeneratorFactory.of("L64X128MixRandom").create(seed);
        var deadline = System.nanoTime() + Duration.ofSeconds(durationSec).toNanos();
        var values = new TreeMap<Double, byte[]>();

        long iterations = 0L;
        long insertedBytes = 0L;
        long removedEntries = 0L;
        var peakSize = 0;

        while (System.nanoTime() < deadline) {
            removedEntries += removeOldValues(values, addCount, maxSize, random);

            for (var i = 0; i < addCount; i++) {
                var key = random.nextDouble();
                var block = new byte[random.nextInt(minBlockSize, maxBlockSize + 1)];
                values.put(key, block);
                insertedBytes += block.length;
            }

            iterations++;
            peakSize = Math.max(peakSize, values.size());

            if (iterations % reportEvery == 0L) {
                System.out.printf(
                    Locale.ROOT,
                    "iteration=%d size=%d peak=%d insertedMiB=%.2f removed=%d%n",
                    iterations,
                    values.size(),
                    peakSize,
                    insertedBytes / (1024d * 1024d),
                    removedEntries
                );
            }
        }

        System.out.printf(
            Locale.ROOT,
            "done iterations=%d size=%d peak=%d insertedMiB=%.2f removed=%d%n",
            iterations,
            values.size(),
            peakSize,
            insertedBytes / (1024d * 1024d),
            removedEntries
        );
    }

    private static int removeOldValues(
        TreeMap<Double, byte[]> values,
        int addCount,
        int maxSize,
        RandomGenerator random
    ) {
        if (values.isEmpty()) {
            return 0;
        }

        var p = 1d - (maxSize - addCount) / (double) values.size();
        var clampedP = clamp01(p);
        var from = clamp01(clampedP / 2d);
        var to = clamp01(3d * clampedP / 2d);
        var threshold = from == to ? from : random.nextDouble(from, to);
        var removed = values.headMap(threshold, false).size();
        values.headMap(threshold, false).clear();
        return removed;
    }

    private static double clamp01(double value) {
        return Math.max(0d, Math.min(1d, value));
    }

    private static int getIntArg(String[] args, String name, int defaultValue) {
        var raw = getArg(args, name);
        return raw == null ? defaultValue : Integer.parseInt(raw);
    }

    private static long getLongArg(String[] args, String name, long defaultValue) {
        var raw = getArg(args, name);
        return raw == null ? defaultValue : Long.parseLong(raw);
    }

    private static String getArg(String[] args, String name) {
        for (var i = 0; i < args.length; i++) {
            if (name.equals(args[i])) {
                if (i + 1 >= args.length) {
                    throw new IllegalArgumentException("Missing value for " + name);
                }
                return args[i + 1];
            }
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("""
            Usage:
              java [-Xms...] [-Xmx...] cli_tests/GcPressureDemo.java [options]

            Options:
              --duration-sec N      How long to run. Default: 60
              --max-size N          Target upper bound for live entries. Default: 4000
              --add-count N         Number of new arrays added per iteration. Default: 1000
              --min-block-size N    Minimum byte[] size in bytes. Default: 8192
              --max-block-size N    Maximum byte[] size in bytes. Default: 131072
              --seed N              Random seed. Default: 42
              --report-every N      Print progress every N iterations. Default: 1000
              --help                Show this help
            """);
    }
}
