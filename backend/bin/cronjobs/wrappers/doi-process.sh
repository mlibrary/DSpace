#!/bin/bash
source "$(dirname -- "${BASH_SOURCE[0]}")/_runner.sh"

job_command() {
  local operation status result=0
  local args
  for operation in -u -s -r -d; do
    args=(doi-organiser "$operation")
    [[ $CRON_ENVIRONMENT != staging ]] || args+=(-q)
    "$DSPACE_BIN_DIR/dspace" "${args[@]}"
    status=$?
    if (( status != 0 )); then
      printf 'doi-organiser %s failed (exit %s)\n' "$operation" "$status" >&2
      (( result != 0 )) || result=$status
    fi
  done
  return "$result"
}

run_job doi-process normal "$@"
