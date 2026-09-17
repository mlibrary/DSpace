**Setting Up an IntelliJ Run Configuration for `dspace-rest`**

Yes, you can configure IntelliJ to run and debug the legacy `dspace-rest` API.

Because `dspace-rest` is a **traditional Java EE / Jersey Servlet WAR** rather than a Spring Boot application with a `main()` entrypoint, it requires a Servlet container (Tomcat) to host and execute it. Depending on your IntelliJ edition and workflow, you have three primary approaches:

---

**Option 1: Native Tomcat Run Configuration (IntelliJ IDEA Ultimate)**

If you use IntelliJ IDEA Ultimate, you can use the built-in Tomcat Server runner:

1. **Build the WAR artifact**:
    * Build the project with the `dspace-rest` profile:
      ```bash
      mvn package -Pdspace-rest -DskipTests
      ```
2. **Create the Tomcat Run Configuration**:
    * Go to **Run** → **Edit Configurations...**.
    * Click **+** (Add New Configuration) → **Tomcat Server** → **Local**.
    * Name it: `DSpace Legacy REST (Tomcat)`.
    * **Tomcat Home**: Select your local Apache Tomcat 9 installation folder.
3. **Configure Deployment**:
    * Go to the **Deployment** tab.
    * Click **+** → **Artifact...** → select `dspace-rest:war exploded` (or browse to `dspace/target/dspace-installer/webapps/rest`).
    * Set **Application context**: `/rest`.
4. **Configure VM Options**:
    * On the **Server** tab, add your DSpace directory to **VM options**:
      ```text
      -Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer -Xmx1024m
      ```
5. **Debug**:
    * Click the **Debug** button (`Shift + F9`).
    * IntelliJ starts Tomcat with `/rest` deployed. Breakpoints in `org.dspace.rest.*` will trigger when your legacy scripts make requests to `http://localhost:8080/rest`.

---

**Option 2: Remote JVM Debug Configuration (Ultimate & Community)**

If you run DSpace in Docker (`docker-compose.yml`) or run Tomcat in a terminal:

1. **Deploy the `dspace-rest` WAR**:
    * Build with `mvn package -Pdspace-rest -DskipTests`.
    * Ensure `dspace/target/dspace-installer/webapps/rest` is linked or mounted into Tomcat's `webapps/rest`.
2. **Create the Remote Debug Configuration**:
    * Go to **Run** → **Edit Configurations...**.
    * Click **+** → **Remote JVM Debug**.
    * **Name**: `Remote Debug (dspace-rest)`.
    * **Host**: `localhost`.
    * **Port**: `8000` (the standard debugging port exposed in `docker-compose.yml`).
    * **Use module classpath**: `dspace-rest`.
3. **Debug**:
    * Start your Docker container or standalone Tomcat, then click **Debug** in IntelliJ to attach.
    * Your breakpoints will pause execution whenever legacy scripts hit `http://localhost:8080/rest`.

---

**Option 3: Embedded Tomcat Launcher (Community Edition Standalone)**

In IntelliJ Community Edition (which lacks the native Tomcat Server integration), you can create a lightweight launcher class inside `dspace-rest/src/test/java/org/dspace/rest/RestApiLauncher.java` using Tomcat's embedded library:

```java
package org.dspace.rest;

import java.io.File;
import org.apache.catalina.startup.Tomcat;

public class RestApiLauncher {
    public static void main(String[] args) throws Exception {
        System.setProperty("dspace.dir", new File("dspace/target/dspace-installer").getAbsolutePath());
        
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector();

        String webappDir = new File("dspace-rest/src/main/webapp").getAbsolutePath();
        tomcat.addWebapp("/rest", webappDir);

        tomcat.start();
        tomcat.getServer().await();
    }
}
```

* **Run Configuration**: Create a standard **Application** run configuration in IntelliJ targeting `RestApiLauncher` with the `dspace-rest` module classpath.

---

**Important: Local HTTP Configuration (`web.xml`)**

By default, `dspace-rest/src/main/webapp/WEB-INF/web.xml` enforces HTTPS via `<transport-guarantee>CONFIDENTIAL</transport-guarantee>`.

If you are testing locally on plain HTTP (`http://localhost:8080/rest`):
* Open `web.xml` and comment out the `<security-constraint>` block containing `CONFIDENTIAL` so requests over port 8080 are not rejected or redirected.