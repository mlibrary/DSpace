#!/usr/bin/env bash
#
# run_docker_test.sh - Runs the APTrust dry-run tests against a PostgreSQL instance in Docker.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

CONTAINER_NAME="aptrust-test-postgres-$$"
NETWORK_NAME="aptrust-test-net-$$"
STARTED_CONTAINER=0

cleanup() {
    if [ "$STARTED_CONTAINER" -eq 1 ]; then
        echo "--> Tearing down test PostgreSQL container ($CONTAINER_NAME)..."
        docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
        docker network rm "$NETWORK_NAME" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT INT TERM

# Check if Docker is available
if ! command -v docker >/dev/null 2>&1; then
    echo "ERROR: Docker is required to run this test." >&2
    exit 1
fi

# Determine if we should connect to an existing Postgres or spin up an ephemeral container
DB_SERVICE="${DB_SERVICE:-}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-dspace}"
DB_USER="${DB_USER:-dspace}"
DB_PASSWORD="${DB_PASSWORD:-dspace}"

if [ -z "$DB_SERVICE" ]; then
    echo "--> Starting ephemeral PostgreSQL container ($CONTAINER_NAME)..."
    docker network create "$NETWORK_NAME" >/dev/null

    docker run -d \
        --name "$CONTAINER_NAME" \
        --network "$NETWORK_NAME" \
        -e POSTGRES_DB="$DB_NAME" \
        -e POSTGRES_USER="$DB_USER" \
        -e POSTGRES_PASSWORD="$DB_PASSWORD" \
        postgres:15-alpine >/dev/null

    STARTED_CONTAINER=1

    echo "--> Waiting for PostgreSQL to be ready..."
    until docker exec "$CONTAINER_NAME" pg_isready -U "$DB_USER" -d "$DB_NAME" >/dev/null 2>&1; do
        sleep 0.5
    done
    echo "--> PostgreSQL is ready."

    echo "--> Running dry-run test suite inside Perl 5.40 container..."
    docker run --rm \
        --network "$NETWORK_NAME" \
        -v "$REPO_ROOT:/workspace" \
        -w /workspace \
        -e DB_SERVICE="$CONTAINER_NAME" \
        -e DB_PORT="5432" \
        -e DB_NAME="$DB_NAME" \
        -e DB_USER="$DB_USER" \
        -e DB_PASSWORD="$DB_PASSWORD" \
        perl:5.40-slim sh -c '
            apt-get update -qq && apt-get install -y -qq libdbi-perl libdbd-pg-perl libjson-xs-perl libwww-perl >/dev/null 2>&1
            /usr/bin/perl backend/test/aptrust/dry_run_test.t
        '
else
    echo "--> Running dry-run test suite against existing Postgres at $DB_SERVICE:$DB_PORT..."
    DB_SERVICE="$DB_SERVICE" \
    DB_PORT="$DB_PORT" \
    DB_NAME="$DB_NAME" \
    DB_USER="$DB_USER" \
    DB_PASSWORD="$DB_PASSWORD" \
    perl "$SCRIPT_DIR/dry_run_test.t"
fi

echo "--> APTrust dry-run tests completed successfully."
