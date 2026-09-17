Now I'm reviewing the run configurations to understand how the project is set up to execute, so I can determine the appropriate environment and parameters for running or testing the code.

To run the DSpace backend in IntelliJ IDEA for local development, follow the steps below.

---

**1. Start Dependent Infrastructure Services**

The backend requires **PostgreSQL** and **Apache Solr** to be up and accessible on localhost. You can start them using Docker Compose:

```shell
docker compose up -d db solr
```

*(Note: If the entire local production sandbox is currently running, bring down the containerized backend to avoid port conflicts on `8080`: `docker compose stop backend` or `docker compose down backend`).*

---

**2. Configure Local Properties and Database**

1. Ensure your `dspace/config/local.cfg` configuration file exists and points to your local database and Solr instances:
   ```properties
   dspace.dir = /dspace
   dspace.server.url = http://localhost:8080/server
   dspace.ui.url = http://localhost:4000
   db.url = jdbc:postgresql://localhost:5432/dspace
   db.username = dspace
   db.password = dspace
   solr.server = http://localhost:8983/solr
   ```
2. If this is a fresh setup, make sure the database migrations have executed:
   ```shell
   docker compose exec backend /dspace/bin/dspace database migrate
   ```

---

**3. Configure Project SDK in IntelliJ IDEA**

1. Open **File** > **Project Structure...** (or press `Cmd + ;` on macOS).
2. Under **Project Settings** > **Project**, ensure the **SDK** is set to **Java 17** (e.g. Eclipse Temurin 17 / OpenJDK 17).
3. Ensure the Maven project is synchronized (open the **Maven** tool window on the right and click **Reload All Maven Projects**).

---

**4. Create and Launch the Run/Debug Configuration**

You can launch the backend directly as a Spring Boot application:

**Quick Start (Gutter Icon)**
1. In the Project file tree, navigate to:  
   `dspace-server-webapp/src/main/java/org/dspace/app/rest/Application.java`
2. Right-click or click the green **Run** / **Debug** icon in the editor gutter next to the class name `Application` or the `main` method and select **Run 'Application'** or **Debug 'Application'**.

**Manual Run Configuration Setup**
1. Go to **Run** > **Edit Configurations...**.
2. Click **+** (Add New Configuration) and choose **Application** (or **Spring Boot** if using IntelliJ IDEA Ultimate).
3. Set the following fields:
    - **Name**: `DSpace Backend Server`
    - **Main class**: `org.dspace.app.rest.Application`
    - **Use classpath of module**: `dspace-server-webapp`
    - **VM options**:
      ```text
      -Xmx2048m -Xms1024m -Dfile.encoding=UTF-8
      ```
      *(If referencing a custom installed DSpace directory, add `-Ddspace.dir=/path/to/dspace`)*
    - **Working directory**: `$MODULE_DIR$` or the repository root.
4. Click **Apply** and **OK**, then click **Run** or **Debug**.

---

**5. Verify the Backend is Running**

