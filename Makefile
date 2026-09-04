.PHONY: build ensure-source up up-all down clean rebuild logs wait test help

## Default target
.DEFAULT_GOAL := help

## Build the dependencies image and all compose service images.
## Run this on first setup and whenever the source changes.
build:
	docker build -f dependencies.dockerfile -t dspace-dependencies:main .
	docker compose -f compose.yml build

## Build the source image only when it is not already present locally.
## Called automatically by 'up' so you can never accidentally start with a missing source image.
ensure-dependencies:
	@docker image inspect dspace-dependencies:main > /dev/null 2>&1 \
	  && echo "Dependencies image already exists; skipping build." \
	  || (echo "Dependencies image not found; building now..." && \
	      docker build -f dependencies.dockerfile -t dspace-dependencies:main .)

## Start the core services (db, solr, backend, frontend) in the background.
## Builds the source image first if it is not already present.
up: ensure-dependencies
	docker compose -f compose.yml up -d

## Stop and remove containers (volumes are preserved).
down:
	docker compose -f compose.yml down

## Stop and remove containers AND all named volumes (full clean: destroys data).
clean:
	docker compose -f compose.yml down -v --rmi local
	docker rmi -f dspace-dependencies 2>/dev/null || true

## Rebuild all images from scratch and restart core services.
rebuild: clean build up

## Show logs for all running services (Ctrl-C to exit).
logs:
	docker compose -f compose.yml logs -f

## Wait for all core services to be healthy (backend, solr, frontend).
## Polls every 5 s; times out after MAX_WAIT seconds (default 300).
wait:
	@bash tests/wait-for-stack.sh

## Run the smoke-test suite against the running local stack.
## Ensures the dependencies image exists, starts the stack, waits for readiness, then runs tests.
test: up wait
	@bash tests/smoke.sh

## Show this help message.
help:
	@echo ""
	@echo "dspace-containerization: local dev Makefile"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "  build                Build dependencies image + all compose service images"
	@echo "  ensure-dependencies  Build dependencies image only if not already present"
	@echo "  up                   Start services (express, db, solr, backend, frontend)"
	@echo "  down                 Stop containers (volumes preserved)"
	@echo "  clean                Stop containers and delete volumes + images"
	@echo "  rebuild              Full clean, build, and up"
	@echo "  logs                 Tail logs for all services"
	@echo "  wait                 Wait for all services to be healthy"
	@echo "  test                 Ensure dependencies, start stack, wait, run smoke tests"
	@echo "  help                 Show this message"
	@echo ""
