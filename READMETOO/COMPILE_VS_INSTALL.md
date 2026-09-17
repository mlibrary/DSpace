**Comparison: `mvn clean install` vs. `mvn compile`**

`mvn clean install` actually does **more** than `mvn compile`—it encompasses the entire `compile` phase and goes several steps further in Maven's default build lifecycle:

1. **`clean` lifecycle**: Removes the `target/` directories across all modules.
2. **`compile` phase**: Compiles the source code into `.class` files in `target/classes`.
3. **`test` phase** (skipped via `-DskipTests`): Compiles and runs unit tests.
4. **`package` phase**: Packages the compiled code into `.jar` / `.war` archives in `target/`.
5. **`install` phase**: Copies the packaged archives into your local Maven repository cache (`~/.m2/repository`).

Running `mvn clean install` already performs everything `mvn compile` does.

---

**Why the Classpath JAR Scan Warning Still Occurs**

The warning you observed:

```text
WARN org.apache.tomcat.util.scan.StandardJarScanner @ Failed to scan [file:/Users/gkostin/.m2/repository/xalan/xalan/2.7.0/serializer.jar]
Caused by: java.nio.file.NoSuchFileException: /Users/gkostin/.m2/repository/xalan/xalan/2.7.0/serializer.jar
```

is caused by a known legacy quirk between Tomcat's `StandardJarScanner` and `xalan:xalan:2.7.0`:

1. **Manifest `Class-Path` Entry**:
   Inside `xalan-2.7.0.jar`, the `META-INF/MANIFEST.MF` file contains an internal header:
   ```manifest
   Class-Path: serializer.jar xml-apis.jar xercesImpl.jar
   ```
2. **Tomcat's Relative Path Lookup**:
   During application startup, embedded Tomcat scans all JARs on the classpath for TLDs and web fragments. When it inspects `~/.m2/repository/xalan/xalan/2.7.0/xalan-2.7.0.jar`, it reads that manifest `Class-Path` attribute and tries to resolve `serializer.jar` relative to that directory (`~/.m2/repository/xalan/xalan/2.7.0/serializer.jar`).
3. **Maven Repository Layout**:
   In standard Maven repository layouts, `serializer.jar` is located in its own artifact directory (`~/.m2/repository/xalan/serializer/2.7.0/serializer-2.7.0.jar`), rather than inside the `xalan/xalan/2.7.0/` folder.
4. **Result**:
   Tomcat cannot find `serializer.jar` at that relative path, logs a `NoSuchFileException` warning, and safely continues startup without issue.

---

**Conclusion**

Your build executed properly, and all required runtime dependencies are present. This warning is harmless log noise during startup and does not affect the operation of DSpace.