Once the application finishes booting in the Run/Debug console:
- **HAL Browser**: [`http://localhost:8080/server`](http://localhost:8080/server)
- **REST API Endpoint**: [`http://localhost:8080/server/api`](http://localhost:8080/server/api)


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


```shell
mvn package
```

```shell
cd dspace/target/dspace-installer

# For a fresh install:
ant fresh_install

# Or for subsequent updates to an existing installation:
# ant update
```




Then you can run the backend service in development mode with hot reload enabled.

. Run the Backend in Your IDE
•
Ensure your local configuration (dspace/config/local.cfg) points to localhost:5432 for the database and http://localhost:8983/solr for Solr.
•
Run the Spring Boot main class org.dspace.app.rest.Application located in dspace-server-webapp/src/main/java/org/dspace/app/rest/Application.java from IntelliJ IDEA or deploy the dspace-server-webapp module to a local Tomcat 9 instance.

## IntelliJ IDEA Development Configuration
1. Open the project in IntelliJ IDEA.
2. Go to `File > Project Structure > Modules` and ensure that the `dspace-server-webapp` module is marked as a web module.
3. Go to `Run > Edit Configurations` and create a new Spring Boot configuration for the `org.dspace.app.rest.Application` class.
4. Set the working directory to the root of the project.
5. Add the following VM options to enable hot reload:
```
-Dspring.devtools.restart.enabled=true
-Dspring.devtools.restart.exclude=static/**,public/**,templates/**
```
6. Apply the changes and run the configuration.
## Junie Developer Notes
Now I'm rebuilding the Application component in the server webapp to ensure its latest state is compiled and ready for any further analysis or changes.

**Why the Backend Fails to Compile in IntelliJ IDEA**

When attempting to build and run `Application.java` directly in IntelliJ IDEA on a fresh clone or clean build, compilation fails because **required code generation steps handled by Maven have not been executed yet**.

---

**Root Cause Analysis**

1. **Missing Generated JAXB Classes in `dspace-api`**:
   - `dspace-server-webapp` directly depends on the `dspace-api` module.
   - In `dspace-api`, the class `org.dspace.workflow.CurationTaskConfig.java` references classes such as `WorkflowCuration`, `MappingType`, `FlowstepType`, `TaskType`, `WorkflowType`, and `NotifyType`.
   - These classes are **not** present in the source repository. Instead, they are generated during Maven's build phase by the `jaxb2-maven-plugin` (configured in `dspace-api/pom.xml`) from the XML schema `src/main/resources/org/dspace/workflow/workflow-curation.xsd`.

2. **IntelliJ IDEA's Internal Compiler Lacks the Generated Sources**:
   - IntelliJ IDEA’s default internal Java compiler does not automatically trigger Maven plugins like `jaxb2-maven-plugin:xjc` unless Maven has run the source generation phase.
   - When building `Application.java`, IntelliJ tries to compile dependent modules (`dspace-api`), encounters missing symbols in `CurationTaskConfig`, and aborts with compilation errors (such as `package org.dspace.app.rest... does not exist` or `cannot find symbol class NotifyType`).

---

**How to Resolve**

**Step 1: Generate Sources with Maven**
Run Maven from your terminal in the project root to generate the missing JAXB source classes and compile dependencies:

```shell
mvn compile -DskipTests
```
*(or at minimum `mvn generate-sources`)*

This generates the Java source files under `dspace-api/target/generated-sources/jaxb/`.

**Step 2: Sync / Reload Maven in IntelliJ IDEA**
1. Open the **Maven** tool window on the right sidebar of IntelliJ IDEA.
2. Click the **Reload All Maven Projects** icon (the two circular arrows).
3. IntelliJ will automatically discover the `target/generated-sources/jaxb` directory and mark it as a **Generated Sources Root**.

**Step 3: Run `Application.java`**
Once the Maven sync finishes and the generated sources are indexed:
1. Rebuild the project via **Build** > **Build Project** (`Cmd + F9` / `Ctrl + F9`).
2. Run `Application.java` using the gutter icon or your configured Run/Debug configuration.

---

**Optional: Delegate IDE Builds to Maven (Alternative)**
If you prefer IntelliJ IDEA to always use Maven to compile and build the project:
1. Open **Preferences / Settings** (`Cmd + ,` or `Ctrl + Alt + S`).
2. Navigate to **Build, Execution, Deployment** > **Build Tools** > **Maven** > **Runner**.
3. Check **Delegate IDE build/run actions to Maven**.
4. Click **Apply** and **OK**.
### Checkstyle Supression
Now I'm examining the pom.xml file to locate any checkstyle configurations, aiming to understand how code style rules are currently set up in the project.

To maximize development efficiency and minimize disruption depending on whether you need a temporary build bypass or a permanent project configuration, here are the most effective ways to suppress or skip Checkstyle errors in DSpace:

---

**1. Skip Checkstyle Execution via Maven Flag (Fastest for Local Builds)**

If your goal is to build, package, or test without incurring the overhead of style verification, pass the `checkstyle.skip` property:

```shell
mvn package -Dcheckstyle.skip=true
```
*(or combined with skipping tests: `mvn package -DskipTests -Dcheckstyle.skip=true`)*

**Setting in IntelliJ IDEA**
To automatically skip Checkstyle across all Maven executions in IntelliJ:
1. Open **Preferences / Settings** (`Cmd + ,` or `Ctrl + Alt + S`).
2. Navigate to **Build, Execution, Deployment** > **Build Tools** > **Maven** > **Runner**.
3. In **VM Options**, append `-Dcheckstyle.skip=true`.
4. Click **Apply** and **OK**.

---

**2. Allow Violations Without Failing the Build**

If you want visibility into the style report without halting the build pipeline:

```shell
mvn verify -Dcheckstyle.failOnViolation=false
```

This logs warnings/errors to console for information without aborting execution with a build failure.

---

**3. Suppress Specific Files or Rules in `checkstyle-suppressions.xml`**

To selectively disable Checkstyle for specific modules (such as `dspace-server-webapp`), file paths, or check categories, add rules to `checkstyle-suppressions.xml` in the repository root:

Open `checkstyle-suppressions.xml` and add suppression rules:

```xml
<suppressions>
    <!-- Suppress all Checkstyle checks for dspace-server-webapp -->
    <suppress checks=".*" files="dspace-server-webapp[/\\]src[/\\]main[/\\]java"/>

    <!-- Or suppress specific noisy rules across all files -->
    <suppress checks="CustomImportOrder" files=".*"/>
    <suppress checks="LineLength" files=".*"/>
    <suppress checks="Javadoc.*" files=".*"/>
</suppressions>
```

This achieves a balance between maintaining high code quality across core modules while eliminating noise in specific subsystems.

---

**4. Suppress Checks at the Java Code Level**

DSpace's `checkstyle.xml` is configured with `SuppressWarningsFilter` and `SuppressWarningsHolder`. You can suppress specific Checkstyle rules directly on a class, method, or field:

```java
@SuppressWarnings("checkstyle:CustomImportOrder")
@SuppressWarnings("checkstyle:LineLength")
@SuppressWarnings("checkstyle:JavadocMethod")
```

Or suppress all Checkstyle warnings on a target class:

```java
@SuppressWarnings("checkstyle:all")
public class Application extends SpringBootServletInitializer {
    ...
}
```

---

**5. Run Maven Phases Prior to `verify`**

In the root `pom.xml`, the `maven-checkstyle-plugin` is bound to the `verify` lifecycle phase. If you only need to compile sources or build generated artifacts, run goals that execute before `verify`:

```shell
mvn compile
```
or:
```shell
mvn test-compile
```

Checkstyle will not be invoked during `compile` or `test-compile`.


## Yet again



-Dcheckstyle.skip=true

mvn checkstyle:check -Dcheckstyle.failOnViolation=false

java -jar checkstyle-X.X-all.jar -c /checkstyle.xml -g /path/to/project > checkstyle-xpath-suppressions.xml


Now I'm examining the main build configuration file to identify the Checkstyle version and plugin settings, so I can understand how code style enforcement is currently configured.

**Checkstyle Version for Option 3**

To match this project's configuration, you should use **Checkstyle `8.30`**.

This project explicitly overrides the Checkstyle dependency version in `pom.xml` (lines 288–292):

```xml
<dependency>
    <groupId>com.puppycrawl.tools</groupId>
    <artifactId>checkstyle</artifactId>
    <version>8.30</version>
</dependency>
```

*(Note: The `-g` / `--generate-xpath-suppress` CLI flag was introduced in Checkstyle 8.21, so version 8.30 fully supports it).*

---

**How to Download and Run**

**1. Download `checkstyle-8.30-all.jar`**
You can download the standalone executable JAR from GitHub releases:

```shell
curl -sSL -O https://github.com/checkstyle/checkstyle/releases/download/checkstyle-8.30/checkstyle-8.30-all.jar
```

**2. Generate the XPath Suppressions File**
Run the CLI against the project's `checkstyle.xml` configuration and Java source directories:

```shell
java -jar checkstyle-8.30-all.jar -c ./checkstyle.xml -g ./dspace-api/src > checkstyle-xpath-suppressions.xml
```

You can target specific modules (e.g. `./dspace-api/src`, `./dspace-server-webapp/src`) or pass the whole repository source path.





Now I'm examining the pom.xml and dspace-api/pom.xml files to understand how the license-maven-plugin is configured, so I can determine its role and impact on the build process.

**Cause of the Error**

The build failure occurs because the **`com.mycila:license-maven-plugin`** plugin validates that all source files under `src/` contain the standard DSpace license header defined in `LICENSE.header`.

If any newly created, modified, or generated files are missing this header (or have an improperly formatted header), the `check` goal fails the build.

---

**Solutions**

**1. Automatically Format and Add Missing License Headers (Recommended)**

The plugin provides a `format` goal that automatically inserts or updates the required license header in all applicable source files.

- **Format files in the entire project:**
  ```shell
  mvn license:format
  ```

- **Format only the `dspace-api` module:**
  ```shell
  mvn license:format -pl dspace-api
  ```

Once completed, the plugin modifies the files in place, adding the header comment.

---

**2. Find Which Files Are Missing the License Header**

To see the exact list of files causing the failure, run the check goal directly or with verbose error output:

```shell
mvn license:check -pl dspace-api
```
or
```shell
mvn compile -e -pl dspace-api
```

---

**3. Skip the License Check During Build**

If you want to run your build, compile, or run tests without being blocked by license header checks:

```shell
mvn clean install -Dlicense.skip=true
```
*(You can also combine it with checkstyle skip: `mvn clean install -Dlicense.skip=true -Dcheckstyle.skip=true`)*

---

**4. Exclude Specific Files from License Checks**

If certain files should not have the license header (e.g., third-party code or specific templates), add an `<exclude>` pattern to `dspace-api/pom.xml` (or root `pom.xml`):

```xml
<plugin>
    <groupId>com.mycila</groupId>
    <artifactId>license-maven-plugin</artifactId>
    <configuration>
        <excludes>
            <!-- Existing excludes -->
            <exclude>src/test/resources/**</exclude>
            <!-- Add your file pattern here -->
            <exclude>**/path/to/custom/file.java</exclude>
        </excludes>
    </configuration>
</plugin>
```




