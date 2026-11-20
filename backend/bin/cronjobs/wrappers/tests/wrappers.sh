#!/bin/bash
set -euo pipefail

wrappers=$(cd "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
work=$(mktemp -d)
trap 'rm -rf -- "$work"' EXIT
export DSPACE_BIN_DIR="$work/bin" TMPDIR="$work/tmp" MOCK_DIR="$work"
mkdir -p "$DSPACE_BIN_DIR/cronjobs" "$DSPACE_BIN_DIR/aptrust" "$TMPDIR"

cat > "$work/command" <<'MOCK'
#!/bin/bash
printf '%s %s\n' "$(basename "$0")" "$*" >> "$MOCK_DIR/commands"
printf 'stdout from %s\n' "$(basename "$0")"
printf 'stderr from %s\n' "$(basename "$0")" >&2
printf 'environment=%s wrapper=%s\n' "$CRON_ENVIRONMENT" "$CRON_WRAPPER"
case ${MOCK_SCENARIO:-success} in
  failure) exit 7 ;;
  anomaly) printf 'domain anomaly\n'; exit 10 ;;
  filter-error) printf 'ERROR filtering item 2027.42/example\n'; exit 0 ;;
  doi-failure) [[ $* != 'doi-organiser -u'* ]] || exit 9 ;;
  long-output|long-report|early-filter-error)
    if [[ $MOCK_SCENARIO == early-filter-error ]]; then
      printf 'ERROR filtering item 2027.42/example\n'
    fi
    awk 'BEGIN { for (i = 0; i < 10000; i++) print "a long output line" }'
    printf 'end-of-output\n'
    [[ $MOCK_SCENARIO != long-output ]] || exit 8
    ;;
esac
exit 0
MOCK
chmod +x "$work/command"
for command in dspace monthly_report cronjobs/check-checksum cronjobs/report-double-original \
  cronjobs/report-too-many-authors cronjobs/report-about-dearborn-items \
  cronjobs/filter-media-cronjob aptrust/email-aptrust-bagging-errors \
  aptrust/move-to-aptrust-regular aptrust/report-aptrust-status; do
  cp "$work/command" "$DSPACE_BIN_DIR/$command"
done
cat > "$DSPACE_BIN_DIR/cronjobs/perl_mailer" <<'MOCK'
#!/bin/bash
printf '%s\n' "$*" >> "$MOCK_DIR/mail-args"
cat >> "$MOCK_DIR/mail-body"
exit "${MOCK_MAIL_STATUS:-0}"
MOCK
chmod +x "$DSPACE_BIN_DIR/cronjobs/perl_mailer"

fail() { printf 'FAIL: %s\n' "$*" >&2; exit 1; }
contains() { grep -Fq -- "$2" "$1" || fail "$1 does not contain $2"; }
run() {
  local expected=$1 job=$2 environment=$3 status=0
  rm -f "$work/mail-args" "$work/mail-body" "$work/commands"
  bash "$wrappers/$job.sh" "$environment" > "$work/log" 2>&1 || status=$?
  [[ $status == "$expected" ]] || { cat "$work/log" >&2; fail "$job: expected $expected, got $status"; }
  [[ -z $(find "$TMPDIR" -type f -print) ]] || fail 'temporary capture leaked'
}

