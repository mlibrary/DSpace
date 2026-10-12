# APTrust Perl Scripts Test Suite

This test suite verifies the APTrust deposit workflows (`backend/bin/aptrust/move-to-aptrust-regular` and `backend/bin/aptrust/move-to-aptrust-retry`), specifically verifying that items missing titles are skipped from APTrust ingest and recorded in the `aptrust_bagging_error` table.

## Contents

- `dry_run_test.t`: Standard Perl Test::More test suite verifying:
  - `ItemHasTitle` logic (handles with valid title, missing title, whitespace-only title, non-existent handle).
  - `aptrust-info.txt` `Title:` field validation regex.
  - End-to-end dry-run execution of `move-to-aptrust-regular -d` against PostgreSQL.
  - End-to-end dry-run execution of `move-to-aptrust-retry -d` against PostgreSQL.
  - Verification that skipped items are recorded into `aptrust_bagging_error` and not written to `aptrust_bags_sent`.
- `run_docker_test.sh`: Automated test runner script.

## Running the Tests

### Option A: Using Docker (One Command, Recommended)

Runs the test against an ephemeral PostgreSQL Docker container and Perl 5.40 environment:

```bash
./backend/test/aptrust/run_docker_test.sh
```

### Option B: Against an Existing PostgreSQL Instance

If you already have a PostgreSQL instance running (e.g. via `docker compose up -d db`):

```bash
DB_SERVICE=localhost DB_PORT=5432 DB_NAME=dspace DB_USER=dspace DB_PASSWORD=dspace perl backend/test/aptrust/dry_run_test.t
```
