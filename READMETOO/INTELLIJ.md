Now I'm examining the run.xml files to understand how the automated workflows or processes are defined, so I can figure out the structure and key components of the system's execution steps.

To maximize developer productivity, minimize setup overhead, and enable seamless debugging and rapid feedback cycles, running the backend directly in IntelliJ IDEA while offloading supporting services to Docker is the optimal workflow.

Here is the complete step-by-step guide to configure and run the DSpace backend in IntelliJ IDEA.

---

**1. Start Supporting Services (Database & Solr)**

Rather than manually installing and configuring PostgreSQL and Apache Solr locally, leverage Docker to spin up healthy, pre-configured instances in seconds:

```shell
docker compose up -d db solr
```

Ensure the database schema and migrations are initialized:
```shell
# If you haven't run migrations yet, you can run them via the DSpace CLI or container:
docker compose run --rm backend /dspace/bin/dspace database migrate
```

---

**2. Configure Local Properties (`local.cfg`)**

DSpace requires a configuration directory (`dspace.dir`) containing valid configuration files. Ensure your local configuration file (e.g., `dspace/config/local.cfg`) connects to the local host ports:

```properties
# DSpace directory path (can point to the project's dspace folder or an installed directory)
dspace.dir = /Users/gkostin/GitHub/mlibrary/DSpace/dspace

# Public URLs
dspace.server.url = http://localhost:8080/server
dspace.ui.url = http://localhost:4000
rest.cors.allowed-origins = ${dspace.ui.url}

# Database Connection (Docker container exposed on port 5432)
db.url = jdbc:postgresql://localhost:5432/dspace
db.username = dspace
db.password = dspace

# Solr Search Engine (Docker container exposed on port 8983)
solr.server = http://localhost:8983/solr

# File storage
filestorage.dir = ${dspace.dir}/assetstore

# Disable OIDC for local development (use password authentication)
plugin.sequence.org.dspace.authenticate.AuthenticationMethod = org.dspace.authenticate.PasswordAuthentication
```

---

**3. Project & SDK Setup in IntelliJ IDEA**

1. Open the project root in **IntelliJ IDEA**.
2. Ensure IntelliJ recognizes the project as a **Maven** project (if prompted, click **Load Maven Project**).
3. Set the Project SDK:
    - Navigate to **File** > **Project Structure** > **Project Settings** > **Project**.
    - Set **SDK** to **Java 17** (e.g., Eclipse Temurin 17).
    - Set **Language Level** to **17**.

---

**4. Create an Application Run Configuration**

You can configure either a **Spring Boot** run configuration (in IntelliJ Ultimate) or a standard **Application** run configuration (in IntelliJ Community / Ultimate):

**Steps to Create the Configuration:**
1. Go to **Run** > **Edit Configurations...**.
2. Click **+** (Add New Configuration) and select **Application** (or **Spring Boot**).
3. Configure the following fields:
    - **Name**: `DSpace Backend (Server Webapp)`
    - **JDK / Java version**: Java 17
    - **Main class**: `org.dspace.app.rest.Application`
    - **Use classpath of module**: `dspace-server-webapp`
    - **Working directory**: `$PROJECT_DIR$` (or `/Users/gkostin/GitHub/mlibrary/DSpace`)

4. **VM Options** (Click *Modify options* > *Add VM options* if not visible):
   ```text
   -Xmx2048m -Dfile.encoding=UTF-8 -Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace
   ```
   *(Replace `/Users/gkostin/GitHub/mlibrary/DSpace/dspace` with the absolute path to your `dspace` directory or installation path).*

5. **Environment Variables** (Optional fallback if not using `local.cfg`):
   ```text
   dspace__P__dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace;db__P__url=jdbc:postgresql://localhost:5432/dspace;solr__P__server=http://localhost:8983/solr
   ```

6. Click **Apply** and **OK**.

---

**5. Launch & Debug**

- **Run**: Click the green **Run** button (or `Control + R` / `Shift + F10`) to start the Spring Boot embedded Tomcat server.
- **Debug**: Click the **Debug** button (or `Control + D` / `Shift + F9`) to set breakpoints in controllers, services, converters, or filters for live debugging.

---

**6. Verify Service Availability**

Once the application finishes startup initialization, verify the endpoints:
- **HAL Browser**: `http://localhost:8080/server`
- **REST API Root**: `http://localhost:8080/server/api`
- **Health / Actuator**: `http://localhost:8080/server/api/health` (if enabled)

---

**7. Efficiency Tips for Local Development**

- **Port Conflicts**: Ensure no other process or Docker container (such as the `backend` container from `compose.yml`) is binding to port `8080`.
- **Hot Swap / Live Reload**: When running in Debug mode, editing method bodies and recompiling (`Command + Shift + F9` / `Ctrl + Shift + F9`) allows hot-swapping code without restarting the Spring Boot application context.
- **Solr Indexing**: If searches or item lookups return empty results after bootstrapping, run the discovery index update via IntelliJ terminal:
  ```shell
  dspace/bin/dspace index-discovery -b
  ```