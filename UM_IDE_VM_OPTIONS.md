**The Cause of the Issue**

In the command line you provided, the `-D` flags are placed **after** the main class name:

```text
... org.dspace.app.rest.Application -Ddspace.dir=/Users/gkostin/... -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true
```

In Java syntax:
```bash
java [JVM options / system properties] <MainClass> [program arguments]
```

Because `-Ddspace.dir=...` appears after `org.dspace.app.rest.Application`, the JVM treats it as a **program argument** (passed to `String[] args` in `main()`) rather than a **Java System Property** (VM Option). As a result, `System.getProperty("dspace.dir")` remains unset, leading Spring to encounter the unresolved placeholder `${dspace.dir}`.

---

**How to Fix in IntelliJ IDEA**

The arguments were entered into the **Program arguments** field instead of the **VM options** field.

1. In IntelliJ IDEA, open **Run > Edit Configurations...**.
2. Select your `Application` run configuration.
3. Remove `-Ddspace.dir=...`, `-Dserver.servlet.context-path=...`, and `-Djava.net.preferIPv4Stack=true` from the **Program arguments** field.
4. Add them to the **VM options** field:
    - If the *VM options* field is not visible, click **Modify options** (or `Alt+V` / `Cmd+V`) and select **Add VM options**.
    - Paste the following into the **VM options** field:
      ```text
      -Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true
      ```
5. Click **Apply** and re-run the application.

---

**Expected Command Line**

Once configured correctly in IntelliJ IDEA, the generated command line will place the `-D` options before the main class:

```text
java ... -Dfile.encoding=UTF-8 -Ddspace.dir=/Users/gkostin/GitHub/mlibrary/DSpace/dspace/target/dspace-installer -Dserver.servlet.context-path=/server -Djava.net.preferIPv4Stack=true -classpath ... org.dspace.app.rest.Application
```