#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/cronjobs/check-checksum"
}

run_job checksum-checker anomaly "$@"
