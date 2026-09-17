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

**The `dspace/dspace-backend` package is dependent on the `dspace/dspace-dependencies` package and needs to be built first.**
## Local Production Sandbox
Using Docker Compose, you can build and run the backend services locally to simulate a production environment.
```shell
docker build -f dependencies.dockerfile -t dspace-dependencies .
docker compose up -d
docker compose exec backend /dspace/bin/dspace database migrate
```
### Services Endpoints
| URL                                     | Container | Comments                                                                     |
|-----------------------------------------|-----------|------------------------------------------------------------------------------|
| http://localhost:8080/server        | backend   | The HAL Browser                                                                   |
| http://localhost:8080/server/api        | backend   | Server API                                                                   |
| http://localhost:8080/rest              | backend   | REST (Deprecated) Used by Perl Scripts                                       |
| http://localhost:8983/solr              | solr      | Solr GUI                                                                     |
| jdbc:postgresql://localhost:5432/dspace | db        | PostgreSQL (database: dspace, user: dspace, password: dspace)                                 |
| http://localhost:3000/metrics           | express   | Metrics service                                       |

### Log In

First, make yourself an admin user.
```shell
docker compose exec backend /dspace/bin/dspace create-administrator
```
For example, the following command will create an administrator account with the email address `gkostin@umich.edu`.

```terminaloutput
% docker compose exec backend /dspace/bin/dspace create-administrator
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
%
```
**Ignore the warnings** about multiple config files.
- The `compose.yml` file is used to build and run the backend services.
- The `docker-compose.yml` file is the upstream compose file which has been kept as a reference file.

Now you can log in to the HAL Browser at [http://localhost:8080/server](http://localhost:8080/server) with the email address and password you just created.

If you are using the local frontend service, you can [log in](https://github.com/mlibrary/dspace-angular#log-in) to the frontend with the same email address and password.
### Additional Information
For more information see [DSpace README](DSPACE_README.md) (the original upstream README)

## Local Development
Ensure the [Local Production Sandbox](local-production-sandbox) is running
```shell
docker compose up -d
```
Then bring down the `backend service` to free up the ports for local development.
````shell
docker compose down backend
````
Set up your local development tools.
```shell
mise trust
mise install
```
Create `.dspace/config/local.cfg` file with the following content. Replace `ALMA_API_KEY` and `PUBMED_API_KEY` with your actual API keys.
```config
#      - dspace__P__dir=/dspace
#      - dspace__P__name=DSpace Local Production
#      - db__P__url=jdbc:postgresql://db:5432/dspace
#      - solr__P__server=http://solr:8983/solr
      # This needs an explanation: TODO: Why is this needed?  Why not just use the default value of /dspace/assetstore?
#      - filestorage__P__dir=/dspace
      # dspace.server.url MUST match the Angular DSPACE_REST_HOST:PORT/NAMESPACE so that
      # HAL root links use the same hostname Angular is configured with.  A mismatch causes
      # Angular SSR's URL normalizer to recurse infinitely (Maximum call stack size exceeded).
      # Each "." in the property key is represented by "__P__" in the env var name.
      - dspace__P__server__P__url=http://localhost:8080/server
      # dspace.ui.url is used by the backend to generate frontend links (e.g. e-mail alerts).
#      - dspace__P__ui__P__url=http://localhost:4000
      # Disable OIDC authentication for local dev; use password auth only.
      # NOTE: Setting the frontend environment variable DSPACE_AUTH_SHOWPASSWORDLOGIN: 'true' is required to show the password login form in the Angular UI.
#      - plugin__P__sequence__P__org__P__dspace__P__authenticate__P__AuthenticationMethod=org.dspace.authenticate.PasswordAuthentication
      # Enable OIDC authentication for local dev; use institutional SSO only.
      # NOTE: NOT setting the frontend environment variable DSPACE_AUTH_SHOWPASSWORDLOGIN: 'true' is required to show the OIDC login form in the Angular UI.
#      - plugin__P__sequence__P__org__P__dspace__P__authenticate__P__AuthenticationMethod=org.dspace.authenticate.OidcAuthentication
      # NOTE: See config/modules/authentication-oidc.cfg for authentication-oidc.client-id = dspace-7-testing settings

# api.user.key = ALMA_API_KEY
# pubmed.apiKey = PUBMED_API_KEY
rest.cors.allowed-origins = ${dspace.ui.url}
# filestorage.dir=data
ip.umIPs = 141.211.|35.2.
ip.bioIPsRange1 = 192.0.1.1|192.0.1.254
ip.bioIPsRange2 = 192.0.2.1|192.0.2.254
ip.BentleyOnlyIPs = 141.211.1.
# oai.config.dir = ${dspace.dir}/config/crosswalks/oai
# oai.description.file = ${dspace.dir}/config/crosswalks/oai/oai.cfg
# oai.cache.dir = ${dspace.dir}/var/oai
webui.user.assumelogin = true
proxies.trusted.ipranges = 127.0.0.1
# CORS configuration for local dev; allow localhost:4000 (Angular) and localhost:8080 (backend) to access the REST API.
# - dspace__P__rest__P__cors__P__allowedOrigins=http://localhost:4000,http://localhost:8080
# Ensure Authorization and DSPACE-XSRF-TOKEN are in exposed headers
# - dspace__P__rest__P__cors__P__exposedHeaders=Authorization,DSPACE-XSRF-TOKEN,Location,WWW-Authenticate
# - dspace__P__jwt__P__response__P__header__P__samesite=Lax
# - dspace__P__jwt__P__response__P__header__P__secure=false
```
Then you can run the backend service in development mode with hot reload enabled.

