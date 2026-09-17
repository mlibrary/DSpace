**How IntelliJ IDEA Run Configurations Work**

In short: **IntelliJ compiles your code automatically before running, but it does NOT run Maven `install` or `package` by default.**

---

**1. What Happens by Default: IntelliJ `Build`**

When you click **Run** or **Debug** on an existing Run Configuration (e.g., Spring Boot `Application` or a JUnit test):

1. **Before Launch Step (`Build`)**:
    * Every standard Run Configuration contains a **Before launch** task set to `Build` (incremental compilation).
    * IntelliJ inspects modified `.java` files and compiles them to `.class` files in the module output directory (e.g., `target/classes`).
    * It also copies standard resource files from `src/main/resources`.
2. **Classpath Execution**:
    * IntelliJ constructs the runtime classpath using your project module dependencies and downloaded JARs from `~/.m2/repository`.
    * It starts the JVM directly using your main class (such as `org.dspace.app.rest.Application`).

---

**2. Difference Between IntelliJ `Build` and Maven Goals**

| Action | What It Does | Handled by Run Config Default? |
|---|---|---|
| **IntelliJ `Build`** | Incremental `javac` compilation of changed source files into `.class` files. | **Yes** (automatic via "Before launch") |
| **`mvn compile`** | Full Maven-managed compilation through the `maven-compiler-plugin`. | **No** (unless added to Before launch) |
| **`mvn package`** | Compiles, runs tests, and packages code into JAR/WAR archives or installer assemblies. | **No** |
| **`mvn install`** | Copies packaged artifacts into your local Maven cache (`~/.m2/repository`). | **No** |

---

**3. When You Can Rely on IntelliJ vs. When to Run Maven Manually**

**You can rely purely on IntelliJ Run Configuration when:**
* You are making standard Java code changes inside existing classes.
* You are running or debugging unit tests.
* You are modifying application properties or resources directly loaded from the classpath.

**You need to run Maven manually (or trigger sync) when:**
* **Adding or modifying dependencies in `pom.xml`**: Click the **Reload All Maven Projects** icon (the small "m" sync button in the Maven Tool Window) so IntelliJ updates its dependency index.
* **Working with DSpace assembly modules**: DSpace has an assembly/installer module (`dspace/target/dspace-installer`) where configurations and file structures are assembled via Maven plugins. If your configuration references paths inside `dspace-installer` (such as `dspace/target/dspace-installer/config/...`), updating those files requires running `mvn package -DskipTests` or `mvn install -DskipTests`.
* **Annotation processing / generated code changes**: If metamodels (e.g., Hibernate JPA metamodels like `SystemWideAlert_.java`) or code generators need to be regenerated.

---

**4. Customizing "Before Launch" in IntelliJ**

If you want IntelliJ to run a specific Maven goal automatically before starting the application:

1. Open **Run** → **Edit Configurations...**.
2. Select your Run Configuration on the left (e.g., `Application`).
3. Scroll down to the **Before launch** section.
4. Click the **+** (Add) icon and choose **Run Maven Goal**.
5. Specify the working directory and goal (e.g., `compile` or `package -DskipTests`).
6. Apply and save. Now IntelliJ will execute that Maven phase prior to launching the application.

