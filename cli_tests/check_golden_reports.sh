#!/bin/sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
# shellcheck source=report_helpers.sh
. "$script_dir/report_helpers.sh"

require_cli_jar

compare_report() {
    collector=$1
    file_name=$2
    golden_file=$(reports_dir "$collector" golden_reports)/"$file_name"
    candidate_file=$(reports_dir "$collector" candidates)/"$file_name"

    require_non_empty_file "$golden_file"
    require_non_empty_file "$candidate_file"

    if ! cmp -s "$golden_file" "$candidate_file"; then
        echo "Report mismatch: $collector/$file_name" >&2
        diff -u "$golden_file" "$candidate_file" | sed -n '1,200p' >&2 || true
        fail "candidate report differs from golden report"
    fi
}

for collector in $collectors; do
    check_collector_logs "$collector"
    check_report_files "$collector" golden_reports

    echo "Creating candidate reports for $collector"
    create_reports "$collector" candidates
    check_report_files "$collector" candidates

    compare_report "$collector" full.md
    compare_report "$collector" full.html
    compare_report "$collector" safepoints.md
    compare_report "$collector" safepoints.html
    compare_report "$collector" console.txt
done

echo "Golden reports check passed"
