#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/cronjobs/report-too-many-authors"
}

run_job report-too-many-authors anomaly "$@"
