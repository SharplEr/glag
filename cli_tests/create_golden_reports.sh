#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# shellcheck source=report_helpers.sh
. "$script_dir/report_helpers.sh"

generated_logs_dir="$work_dir/.generated_gc_logs"
workload_java_options=${GLAG_WORKLOAD_JAVA_TOOL_OPTIONS:-"-Xms256m -Xmx256m"}
log_level=${GLAG_LOG_LEVEL:-debug}
duration_sec=${GLAG_WORKLOAD_DURATION_SEC:-12}
max_size=${GLAG_WORKLOAD_MAX_SIZE:-2000}
add_count=${GLAG_WORKLOAD_ADD_COUNT:-500}
min_block_size=${GLAG_WORKLOAD_MIN_BLOCK_SIZE:-4096}
max_block_size=${GLAG_WORKLOAD_MAX_BLOCK_SIZE:-32768}
report_every=${GLAG_WORKLOAD_REPORT_EVERY:-1000}

require_cli_jar
mkdir -p "$work_dir" "$generated_logs_dir"

echo "Generating GC and safepoint logs into $generated_logs_dir"
env JAVA_TOOL_OPTIONS="$workload_java_options" "$script_dir/generate-gc-logs.sh" \
    -o "$generated_logs_dir" \
    -l "$log_level" \
    -j "$java_bin" \
    -c "$(printf '%s' "$collectors" | tr ' ' ',')" \
    -- "$script_dir/GcPressureDemo.java" \
    --duration-sec "$duration_sec" \
    --max-size "$max_size" \
    --add-count "$add_count" \
    --min-block-size "$min_block_size" \
    --max-block-size "$max_block_size" \
    --report-every "$report_every"

for collector in $collectors; do
    source_dir="$generated_logs_dir/$collector"
    target_logs_dir=$(gc_logs_dir "$collector")

    require_zero_status_file "$source_dir/exit-code.txt" "$collector workload"
    require_non_empty_file "$source_dir/gc.log"
    require_non_empty_file "$source_dir/safepoint.log"

    mkdir -p "$target_logs_dir"
    cp "$source_dir/gc.log" "$target_logs_dir/gc.log"
    cp "$source_dir/safepoint.log" "$target_logs_dir/safepoint.log"
    cp "$source_dir/stdout.log" "$target_logs_dir/stdout.log"
    cp "$source_dir/stderr.log" "$target_logs_dir/stderr.log"
    cp "$source_dir/exit-code.txt" "$target_logs_dir/exit-code.txt"

    check_collector_logs "$collector"

    echo "Creating golden reports for $collector"
    create_reports "$collector" golden_reports
    check_report_files "$collector" golden_reports
done

echo "Golden reports are ready in $work_dir"
