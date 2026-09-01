# [Deep Blue Documents](https://deepblue.lib.umich.edu/) 
## [DSpace](https://dspace.lyrasis.org/) backend services.
GitHub Actions [workflows](https://github.com/mlibrary/DSpace/actions) to produce Docker [images](https://github.com/orgs/mlibrary/packages?repo_name=DSpace) of DSpace 7.6 backend services.

| Dockerfile              | Image | Description                             |
|-------------------------|-------|-----------------------------------------|
| dependencies.dockerfile | dspace-dependencies | Dependencies for DSpace backend service |
| backend.dockerfile      | dspace-backend      | DSpace backend service                  |
| db.dockerfile           | dspace-db           | PostgreSQL database for DSpace          |
| solr.dockerfile         | dspace-solr         | Solr search engine for DSpace           |
| express.dockerfile      | dspace-express      | Express server for Prometheus metrics   |

## Building and running locally
The `compose.yml` file is configured for local development and testing (`docker-compose.yml` is the upstream DSpace docker compose file).
```shell
docker compose up -d
```
### With optional Prometheus metrics service

```shell
docker compose --profile optional up -d
```

### Service URLs
| URL                                     | Container | Comments                                     |
|-----------------------------------------|-----------|----------------------------------------------|
| jdbc:postgresql://localhost:5432/dspace | db        | PostgreSQL  (user: dspace, password: dspace) |
| http://localhost:8080/server            | backend   | Server API                                   |
| http://localhost:8983/solr              | solr      | Solr GUI                                     |
| http://localhost:3000/metrics           | express   | Prometheus metrics endpoint (optional)       |

### Notes
- The backend container exposes port **8000** (JDWP remote debugger — root `backend.dockerfile` for local dev only) and port **8009** (AJP connector). Neither is mapped in `docker-compose.yml` by default. Add a port mapping to `docker-compose.yml` if you need to attach a remote debugger locally.
- The `backend` service uses `depends_on` with `condition: service_healthy` for `db` and `solr`, and the `frontend` service waits for `backend` to be healthy, ensuring correct startup ordering without manual delays.
- **Backend configuration** is supplied entirely via `environment:` variables in `docker-compose.yml` (mirroring the Kubernetes ConfigMap pattern used in production). Key local-dev overrides: `plugin__P__sequence__P__org__P__dspace__P__authenticate__P__AuthenticationMethod` disables OIDC and enables password auth; `ip__P__bioIPsRange1` / `ip__P__bioIPsRange2` are set to non-routable CIDR placeholders (`192.0.2.0/24`) so that `OidcAuthenticationBean` — which calls `String.split()` on those properties unconditionally at startup — does not throw a `NullPointerException` on every `/server/api` request. In production/staging, real IP ranges and all other settings come from the Kubernetes ConfigMap.

## Integration Testing

A shell-based smoke test suite lives in [`tests/`](tests/). It requires only `bash` and `curl`.

### Quick run (stack already up)
```shell
bash tests/smoke.sh
```

### Full run (start → wait → test)
```shell
make test
```
This is equivalent to:
```shell
make up                     # docker compose up -d
bash tests/wait-for-stack.sh  # poll until backend/solr/frontend are ready
bash tests/smoke.sh           # run all assertions
```

### What is tested

| Layer            | Endpoint                           | Assertion                                                                  |
|------------------|------------------------------------|----------------------------------------------------------------------------|
| Backend REST API | `GET /server/api`                  | HTTP 200, HAL `_links` present                                             |
| Backend REST API | `GET /server/api`                  | `dspaceVersion` and `dspaceServer` fields present                          |
| Backend REST API | `GET /server/api/core/communities` | HTTP 200                                                                   |
| Backend REST API | `GET /server/api/core/collections` | HTTP 200                                                                   |
| Backend REST API | `GET /server/api/authn/status`     | HTTP 200, `"authenticated":false`                                          |
| Backend Actuator | `GET /server/actuator/health`      | `"status":"UP"` or `"UP_WITH_ISSUES"`                                      |
| Solr             | `GET /solr/admin/info/system`      | HTTP 200, version info present                                             |
| Solr             | `GET /solr/admin/cores`            | All four DSpace cores present (`authority`, `oai`, `search`, `statistics`) |
| Solr             | `GET /solr/search/admin/ping`      | HTTP 200                                                                   |
| Frontend         | `GET /`                            | HTTP 200, no `ng-error` boundary                                           |
| Frontend (SSR)   | `GET /communities/`                | HTTP 200, `ds-root` element present, `DSpace` title present                |

### CI (GitHub Actions)
The workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) is the primary CI workflow. It runs automatically on:
- **Direct pushes to `main`** — validates the branch after a merge.
- **Pull requests targeting `main`** — validates every push to a PR branch before it lands.
