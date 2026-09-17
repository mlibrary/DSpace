Now I'm examining the pom.xml file in the dspace directory to identify how the dspace-rest module is configured, so I can understand its dependencies and setup before making any changes.

**How to Build and Deploy the Deprecated `dspace-rest` API**

In DSpace 7.x, the legacy `dspace-rest` API (v6.x Jersey-based webapp) is disabled by default to streamline builds and runtime overhead. However, the codebase retains full compatibility so you can build and run both `dspace-server-webapp` (the modern REST API) and `dspace-rest` (the deprecated REST API) side by side against the same database and configuration.

Below is the complete step-by-step guide to building, configuring, and running the `dspace-rest` module.

---

**Step 1: Build DSpace with the `dspace-rest` Maven Profile**

Because `dspace-rest` is excluded from default builds, you must explicitly enable its Maven profile (`-Pdspace-rest`):

```bash
mvn clean package -Pdspace-rest -DskipTests
```

**What this produces:**
* Compiles the `dspace-rest` module.
* Assembles the unpacked webapp at `dspace/target/dspace-installer/webapps/rest`.
* Creates the installable WAR artifact (`dspace-rest/target/dspace-rest-7.6.war`).

---

**Step 2: Configure HTTP vs. HTTPS (Local Development Note)**

By default, the legacy `dspace-rest` `web.xml` enforces SSL/TLS encryption via `<transport-guarantee>CONFIDENTIAL</transport-guarantee>`. If your local development Tomcat does not have HTTPS (port 8443) configured, requests to `http://localhost:8080/rest` will fail or redirect to a non-existent HTTPS port.

For **local testing/development on HTTP (port 8080)**:
1. Open `dspace/target/dspace-installer/webapps/rest/WEB-INF/web.xml` (or `dspace-rest/src/main/webapp/WEB-INF/web.xml`).
2. Locate the `<security-constraint>` block (around line 60) and comment it out or remove `<transport-guarantee>CONFIDENTIAL</transport-guarantee>`:
   ```xml
   <!--
   <security-constraint>
       <web-resource-collection>
           <web-resource-name>DSpace REST API (Deprecated)</web-resource-name>
           <url-pattern>/*</url-pattern>
       </web-resource-collection>
       <user-data-constraint>
           <transport-guarantee>CONFIDENTIAL</transport-guarantee>
       </user-data-constraint>
   </security-constraint>
   -->
   ```
*(Note: DSpace also provides a pre-configured insecure test version in `dspace/src/main/docker/test/rest_web.xml` for this purpose).*

---

**Step 3: Deploying `dspace-rest`**

Because `dspace-rest` is a legacy Java EE / Jersey servlet web application and `dspace-server-webapp` is a Spring Boot application, choose the deployment method that fits your environment:

**Option A: Running via Tomcat / Docker (Recommended for Dual Webapps)**
If you run DSpace in Docker or a standalone Apache Tomcat instance:

1. **Link or copy both webapps into Tomcat's `webapps/` folder**:
   ```bash
   # Link the modern REST API (/server)
   ln -s /path/to/dspace/webapps/server /path/to/tomcat/webapps/server

   # Link the legacy REST API (/rest)
   ln -s /path/to/dspace/webapps/rest   /path/to/tomcat/webapps/rest
   ```
2. **Start Tomcat**:
   Both web applications will boot up in the same servlet container sharing the underlying database:
    * Modern REST API: `http://localhost:8080/server/api`
    * Deprecated REST API: `http://localhost:8080/rest`

---

**Option B: Running in IntelliJ IDEA**

* **The Limitation**: The Spring Boot `Application` runner in IntelliJ only starts the embedded Tomcat container for `dspace-server-webapp`. It does not automatically serve the separate `dspace-rest` servlet structure.
* **The Solution**:
    1. Use **Tomcat** (or the container in `docker-compose.yml`) to host both webapps (`/server` and `/rest`).
    2. Use IntelliJ's **Remote JVM Debug** configuration (attached to port `8000`) to debug code execution across both web applications seamlessly.

---

**Step 4: Verification**

Once Tomcat starts:
1. Make a request to the root endpoint:
   ```bash
   curl -i http://localhost:8080/rest/test
   ```
2. Query the collections or communities endpoint using your existing legacy scripts:
   ```bash
   curl -i http://localhost:8080/rest/communities
   ```
3. The response will return the legacy JSON structure expected by your transition scripts.