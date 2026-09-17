Now I'm searching documentation and configuration files for references to deployment, Tomcat, or the dspace-server-webapp to understand how the application is currently deployed and configured.

To deploy the `dspace-server-webapp` (DSpace 7 REST API / Server Webapp) to a local Apache Tomcat instance, follow the step-by-step instructions below.

---

**Prerequisites**

1. **Java Development Kit (JDK)**: Java 17 (e.g., Eclipse Temurin 17).
2. **Servlet Container**: **Apache Tomcat 9.x**
   *(Note: Do not use Tomcat 10+, as it requires `jakarta.*` packages which are not compatible with Spring Boot 2.x / DSpace 7.x).*
3. **Build Tools**: Maven 3.9+ and Apache Ant 1.10+.
4. **Supporting Services**:
    - **PostgreSQL** (running on port `5432`)
    - **Apache Solr** (running on port `8983`)

   *(You can quickly start these supporting services using Docker Compose: `docker compose up -d db solr`)*

---

**Step 1: Configure DSpace (`local.cfg`)**

Create or update `dspace/config/local.cfg` in the project root to configure paths and service endpoints:

```properties
# DSpace installation directory where Ant will install files
dspace.dir = /Users/your-user/dspace

# Public URL of the backend webapp
dspace.server.url = http://localhost:8080/server

# Angular UI URL (for CORS and email links)
dspace.ui.url = http://localhost:4000
rest.cors.allowed-origins = ${dspace.ui.url}

# Database connection
db.url = jdbc:postgresql://localhost:5432/dspace
db.username = dspace
db.password = dspace

# Solr server URL
solr.server = http://localhost:8983/solr
```

---

**Step 2: Build the DSpace Package with Maven**

Build the source code to create the installation package:

```shell
mvn package
```

This compiles all modules and places the installer files in `dspace/target/dspace-installer`.

---

**Step 3: Install/Deploy DSpace Files with Ant**

Navigate to the generated installer directory and run Ant to assemble the installation directory (`${dspace.dir}`):

```shell
cd dspace/target/dspace-installer

# For a fresh install:
ant fresh_install

# Or for subsequent updates to an existing installation:
# ant update
```

This will populate `${dspace.dir}` with `bin`, `config`, `lib`, and `webapps/server`.

---

**Step 4: Run Database Migrations**

Before launching Tomcat, ensure database migrations have run:

```shell
/Users/your-user/dspace/bin/dspace database migrate
```

*(Optional) Create an initial administrator user:*
```shell
/Users/your-user/dspace/bin/dspace create-administrator
```

---

**Step 5: Configure Apache Tomcat 9**

**1. Set Memory and JVM Options (`setenv.sh` / `setenv.bat`)**
Create or edit `bin/setenv.sh` inside your Tomcat installation directory (e.g., `/path/to/apache-tomcat-9.0.x/bin/setenv.sh`):

```shell
export JAVA_OPTS="-Xmx2048m -Xms1024m -Dfile.encoding=UTF-8"
```
Make sure `setenv.sh` is executable: `chmod +x bin/setenv.sh`.

**2. Configure UTF-8 URI Encoding (`conf/server.xml`)**
Open `conf/server.xml` in Tomcat and verify that the HTTP connector on port `8080` specifies `URIEncoding="UTF-8"`:

```xml
<Connector port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="8443"
           URIEncoding="UTF-8" />
```

---

**Step 6: Deploy `server` to Tomcat**

You can deploy the web application using either of two methods:

**Option A: Symlink or Copy Webapp (Standard)**
Create a symbolic link from `${dspace.dir}/webapps/server` into Tomcat’s `webapps/` folder:

```shell
ln -s /Users/your-user/dspace/webapps/server /path/to/apache-tomcat-9.0.x/webapps/server
```

*(On Windows, copy the directory `C:\dspace\webapps\server` to `[TOMCAT_HOME]\webapps\server` or create a directory junction).*

**Option B: Tomcat Context XML Descriptor**
Alternatively, create a context XML file at `conf/Catalina/localhost/server.xml` inside your Tomcat directory:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Context docBase="/Users/your-user/dspace/webapps/server" reloadable="true">
</Context>
```

---

**Step 7: Start Tomcat and Verify**

1. Start Tomcat:
   ```shell
   /path/to/apache-tomcat-9.0.x/bin/startup.sh
   ```
2. Monitor the startup logs:
   ```shell
   tail -f /path/to/apache-tomcat-9.0.x/logs/catalina.out
   tail -f /Users/your-user/dspace/log/dspace.log
   ```
3. Test the deployment in your browser:
    - **HAL Browser**: `http://localhost:8080/server`
    - **REST API Endpoint**: `http://localhost:8080/server/api`

---

**Alternative: Running via IDE (IntelliJ IDEA)**

If you are developing locally, you can also run the webapp directly without an external Tomcat installation:
- Set up a Run Configuration in IntelliJ IDEA for the Spring Boot main class:
  `org.dspace.app.rest.Application` located in `dspace-server-webapp/src/main/java/org/dspace/app/rest/Application.java`.
- Pass `-Ddspace.dir=/Users/your-user/dspace` in the VM options if you want it to reference your installed configuration directory.