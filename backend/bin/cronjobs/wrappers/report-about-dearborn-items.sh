#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/cronjobs/report-about-dearborn-items"
}

run_job report-about-dearborn-items normal "$@"
