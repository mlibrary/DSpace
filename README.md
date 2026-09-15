# Deep Blue Documents Backend Services

[Deep Blue Documents](https://deepblue.lib.umich.edu/) is the University of Michigan's institutional repository for scholarly works.

This repository contains the backend services: DSpace backend service, PostgreSQL database, Solr search engine, and Express metrics endpoint.

The frontend service is in the [mlibrary/dspace-angular](https://github.com/mlibrary/dspace-angular) repository.

## Workflows to build GitHub Packages

| Workflow              | Package | Description                                   |
|-------------------------|-------|-----------------------------------------------|
| Build dspace-dependencies image | dspace/dspace-dependencies | Dependencies for DSpace backend service       |
| Build dspace-backend image      | dspace/dspace-backend      | DSpace backend service                        |
| Build dspace-db image           | dspace/dspace-db           | PostgreSQL database for DSpace                |
| Build dspace-solr image         | dspace/dspace-solr         | Solr search engine for DSpace                 |
| Build dspace-express image      | dspace/dspace-express      | Express server for Prometheus metrics         |

NOTE: The `dspace/dspace-backend` image is dependent on the `dspace/dspace-dependencies` image, which is built first and cached for reuse.
## Local Production Sandbox

Using Docker Compose, you can build and run the backend services locally to simulate a production environment.

The `dspace-backend` image is dependent on the `dspace-dependencies` image, which is built first and cached for reuse.

```shell
docker build -f dependencies.dockerfile -t dspace-dependencies .
```
The backend services can then be built and run.
```shell
docker compose up -d
```
Then the database migrations can be applied to the local database.
```shell
docker compose exec backend /dspace/bin/dspace database migrate
```

### Services Endpoints
| URL                                     | Container | Comments                                                                     |
|-----------------------------------------|-----------|------------------------------------------------------------------------------|
| http://localhost:8080/server        | backend   | The HAL Browser                                                                   |
| http://localhost:8080/server/api        | backend   | Server API                                                                   |
| http://localhost:8080/rest              | backend   | REST (Deprecated) Used by Perl Scripts                                       |
| http://localhost:8983/solr              | solr      | Solr GUI                                                                     |
| jdbc:postgresql://localhost:5432/dspace | db        | PostgreSQL  (database: dspace, user: dspace, password: dspace)                                 |
| http://localhost:3000/metrics           | express   | Metrics endpoint (Prometheus)                                       |
