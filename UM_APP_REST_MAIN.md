# Application Entry Point (Application.java)

This document explains the role of the `main` method in `org.dspace.app.rest.Application` and how it facilitates development within IntelliJ IDEA.

## The `main` Method

In the `dspace-server-webapp` module, the `Application.java` class includes a standard Java `main` entry point:

```java
public static void main(String[] args) {
    new SpringApplicationBuilder(Application.class)
            .initializers(new DSpaceKernelInitializer(), new DSpaceConfigurationInitializer())
            .run(args);
}
```

### Purpose for IDE Development
Adding this method allows developers to launch the DSpace REST API directly from IntelliJ IDEA as a standard Java application. By clicking the "Run" or "Debug" icon next to the `main` method, the IDE executes the Spring Boot bootstrap process.

This facilitates a faster development cycle by:
*   Eliminating the need to install and manage a standalone Tomcat server locally.
*   Simplifying the Run Configuration (no complex WAR deployment setups).
*   Enabling rapid debugging using the IDE's built-in debugger.

## Embedded vs. Integrated Tomcat
While IntelliJ IDEA has features to manage external Tomcat servers, this setup leverages **Spring Boot's Embedded Tomcat**.

*   **Self-Contained**: The Tomcat server is included as a dependency in the DSpace project. When the `main` method is executed, the application starts its own Tomcat instance.
*   **IDE Integration**: IntelliJ IDEA treats the running application like any other Java process, providing integrated console logs, thread monitoring, and one-click restarts.
*   **Startup**: The embedded server starts significantly faster than an external container because it only loads the specific application context required for the REST API.

## Production Compatibility (No Interference)
The presence of the `main` method does not interfere with production releases where DSpace is typically deployed as a WAR file into a standalone Tomcat instance.

The `Application` class supports both modes by extending `SpringBootServletInitializer` and overriding the `configure` method:

```java
public class Application extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class)
                          .initializers(new DSpaceKernelInitializer(), new DSpaceConfigurationInitializer());
    }
}
```

### Deployment Behavior:
*   **IDE/Executable JAR**: When run via `main()`, the embedded Tomcat is used.
*   **Standalone Tomcat (Production)**: When the WAR is deployed to an external Tomcat, the container ignores the `main()` method entirely. Instead, it uses the `configure()` method to initialize the application. This ensures that the production environment remains standard and unaffected by the development-time convenience of the `main` method.
