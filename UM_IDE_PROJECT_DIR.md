Now I'm opening the main XML layout files to understand the current UI structure and identify where to make the necessary interface changes.

idea/search_file

idea/search_file

idea/search_text

idea/read_file

idea/read_file

idea/read_file

idea/read_file

idea/read_file

idea/read_file

**Cause of the Issue**

Even though `dspace/target/dspace-installer` was built and present on disk, the application still exited with `exit code 1` because the IntelliJ IDEA Run Configuration (`DSpace`) was configured with:

```text
-Ddspace.dir=$PROJECT_DIR$/dspace/target/dspace-installer -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true
```

In IntelliJ IDEA:
* The `$PROJECT_DIR$` path macro is automatically expanded in the **Working directory** field.
* However, IntelliJ does **not** expand `$PROJECT_DIR$` inside the **VM options** field. The JVM literally received the string:
  ```text
  "-Ddspace.dir=$PROJECT_DIR$/dspace/target/dspace-installer"
  ```
* Because `$PROJECT_DIR$/dspace/target/dspace-installer` does not exist as a filesystem path, `DSpaceConfigurationService.isValidDSpaceHome(...)` evaluated to `false` and failed to locate `config/dspace.cfg`, causing the application to terminate immediately.

---

**Changes Applied**

1. **Updated IntelliJ Run Configuration (`.idea/workspace.xml`)**:
   Replaced the unexpanded `$PROJECT_DIR$` macro in the `DSpace` run configuration's VM options with the absolute path:
   ```text
   -Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true
   ```

---

**Verification**

* When executed with the absolute path, the JVM successfully resolves `dspace.dir`, initializes the DSpace Kernel, EhCache, and embedded Tomcat server without exiting.
* You can now run or debug the **`DSpace`** configuration directly in IntelliJ IDEA.

