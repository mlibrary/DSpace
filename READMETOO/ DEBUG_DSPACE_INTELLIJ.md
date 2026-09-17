Now I'm reviewing the run configurations to understand how the project is set up to execute, so I can determine the appropriate environment and parameters for running or testing the code.

**Step-by-Step Guide: Debugging DSpace in IntelliJ IDEA**

Debugging DSpace 7.x/8.x involves running the Spring Boot REST backend (`dspace-server-webapp`) in debug mode while having its required backing services (PostgreSQL and Solr) running.

---

**Step 1: Ensure Supporting Services Are Running (PostgreSQL & Solr)**

DSpace requires a running PostgreSQL database and Apache Solr instance. If you use Docker, start the supporting containers:

```bash
docker compose up -d dspacedb dspacesolr
```

Verify that:
- PostgreSQL is listening on `localhost:5432`
- Solr is listening on `localhost:8983`

---

**Step 2: Set Up or Edit the Spring Boot Run/Debug Configuration**

IntelliJ IDEA provides a dedicated Spring Boot Run Configuration. Follow these steps to configure it:

1. In IntelliJ IDEA, go to the top menu: **Run** → **Edit Configurations...**.
2. If you already have a configuration named `Application`, select it from the left pane. If not, click **+** (Add New Configuration) → **Spring Boot** (or **Application** if using IntelliJ Community).
3. Configure the fields as follows:
    * **Name**: `Application` (or `DSpace Server Webapp`)
    * **Main class**: `org.dspace.app.rest.Application`
    * **Use classpath of module**: `dspace-server-webapp`
    * **Working directory**: `$PROJECT_DIR$` (the root repository directory `/Users/gkostin/GitHub/mlibrary/DSpace`)
    * **JRE / Java version**: Select Java 11 or Java 17 (matching your project JDK setup).
4. **Environment Variables / VM Options** (Optional but helpful):
    * If your configuration references an assembled install directory, you can specify VM options:
      `-Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer`
5. **Before Launch Configuration**:
    * Ensure `Build` is present in the **Before launch** list.
    * If you need the installer directory synchronized before starting, you can add a **Run Maven Goal**: `package -DskipTests` with the working directory set to the project root.
6. Click **Apply** and **OK**.

---

**Step 3: Starting the Debugger**

1. Select your `Application` configuration in the top-right toolbar dropdown.
2. Click the **Debug** icon (the green bug icon next to the Play button) or press `Shift + F9` (macOS: `Control + D`).
3. IntelliJ will compile changed files and launch the Spring Boot embedded Tomcat server.
4. Watch the **Console** tab in the **Debug** tool window. When you see:
   ```text
   Started Application in X.XXX seconds
   ```
   the application is ready and waiting for incoming requests.

---

**Step 4: Setting Breakpoints and Debugging Code**

1. **Set Breakpoints**:
    * Open any Java file where you want execution to pause (e.g., `org.dspace.app.rest.repository.ItemRestRepository` or `org.dspace.app.rest.security.StatelessLoginFilter`).
    * Click in the left gutter next to the line number. A red dot will appear.
2. **Trigger the Breakpoint**:
    * Make an HTTP request (via browser, curl, or Postman) to the relevant REST endpoint (e.g., `http://localhost:8080/server/api/core/items`).
    * IntelliJ will immediately pause execution and highlight the active line.
3. **Core Debugger Controls**:
    * **Step Over (`F8`)**: Executes the current line and moves to the next line in the same method.
    * **Step Into (`F7`)**: Enters the method called on the current line to inspect its internals.
    * **Step Out (`Shift + F8`)**: Finishes the current method and returns to the caller.
    * **Resume Program (`F9` / `Cmd + Option + R`)**: Continues normal execution until the next breakpoint.
    * **Evaluate Expression (`Option + F8`)**: Opens an interactive prompt allowing you to run arbitrary Java expressions against current in-memory variables.
    * **Variables Pane**: Inspect field values, object states, and Spring beans in scope.

---

**Alternative: Remote Debugging (If Running DSpace in Docker)**

If you run the full DSpace backend inside the `dspace` Docker container instead of natively:

1. Note that `docker-compose.yml` exposes port `8000` for JDWP debugging.
2. In IntelliJ, go to **Run** → **Edit Configurations...**.
3. Click **+** → **Remote JVM Debug**.
4. Set:
    * **Host**: `localhost`
    * **Port**: `8000`
    * **Use module classpath**: `dspace-server-webapp`
5. Click **Apply** → **OK**, then launch this configuration in **Debug** mode to attach IntelliJ's debugger to the containerized JVM.

---

**Key Tips for Java / Maven / Spring Boot Beginners**

* **Hot Reloading / Code Changes**: When debugging natively, if you change method bodies in a `.java` file, you can recompile that file (**Build** → **Recompile '...java'** or `Cmd + Shift + F9`) while the debugger is running, and the JVM will hot-swap the modified bytecode without needing a restart.
* **Maven Sync**: Whenever you edit `pom.xml`, click the **Reload All Maven Projects** button in the Maven tool window so IntelliJ updates dependencies and classpath indices.
* **Database Reset / Migration**: If database schema errors occur, run Flyway migrations via Maven or the CLI (`docker compose exec dspace /dspace/bin/dspace database migrate`).