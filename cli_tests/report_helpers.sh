#!/bin/sh

set -eu

collectors=${GLAG_COLLECTORS:-"Serial Parallel G1 Shenandoah ZGC"}
java_bin=${JAVA_BIN:-java}
threshold_ms=${GLAG_THRESHOLD_MS:-50}
examples=${GLAG_EXAMPLES:-5}

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
repo_root=$(CDPATH= cd -- "$script_dir/.." && pwd)
work_dir=${GLAG_CLI_TESTS_WORK_DIR:-"$script_dir/work"}
cli_jar=${GLAG_CLI_JAR:-"$repo_root/cli/target/glag-1.0-SNAPSHOT-jar-with-dependencies.jar"}

fail() {
    echo "ERROR: $*" >&2
    exit 1
}

require_file() {
    if [ ! -f "$1" ]; then
        fail "file does not exist: $1"
    fi
}

require_non_empty_file() {
    require_file "$1"
    if [ ! -s "$1" ]; then
        fail "file is empty: $1"
    fi
}

require_zero_status_file() {
    require_file "$1"
    status=$(cat "$1")
    if [ "$status" != 0 ]; then
        fail "$2 failed with exit code $status"
    fi
}

require_cli_jar() {
    require_non_empty_file "$cli_jar"
}

collector_root() {
    printf '%s/%s\n' "$work_dir" "$1"
}

gc_logs_dir() {
    printf '%s/gc_logs\n' "$(collector_root "$1")"
}

reports_dir() {
    printf '%s/%s\n' "$(collector_root "$1")" "$2"
}

check_collector_logs() {
    collector=$1
    logs_dir=$(gc_logs_dir "$collector")
    require_non_empty_file "$logs_dir/gc.log"
    require_non_empty_file "$logs_dir/safepoint.log"
}

run_glag_report() {
    safepoint_log=$1
    gc_log=$2
    output_file=$3
    status_file=$4
    stdout_file=$5
    stderr_file=$6

    rm -f "$output_file" "$status_file" "$stdout_file" "$stderr_file"

    if [ "$gc_log" = "-" ]; then
        if "$java_bin" -jar "$cli_jar" \
            -s "$safepoint_log" \
            -t "$threshold_ms" \
            -o "$output_file" \
            >"$stdout_file" \
            2>"$stderr_file"
        then
            status=0
        else
            status=$?
        fi
    else
        if "$java_bin" -jar "$cli_jar" \
            -s "$safepoint_log" \
            -g "$gc_log" \
            -t "$threshold_ms" \
            --examples "$examples" \
            -o "$output_file" \
            >"$stdout_file" \
            2>"$stderr_file"
        then
            status=0
        else
            status=$?
        fi
    fi

    printf '%s\n' "$status" >"$status_file"
    if [ "$status" != 0 ]; then
        fail "glag failed with exit code $status; see $stderr_file"
    fi
    require_non_empty_file "$output_file"
}

run_glag_console() {
    safepoint_log=$1
    output_file=$2
    status_file=$3
    stderr_file=$4

    rm -f "$output_file" "$status_file" "$stderr_file"

    if "$java_bin" -jar "$cli_jar" \
        -s "$safepoint_log" \
        -t "$threshold_ms" \
        >"$output_file" \
        2>"$stderr_file"
    then
        status=0
    else
        status=$?
    fi

    printf '%s\n' "$status" >"$status_file"
    if [ "$status" != 0 ]; then
        fail "glag console output failed with exit code $status; see $stderr_file"
    fi
    require_non_empty_file "$output_file"
}

create_reports() {
    collector=$1
    target_name=$2
    logs_dir=$(gc_logs_dir "$collector")
    target_dir=$(reports_dir "$collector" "$target_name")

    check_collector_logs "$collector"
    mkdir -p "$target_dir"

    run_glag_report \
        "$logs_dir/safepoint.log" \
        "$logs_dir/gc.log" \
        "$target_dir/full.md" \
        "$target_dir/full.md.exit-code.txt" \
        "$target_dir/full.md.stdout.log" \
        "$target_dir/full.md.stderr.log"

    run_glag_report \
        "$logs_dir/safepoint.log" \
        "$logs_dir/gc.log" \
        "$target_dir/full.html" \
        "$target_dir/full.html.exit-code.txt" \
        "$target_dir/full.html.stdout.log" \
        "$target_dir/full.html.stderr.log"

    run_glag_report \
        "$logs_dir/safepoint.log" \
        - \
        "$target_dir/safepoints.md" \
        "$target_dir/safepoints.md.exit-code.txt" \
        "$target_dir/safepoints.md.stdout.log" \
        "$target_dir/safepoints.md.stderr.log"

    run_glag_report \
        "$logs_dir/safepoint.log" \
        - \
        "$target_dir/safepoints.html" \
        "$target_dir/safepoints.html.exit-code.txt" \
        "$target_dir/safepoints.html.stdout.log" \
        "$target_dir/safepoints.html.stderr.log"

    run_glag_console \
        "$logs_dir/safepoint.log" \
        "$target_dir/console.txt" \
        "$target_dir/console.txt.exit-code.txt" \
        "$target_dir/console.txt.stderr.log"
}

check_report_files() {
    collector=$1
    target_name=$2
    target_dir=$(reports_dir "$collector" "$target_name")

    require_non_empty_file "$target_dir/full.md"
    require_non_empty_file "$target_dir/full.html"
    require_non_empty_file "$target_dir/safepoints.md"
    require_non_empty_file "$target_dir/safepoints.html"
    require_non_empty_file "$target_dir/console.txt"

    require_zero_status_file "$target_dir/full.md.exit-code.txt" "$collector full.md"
    require_zero_status_file "$target_dir/full.html.exit-code.txt" "$collector full.html"
    require_zero_status_file "$target_dir/safepoints.md.exit-code.txt" "$collector safepoints.md"
    require_zero_status_file "$target_dir/safepoints.html.exit-code.txt" "$collector safepoints.html"
    require_zero_status_file "$target_dir/console.txt.exit-code.txt" "$collector console.txt"
}
