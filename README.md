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

**The `dspace/dspace-backend` package is dependent on the `dspace/dspace-dependencies` package which needs to be built first.**
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

If you are using the local frontend service, you can [Log In](https://github.com/mlibrary/dspace-angular#log-in) to the frontend with the same email address and password.

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
Create `.dspace/config/local.cfg` file with the following content.
```config
rest.cors.allowed-origins = ${dspace.ui.url}
proxies.trusted.ipranges = 127.0.0.1
webui.user.assumelogin = true
api.user.key = ALMA_API_KEY
pubmed.apiKey = PUBMED_API_KEY
ip.umIPs = 141.211.|35.2.
ip.bioIPsRange1 = 10.0.0.1|10.0.0.254
ip.bioIPsRange2 = 10.0.1.1|10.0.1.254
ip.BentleyOnlyIPs = 141.211.1.
oai.config.dir = ${dspace.dir}/config/crosswalks/oai
oai.description.file = ${dspace.dir}/config/crosswalks/oai/oai.cfg
oai.cache.dir = ${dspace.dir}/var/oai
```

Do a clean install of the backend service.
```shell
mvn clean install -Dlicense.skip=true -Dcheckstyle.skip=true -DskipTests
mvn clean package -Pdspace-rest -Dlicense.skip=true -Dcheckstyle.skip=true -DskipTests
```
### Testing
See testing instructions in the [README2](README2.md) file (the original upstream README).



### IntelliJ IDEA

The simplest way to run the backend service locally for development is to use IntelliJ IDEA.  The backend service is a Spring Boot application, and IntelliJ has built-in support for running Spring Boot applications.

The easiest way to create a new run configuration is to open the `Application.java` file in the `dspace-server-webapp` module (`dspace-server-webapp/src/main/java/org/dspace/app/rest/Application.java`) and click the green run icon next to the `main` method.  This will create a new run configuration for the backend service.

You'll get the following ERROR message in the console when you run the backend service locally for development:

```
14:11:10.827 [main] ERROR org.springframework.boot.SpringApplication - Application run failed
java.lang.IllegalArgumentException: Circular placeholder reference 'dspace.dir' in property definitions
	at org.springframework.util.PropertyPlaceholderHelper.parseStringValue(PropertyPlaceholderHelper.java:147) ~[spring-core-5.3.27.jar:5.3.27]
        ...
	at org.dspace.app.rest.Application.main(Application.java:85) [classes/:?]

Process finished with exit code 1
```

See the following agent-generated markdown files for more information:

- [INTELLIJ.md](READMETOO/INTELLIJ.md)

### Tomcat

TODO: Add instructions for running Tomcat locally for development.

See the following agent-generated markdown files for more information:
- [TOMCAT.md](READMETOO/TOMCAT.md)
## Additional Information
For more information see [README2](README2.md) (the original upstream README) and agent responses to questions in the READMETOO directory (feel free to contribute additional responses).