for script in "$wrappers"/*.sh "$wrappers/tests/wrappers.sh"; do bash -n "$script"; done

count=0
for script in "$wrappers"/*.sh; do
  job=$(basename "$script" .sh)
  [[ $job != _runner ]] || continue
  count=$((count + 1))
  export MOCK_SCENARIO=success
  run 0 "$job" production
  contains "$work/log" 'stdout from'
  contains "$work/log" 'stderr from'
  contains "$work/log" 'environment=production wrapper=1'
  contains "$work/log" "[production] $job started"
  if [[ $job == monthly-report ]]; then
    contains "$work/mail-args" 'production: monthly-report REPORT'
  else
    [[ ! -e $work/mail-args ]] || fail "$job mailed on success"
  fi

  export MOCK_SCENARIO=failure
  run 7 "$job" production
  contains "$work/mail-args" "production: $job FAILED"
  contains "$work/mail-body" 'stderr from'
  [[ $(wc -l < "$work/mail-args") -eq 1 ]] || fail "$job sent duplicate mail"
done
[[ $count == 15 ]] || fail "expected 15 wrappers, got $count"

export MOCK_SCENARIO=success
for environment in staging demo workshop preview testing; do
  run 0 index-discovery "$environment"
  contains "$work/commands" 'dspace index-discovery'
  [[ ! -e $work/mail-args ]] || fail "index-discovery mailed on success in $environment"
  run 0 index-oai "$environment"
  contains "$work/commands" 'dspace oai import'
  [[ ! -e $work/mail-args ]] || fail "index-oai mailed on success in $environment"
done
for environment in staging demo workshop preview testing custom-environment; do
  for script in "$wrappers"/*.sh; do
    job=$(basename "$script" .sh)
    [[ $job != _runner ]] || continue
    run 0 "$job" "$environment"
    contains "$work/log" "environment=$environment wrapper=1"
    [[ -s $work/commands ]] || fail "$job did not run in $environment"
  done
done
run 0 filter-media testing
contains "$work/commands" 'dspace filter-media'
run 0 checksum-checker preview
contains "$work/commands" 'check-checksum'
bash -c 'source "$1"; job_command() { echo invoked; }; run_job new-aptrust-task normal testing' \
  _ "$wrappers/_runner.sh" > "$work/log" 2>&1
contains "$work/log" 'invoked'
bash -c 'source "$1"; job_command() { echo invoked; }; run_job new-maintenance-task normal testing' \
  _ "$wrappers/_runner.sh" > "$work/log" 2>&1
contains "$work/log" 'invoked'
export MOCK_SCENARIO=failure
for job in index-discovery index-oai; do
  run 7 "$job" testing
  contains "$work/mail-args" "testing: $job FAILED"
  contains "$work/mail-body" 'stderr from'
done
export MOCK_SCENARIO=success
run 0 index-oai staging
contains "$work/commands" 'dspace oai import'
run 0 sub-weekly production
contains "$work/commands" 'dspace subscription-send -f W'
run 0 sub-monthly production
contains "$work/commands" 'dspace subscription-send -f M'
run 0 dspace-checksum-checker staging
contains "$work/commands" 'dspace checker -d 1h -p'
run 0 filter-media staging
contains "$work/commands" 'dspace filter-media'
run 0 filter-media production
contains "$work/commands" 'filter-media-cronjob'

export MOCK_SCENARIO=anomaly
for job in checksum-checker report-double-original report-too-many-authors \
  email-aptrust-bagging-errors report-aptrust-status; do
  run 10 "$job" production
  contains "$work/mail-args" "production: $job ANOMALY"
done
run 10 checksum-checker staging
contains "$work/mail-args" 'staging: checksum-checker ANOMALY'
run 10 index-discovery staging
contains "$work/mail-args" 'staging: index-discovery FAILED'

export MOCK_SCENARIO=filter-error
run 10 filter-media staging
contains "$work/mail-args" 'staging: filter-media ANOMALY'
export MOCK_SCENARIO=early-filter-error
run 10 filter-media staging
contains "$work/log" 'ERROR filtering item'
contains "$work/mail-body" 'end-of-output'

export MOCK_SCENARIO=doi-failure
run 9 doi-process staging
contains "$work/commands" 'dspace doi-organiser -u -q'
contains "$work/commands" 'dspace doi-organiser -d -q'
[[ $(wc -l < "$work/commands") -eq 4 ]] || fail 'DOI did not run all operations'
run 9 doi-process production
contains "$work/commands" 'dspace doi-organiser -d'
if grep -Fq -- '-q' "$work/commands"; then fail 'production DOI added -q'; fi

export MOCK_SCENARIO=long-output
run 8 index-discovery staging
contains "$work/log" 'stdout from'
contains "$work/mail-body" 'end-of-output'
[[ $(wc -c < "$work/mail-body") -lt 66000 ]] || fail 'notification output is unbounded'

export MOCK_SCENARIO=long-report
run 0 monthly-report production
contains "$work/mail-body" 'stdout from'
contains "$work/mail-body" 'end-of-output'
[[ $(wc -c < "$work/mail-body") -gt 65536 ]] || fail 'monthly report was truncated'

mkdir "$work/tools"
cat > "$work/tools/tee" <<'MOCK'
#!/bin/bash
cat > /dev/null
exit 3
MOCK
chmod +x "$work/tools/tee"
export MOCK_SCENARIO=success
PATH="$work/tools:$PATH" run 74 index-discovery staging
contains "$work/log" 'output capture failed'
contains "$work/mail-args" 'staging: index-discovery FAILED'

export MOCK_SCENARIO=success MOCK_MAIL_STATUS=4
run 75 monthly-report production
contains "$work/log" 'email delivery failed'
export MOCK_SCENARIO=failure
run 7 index-discovery staging
contains "$work/log" 'email delivery failed'
unset MOCK_MAIL_STATUS

export MOCK_SCENARIO=success CRON_EMAIL_TO=cron-test@example.invalid
run 0 monthly-report production
contains "$work/mail-args" 'cron-test@example.invalid'
unset CRON_EMAIL_TO
status=0
bash "$wrappers/index-oai.sh" > "$work/log" 2>&1 || status=$?
[[ $status == 64 ]] || fail 'missing environment was accepted'
status=0
bash "$wrappers/index-oai.sh" "" > "$work/log" 2>&1 || status=$?
[[ $status == 64 ]] || fail 'empty environment was accepted'
status=0
bash "$wrappers/index-oai.sh" staging extra > "$work/log" 2>&1 || status=$?
[[ $status == 64 ]] || fail 'extra arguments were accepted'

printf 'All wrapper checks passed (%s job wrappers).\n' "$count"
