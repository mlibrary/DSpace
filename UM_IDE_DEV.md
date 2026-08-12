# UM IDE DEV

## Clone

```
git clone https://github.com/mlibrary/DSpace.git
cd DSpace
git checkout umich-ide-dev
```

## Prerequisites

### Mise

See [UM_MISE_README.md](UM_MISE_README.md) for details on how `mise` manages Java and Maven versions.

```
mise trust
mise install
```

## Build

```
mvn clean install -DskipTests
```

## Create dspace/config/local.cfg

```
dspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer
dspace.server.url = http://localhost:8080/server
dspace.ui.url = http://localhost:4000
dspace.name = DSpace at My University
db.url = jdbc:postgresql://localhost:5432/dspace
db.driver = org.postgresql.Driver
db.dialect = org.hibernate.dialect.PostgreSQL94Dialect
db.username = dspace
db.password = dspace
db.schema = public
solr.server = http://localhost:8983/solr
rest.cors.allowed-origins = ${dspace.ui.url}
filestorage.dir=data
pubmed.apiKey=CHANGEME
ip.umIPs = 141.211.|35.2.
ip.bioIPsRange1 = 10.0.0.1|10.0.0.254
ip.bioIPsRange2 = 10.0.1.1|10.0.1.254
ip.BentleyOnlyIPs = 141.211.1.
oai.config.dir = ${dspace.dir}/config/crosswalks/oai
oai.description.file = ${dspace.dir}/config/crosswalks/oai/oai.cfg
oai.cache.dir = ${dspace.dir}/var/oai
```

## Docker Compose

```
docker compose build
docker compose up -d
```

## IntelliJ IDEA Setup

### Run Configuration

1. Open the project in IntelliJ IDEA.
2. Create a new **Application** run configuration.
3. **Main class**: `org.dspace.app.rest.Application` (in `dspace-server-webapp` module).
4. **VM Options**: `-Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer`
5. **Working directory**: `$PROJECT_DIR$`

## Sanity Check
```
curl -i http://localhost:8080/server/api
```

## Create Administrator
```
cd dspace/target/dspace-installer/bin
./dspace create-administrator
```

```
Creating an initial administrator account
E-mail address: gkostin@umich.edu
First name: Greg
Last name: Kostin
Is the above data correct? (y or n): y
Password will not display on screen.
Password: 
Again to confirm: 
Administrator account created
```

## Testing

By default, tests are skipped during the build process to save time and avoid infrastructure dependencies.

### Running Unit Tests
Unit tests do not require the Docker services to be running.
```bash
mvn test -DskipUnitTests=false
```

### Running Integration Tests
Integration tests require the database and Solr services to be running (via Docker Compose).
```bash
mvn install -DskipIntegrationTests=false
```

### Running Specific Tests
To run a single test class:
```bash
mvn test -DskipUnitTests=false -Dtest=org.dspace.app.rest.builder.CollectionBuilderTest -DfailIfNoTests=false
```
