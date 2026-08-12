# UM Mise README

This document explains how `mise` manages the development environment for DSpace and how to configure your tools to use it.

## Overview

Mise is a tool manager that ensures you are using the exact versions of Java and Maven required for the project, as defined in `.mise.toml`.

```toml
[tools]
java = "temurin-17"
maven = "3.9.16"
```

## How Mise Works

### Environment Interception
When you enter the DSpace directory, `mise` automatically updates your `PATH` and `JAVA_HOME` environment variables. This requires `mise` to be activated in your shell. 

Add the following to your shell profile (e.g., `~/.zshrc` or `~/.bashrc`):
```bash
eval "$(mise activate zsh)"
```

### Tool Resolution
Because `mise` places its shims at the beginning of your `PATH`, when you run `mvn` or `java`, the system executes the specific versions managed by `mise`.

## Command Line Usage

Before building the project for the first time, or when `.mise.toml` changes:

```bash
mise trust
mise install
```

Once installed, any standard command will use the managed versions:
```bash
mvn clean install
java -version
```

## IntelliJ IDEA Configuration

IntelliJ IDEA does not automatically inherit Mise's dynamic environment changes unless launched from a terminal where `mise` is already active. It is recommended to point the IDE to the specific SDKs manually.

### Locating Mise Installs
You can find where `mise` has installed the tools using the following commands:

```bash
mise where java
# Example: /Users/gkostin/.local/share/mise/installs/java/temurin-17

mise where maven
# Example: /Users/gkostin/.local/share/mise/installs/maven/3.9.16
```

### Configuring the Project SDK
1. Open **File > Project Structure > Project**.
2. Under **SDK**, click **Add SDK > JDK...**.
3. Navigate to the path returned by `mise where java`.

### Configuring Maven
1. Open **Settings > Build, Execution, Deployment > Build Tools > Maven**.
2. Set **Maven home path** to the path returned by `mise where maven`.
3. Ensure the **JDK for importer** and **Runner JDK** are set to the `mise` managed Java version.
