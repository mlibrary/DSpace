**Recommendation: Use `mvn package`**

When creating or refreshing the `dspace/target/dspace-installer` directory for running or debugging inside IntelliJ IDEA, **`mvn package` (with `-DskipTests`) produces the highest net benefit in developer velocity and resource efficiency.**

---

**Key Reasons: Maximizing Efficiency and Output**

**1. Minimizes Redundant Work and Execution Latency**
* The DSpace installer assembly (`dspace/target/dspace-installer`) is produced during the **`package` lifecycle phase** by the Maven Assembly Plugin in the `dspace` module.
* `mvn install` executes the full `package` phase and then spends additional CPU cycles, memory, and disk I/O copying large JAR, WAR, and POM artifacts into the local cache (`~/.m2/repository`).
* Skipping the `install` phase eliminates this unneeded overhead, delivering the fastest feedback loop and saving valuable development time.

**2. IntelliJ Directly Uses Workspace Dependencies**
* When launching or debugging Spring Boot (`dspace-server-webapp`) in IntelliJ, the IDE constructs classpaths directly from module workspace outputs (e.g., `dspace-api/target/classes`) rather than querying `~/.m2/repository`.
* Populating the local `.m2` repository offers zero additional runtime benefit to the IDE runner.

**3. Reduces Local Disk Churn and Wear**
* Over repeated iterations, writing multi-megabyte deployment archives to `~/.m2/repository` consumes disk storage and increases disk wear with no corresponding gain in utility.

---

**Comparative Evaluation**

| Metric / Objective | `mvn package -DskipTests` | `mvn install -DskipTests` |
|---|---|---|
| **Assembles `dspace-installer`** | **Yes** (fully generated) | **Yes** (fully generated) |
| **Turnaround Time** | **Shortest** | Longer (unnecessary copying) |
| **Disk I/O & Repository Churn** | **Minimal** | High |
| **Utility for IntelliJ Execution** | **Optimal** | Redundant |

---

**Optimization Strategy: Targeted Submodule Packaging**

To further maximize turnaround speed when only updating configuration or assembly definitions, avoid rebuilding unneeded submodules by targeting the `dspace` module directly:

```bash
mvn package -DskipTests -pl dspace -am
```

* `-pl dspace` (`--projects dspace`): Constrains execution to the assembly module.
* `-am` (`--also-make`): Builds prerequisite modules only when necessary.

---

**Summary**

For optimal productivity, configure your pre-launch step or terminal command using:
```bash
mvn package -DskipTests
```
This strategy achieves the exact required filesystem state while minimizing wasted time and system resources.