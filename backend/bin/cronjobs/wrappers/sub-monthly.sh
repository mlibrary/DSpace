#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/dspace" subscription-send -f M
}

run_job sub-monthly normal "$@"
