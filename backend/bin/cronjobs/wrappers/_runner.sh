#!/bin/bash

set -uo pipefail

run_job() {
  local job=$1 mode=$2
  shift 2
  if [[ $# -ne 1 ]]; then
    printf 'Usage: bash %s.sh ENVIRONMENT\n' "$job" >&2
    exit 64
  fi
  export CRON_ENVIRONMENT=$1 CRON_WRAPPER=1
  case "$CRON_ENVIRONMENT" in
    production|staging|demo|workshop|preview|testing) ;;
    *) printf 'Unknown environment: %s\n' "$CRON_ENVIRONMENT" >&2; exit 64 ;;
  esac

  case "$job:$CRON_ENVIRONMENT" in
    index-discovery:production|index-discovery:staging|index-discovery:demo|index-discovery:workshop|\
    index-oai:production|index-oai:staging|index-oai:demo|index-oai:workshop|\
    filter-media:staging|dspace-checksum-checker:staging|checksum-checker:staging|doi-process:staging|\
    *:production) ;;
    *)
      printf '%s is not configured in %s; refusing to run.\n' "$job" "$CRON_ENVIRONMENT" >&2
      exit 64
      ;;
  esac

  DSPACE_BIN_DIR=${DSPACE_BIN_DIR:-/dspace/bin}
  local recipient=${CRON_EMAIL_TO:-ulib-deepblue-documents-cron-reporting@umich.edu}
  umask 077
  cron_output=$(mktemp "${TMPDIR:-/tmp}/deepblue-cron.XXXXXXXX") || exit 74
  trap 'rm -f -- "$cron_output"' EXIT
  trap 'exit 130' INT
  trap 'exit 143' TERM

  printf '[%s] %s started\n' "$CRON_ENVIRONMENT" "$job" >&2
  job_command 2>&1 | tee -a /dev/stderr | capture_output "$mode" > "$cron_output"
  local statuses=("${PIPESTATUS[@]}")
  local result=${statuses[0]} state=FAILED
  if (( statuses[1] != 0 || statuses[2] != 0 )); then
    printf '[%s] %s: output capture failed (tee=%s, capture=%s)\n' \
      "$CRON_ENVIRONMENT" "$job" "${statuses[1]}" "${statuses[2]}" >&2
    (( result != 0 )) || result=74
  fi
  printf '[%s] %s finished (exit %s)\n' "$CRON_ENVIRONMENT" "$job" "$result" >&2

  if (( result == 0 )); then
    [[ $mode == report ]] || exit 0
    state=REPORT
  elif [[ $mode == anomaly && $result == 10 ]]; then
    state=ANOMALY
  fi

  {
    printf 'Environment: %s\nJob: %s\nResult: %s (exit %s)\n\n' \
      "$CRON_ENVIRONMENT" "$job" "$state" "$result"
    if [[ $mode != report ]]; then
      printf 'Last 65536 bytes of combined stdout/stderr (full output is in container logs):\n\n'
    fi
    cat "$cron_output"
  } | "$DSPACE_BIN_DIR/cronjobs/perl_mailer" \
    -s "$CRON_ENVIRONMENT: $job $state" -e "$recipient"
  local mail_status=$?
  if (( mail_status != 0 )); then
    printf '[%s] %s: email delivery failed (exit %s)\n' \
      "$CRON_ENVIRONMENT" "$job" "$mail_status" >&2
    (( result != 0 )) || result=75
  fi
  exit "$result"
}

capture_output() {
  if [[ $1 == report ]]; then
    cat
  else
    tail -c 65536
  fi
}
