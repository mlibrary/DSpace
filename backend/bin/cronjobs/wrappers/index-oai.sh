#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  "$DSPACE_BIN_DIR/dspace" oai import
}

run_job index-oai normal "$@"
