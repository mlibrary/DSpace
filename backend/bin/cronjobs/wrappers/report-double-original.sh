#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/cronjobs/report-double-original"
}

run_job report-double-original anomaly "$@"
