#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/aptrust/report-aptrust-status"
}

run_job report-aptrust-status anomaly "$@"
