#!/bin/sh

set -eu

usage() {
    cat <<'EOF'
Usage:
  generate-gc-logs.sh [options] -- /path/to/app.jar [app args...]
  generate-gc-logs.sh [options] -- /path/to/App.java [app args...]

Runs the same workload several times with different HotSpot garbage collectors and stores
the generated GC and safepoint logs in separate output directories.

The workload may be either:

  - an executable jar,
  - a single-file Java source launched by `java path/to/File.java`.

Options:
  -o, --output-dir DIR      Output directory. Default: ./generated-gc-logs
  -l, --log-level LEVEL     GC log level for -Xlog:gc*=LEVEL. Default: debug
  -j, --java BIN            Java executable. Default: java
  -c, --collectors LIST     Comma-separated collectors.
                            Default: Serial,Parallel,G1,Shenandoah,ZGC
  -h, --help                Show this help

Examples:
  ./cli_tests/generate-gc-logs.sh -- ./target/demo.jar
  ./cli_tests/generate-gc-logs.sh -- ./cli_tests/GcPressureDemo.java --duration-sec 30
  ./cli_tests/generate-gc-logs.sh -l info -o /tmp/logs -- ./target/demo.jar 1000000
  ./cli_tests/generate-gc-logs.sh -c G1,ZGC -- ./target/demo.jar --size 256m
EOF
}

output_dir=./generated-gc-logs
log_level=debug
java_bin=java
collectors=Serial,Parallel,G1,Shenandoah,ZGC

while [ "$#" -gt 0 ]; do
    case "$1" in
        -o|--output-dir)
            output_dir=$2
            shift 2
            ;;
        -l|--log-level)
            log_level=$2
            shift 2
            ;;
        -j|--java)
            java_bin=$2
            shift 2
            ;;
        -c|--collectors)
            collectors=$2
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        --)
            shift
            break
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if [ "$#" -lt 1 ]; then
    echo "Workload path is required" >&2
    usage >&2
    exit 1
fi

workload_path=$1
shift

if [ ! -f "$workload_path" ]; then
    echo "Workload file does not exist: $workload_path" >&2
    exit 1
fi

case "$workload_path" in
    *.jar) workload_kind=jar ;;
    *.java) workload_kind=java-source ;;
    *)
        echo "Unsupported workload type: $workload_path" >&2
        echo "Expected either a .jar or a .java file" >&2
        exit 1
        ;;
esac

mkdir -p "$output_dir"

collector_flag() {
    case "$1" in
        Serial) printf '%s\n' "-XX:+UseSerialGC" ;;
        Parallel) printf '%s\n' "-XX:+UseParallelGC" ;;
        G1) printf '%s\n' "-XX:+UseG1GC" ;;
        Shenandoah) printf '%s\n' "-XX:+UseShenandoahGC" ;;
        ZGC) printf '%s\n' "-XX:+UseZGC" ;;
        *)
            echo "Unsupported collector: $1" >&2
            exit 1
            ;;
    esac
}

for collector in $(printf '%s\n' "$collectors" | tr ',' ' '); do
    collector_dir=$output_dir/$collector
    gc_log=$collector_dir/gc.log
    safepoint_log=$collector_dir/safepoint.log
    stdout_log=$collector_dir/stdout.log
    stderr_log=$collector_dir/stderr.log
    status_file=$collector_dir/exit-code.txt

    mkdir -p "$collector_dir"

    gc_flag=$(collector_flag "$collector")

    echo "Running $collector..."
    if [ "$workload_kind" = jar ]; then
        if "$java_bin" \
            "$gc_flag" \
            "-Xlog:gc*=$log_level:file=$gc_log:tags,uptime,level" \
            "-Xlog:safepoint*=$log_level:file=$safepoint_log:tags,uptime,level" \
            -jar "$workload_path" \
            "$@" \
            >"$stdout_log" \
            2>"$stderr_log"
        then
            printf '0\n' >"$status_file"
        else
            status=$?
            printf '%s\n' "$status" >"$status_file"
            echo "$collector run failed with exit code $status" >&2
        fi
    else
        if "$java_bin" \
            "$gc_flag" \
            "-Xlog:gc*=$log_level:file=$gc_log:tags,uptime,level" \
            "-Xlog:safepoint*=$log_level:file=$safepoint_log:tags,uptime,level" \
            "$workload_path" \
            "$@" \
            >"$stdout_log" \
            2>"$stderr_log"
        then
            printf '0\n' >"$status_file"
        else
            status=$?
            printf '%s\n' "$status" >"$status_file"
            echo "$collector run failed with exit code $status" >&2
        fi
    fi
done
