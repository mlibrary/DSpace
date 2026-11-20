#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/dspace" checker -d 1h -p
}

run_job dspace-checksum-checker normal "$@"
