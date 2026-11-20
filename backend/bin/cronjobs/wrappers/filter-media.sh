#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

filter_command() {
  if [[ $CRON_ENVIRONMENT == production ]]; then
    "$DSPACE_BIN_DIR/cronjobs/filter-media-cronjob"
  else
    "$DSPACE_BIN_DIR/dspace" filter-media
  fi
}

job_command() {
  # DSpace may report a per-item extraction failure while exiting zero.
  filter_command 2>&1 | awk '/ERROR filtering/ { failed = 1 } { print } END { exit (failed ? 10 : 0) }'
  local statuses=("${PIPESTATUS[@]}")
  (( statuses[0] == 0 )) || return "${statuses[0]}"
  return "${statuses[1]}"
}

run_job filter-media anomaly "$@"
