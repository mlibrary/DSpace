# UM IDE DEV

## Clone

```
git clone https://github.com/mlibrary/DSpace.git
cd DSpace
git checkout umich-ide-dev
```

## Docker Compose

```
docker compose build
docker compose up -d
```

## Mise

See [UM_MISE_README.md](UM_MISE_README.md) for details on how `mise` manages Java and Maven versions.

```
cat .mise.toml
mise trust
mise install
```

## Maven

Inside IntelliJ: You can trigger a full sync at any time via: 
* The Reload All Maven Projects icon (the two circular arrows in the top-left of the Maven tool window). 
* The shortcut Cmd + Shift + I (macOS) / Ctrl + Shift + O (or Cmd + Shift + A / Ctrl + Shift + A and typing Reload All Maven Projects).
* This is important because the project may require specific versions that differ from your system defaults.


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
proxies.trusted.ipranges = 127.0.0.1
rest.cors.allowed-origins = ${dspace.ui.url}
filestorage.dir=data
pubmed.apiKey=PUBMED_API_KEY
api.user.key=ALMA_API_KEY
ip.umIPs = 141.211.|35.2.
ip.bioIPsRange1 = 10.0.0.1|10.0.0.254
ip.bioIPsRange2 = 10.0.1.1|10.0.1.254
ip.BentleyOnlyIPs = 141.211.1.
oai.config.dir = ${dspace.dir}/config/crosswalks/oai
oai.description.file = ${dspace.dir}/config/crosswalks/oai/oai.cfg
oai.cache.dir = ${dspace.dir}/var/oai
```

## Build

```bash
mvn clean install -DskipTests
```

### Why is this needed?
DSpace requires a "home" directory (defined by `dspace.dir`) that contains a specific structure of configurations, JARs, and scripts. The Maven build process assembles this structure into `dspace/target/dspace-installer`.

*   **Initial Setup**: You **must** run this command at least once to create the installer directory.
*   **Java Code Changes**: IntelliJ IDEA handles incremental compilation. After the initial Maven build, you can usually just restart the **Application** in IDEA to see your code changes without running Maven again.
*   **Configuration Changes**: If you modify `local.cfg` or other files in `dspace/config`, you should run `mvn install` again to ensure the changes are copied into the `dspace-installer` directory.

## IntelliJ IDEA Setup

### Run Configuration

1. Open the project in IntelliJ IDEA.
2. Create a new **Application** run configuration.
3. In the modify options drop-down, select **Add dependencies with "provided" scope to classpath**.
4. In the modify options drop-down, select **Add VM Options**.
5. **Main class**: `org.dspace.app.rest.Application` (in `dspace-server-webapp` module). See [UM_APP_REST_MAIN.md](UM_APP_REST_MAIN.md) for details.
6. **VM Options**: `-Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true`
7. **Working directory**: `/Users/gkostin/GitHub/mlibrary/DSpace`
8. Apply the changes and save the configuration.
9. Run the configuration to start the DSpace REST API server.

### On build failure try ...

* The Reload All Maven Projects icon (the two circular arrows in the top-left of the Maven tool window).
* The shortcut Cmd + Shift + I (macOS) / Ctrl + Shift + O (or Cmd + Shift + A / Ctrl + Shift + A and typing Reload All Maven Projects).

```shell
mvn clean install -DskipTests
mvn dependency:resolve
```
9. Run the configuration to start the DSpace REST API server.


## Tail the dspace.log

```shell
tail -f /Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer/data/log/dspace.log
```

## Sanity Check
```
curl -i http://localhost:8080/server/api
```

## Create Administrator
```
bash dspace/target/dspace-installer/bin/dspace create-administrator
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

## Troubleshooting

### License Header Failures
If the build fails with `com.mycila:license-maven-plugin:3.0:check (check-headers)`, it means some files are missing the required license header.

**To fix this automatically, run:**
```bash
mvn license:format
```
You can also target a specific module:
```bash
mvn license:format -pl dspace-api
```

### Checkstyle Violations
If the build fails with Checkstyle violations, it means the code does not follow the project's style guidelines. In this development branch, many custom files are temporarily suppressed in `checkstyle-suppressions.xml`.

**To skip style checks entirely during build:**
```bash
mvn clean install -DskipTests -Dcheckstyle.skip=true
```

### Error Prone Compilation Errors
If you encounter unhandled exceptions from the Error Prone plugin (common when using Java 17 for building a Java 11 project), Error Prone has been disabled in the root `pom.xml` to ensure compatibility. If you need to re-enable it, you will need to adjust the `maven-compiler-plugin` configuration in `pom.xml`.
