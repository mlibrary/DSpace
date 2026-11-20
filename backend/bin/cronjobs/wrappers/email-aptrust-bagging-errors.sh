#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/aptrust/email-aptrust-bagging-errors"
}

run_job email-aptrust-bagging-errors anomaly "$@"
