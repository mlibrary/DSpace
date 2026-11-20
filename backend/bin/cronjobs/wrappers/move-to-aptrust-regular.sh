#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/aptrust/move-to-aptrust-regular"
}

run_job move-to-aptrust-regular normal "$@"
