Now I'm reviewing recent IDE activity logs to understand what caused the process to finish with exit code 1, aiming to identify the source of the error.

Checking recent IDE activity

idea/get_run_configurations

Searched ".idea/**/DSpace*.xml"

Searched ".idea/**/*"

idea/execute_run_configuration

idea/get_run_configurations

Searched "dspace/target/**"

Found "dspace/config/**"

Searched "**/target"

idea/search_symbol

idea/read_file

idea/read_file

idea/read_file

idea/read_file

idea/read_file

idea/read_file

idea/read_file

**Cause of the Issue**

The application exited with `exit code 1` immediately after the Spring Boot banner because the **DSpace home directory (`dspace.dir`) does not exist on disk**.

Here is what happens during startup:
1. When `org.dspace.app.rest.Application` runs, `DSpaceKernelInitializer` attempts to initialize the `DSpaceKernel`.
2. The kernel verifies whether the configured `dspace.dir` path (`/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer`) exists and contains `config/dspace.cfg`.
3. Because the Maven build has not assembled the `dspace/target/dspace-installer` folder yet, `isValidDSpaceHome(...)` returns `false`.
4. `DSpaceConfigurationService` throws `RuntimeException("DSpace home directory could not be determined. It MUST include a subpath of [dspace]/config/dspace.cfg")`, causing Spring Boot to terminate immediately.
5. Additionally, because `logging.config` points to `${dspace.dir}/config/log4j2.xml` (which is also missing inside the unbuilt target directory), the logging system was unable to write the stack trace to the console before exiting.

---

**How to Fix**

**Step 1: Build the `dspace-installer` Directory with Maven**  
DSpace requires the assembled installation directory containing configuration files, crosswalks, and scripts. Run the initial build from the project root:

```bash
mvn clean install -DskipTests
```
*(Alternatively, if Checkstyle errors occur: `mvn clean install -DskipTests -Dcheckstyle.skip=true`)*

This creates the `dspace/target/dspace-installer` directory required by the runtime.

**Step 2: Ensure Docker Services (PostgreSQL & Solr) Are Running**  
Before starting the REST API, start the backing database and search services:

```bash
docker compose up -d
```

**Step 3: Run the Application in IntelliJ IDEA**  
Confirm your IntelliJ IDEA run configuration for `Application`:
- **Main class**: `org.dspace.app.rest.Application`
- **VM options**:
  ```text
  -Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true
  ```
- **Working directory**: `/Users/gkostin/GitHub/mlibrary/DSpace` (or `$PROJECT_DIR$`)

Once `dspace/target/dspace-installer` is present, the kernel and embedded Tomcat will start successfully.

