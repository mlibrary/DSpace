# Deep Blue Documents Backend Services

[Deep Blue Documents](https://deepblue.lib.umich.edu/documents) is the University of Michigan's institutional repository for scholarly works.

This repository contains the backend services: DSpace backend service, PostgreSQL database, Solr search engine, and Express metrics-service.

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
Then backend services can then be built and run.
```shell
docker compose up -d
```
Then the database migrations can be applied to the local database.
```shell
docker compose exec backend /dspace/bin/dspace database migrate
```
Then you may make yourself an admin user.
```shell
docker compose exec backend /dspace/bin/dspace create-administrator
```
Example:
```terminaloutput
gkostin@m-hwf73k3946 DSpace % docker compose exec backend /dspace/bin/dspace create-administrator
WARN[0000] Found multiple config files with supported names: /Users/gkostin/GitHub/mlibrary/DSpace/compose.yml, /Users/gkostin/GitHub/mlibrary/DSpace/docker-compose.yml
WARN[0000] Using /Users/gkostin/GitHub/mlibrary/DSpace/compose.yml
WARN[0000] Found multiple config files with supported names: /Users/gkostin/GitHub/mlibrary/DSpace/compose.yml, /Users/gkostin/GitHub/mlibrary/DSpace/docker-compose.yml
WARN[0000] Using /Users/gkostin/GitHub/mlibrary/DSpace/compose.yml
Creating an initial administrator account
E-mail address: gkostin@umich.edu
First name: Greg
Last name: Kostin
Is the above data correct? (y or n): y
Password will not display on screen.
Password:
Again to confirm:
Administrator account created
gkostin@m-hwf73k3946 DSpace %
```
Then you may log in to the backend service using the HAL Browser at http://localhost:8080/server.

Or the frontend service at http://localhost:4000/login (see [mlibrary/dspace-angular](https://github.com/mlibrary/dspace-angular)#[Local Production Sandbox](https://github.com/mlibrary/dspace-angular#local-production-sandbox)).
### Services Endpoints
| URL                                     | Container | Comments                                                                     |
|-----------------------------------------|-----------|------------------------------------------------------------------------------|
| http://localhost:8080/server        | backend   | The HAL Browser                                                                   |
| http://localhost:8080/server/api        | backend   | Server API                                                                   |
| http://localhost:8080/rest              | backend   | REST (Deprecated) Used by Perl Scripts                                       |
| http://localhost:8983/solr              | solr      | Solr GUI                                                                     |
| jdbc:postgresql://localhost:5432/dspace | db        | PostgreSQL (database: dspace, user: dspace, password: dspace)                                 |
| http://localhost:3000/metrics           | express   | Metrics service                                       |

### NOTES
- The backend services are configured to support the local production frontend service (see [mlibrary/dspace-angular](https://github.com/mlibrary/dspace-angular)#[Local Production Sandbox](https://github.com/mlibrary/dspace-angular#local-production-sandbox)).
- If the behavior of the frontend service is not as expected and the backend services are running and healthy...
- -  check the frontend service logs for any errors or issues.
- -  check the browser console for any errors or issues.
- -  try refreshing the page or clearing the browser cache.
- -  check the backend services logs for any errors or issues.

## Development

