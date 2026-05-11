# Introduction

To build and distribute the **System Watch** application, we implemented a Maven-based build pipeline 
that compiles the source code, packages dependencies, and generates a Windows installer for end users 
in an efficient and user-friendly way.

Video Overview Link: https://www.youtube.com/watch?v=ZKmV76ZSntE

---

# Running the System Watch App

If you only want to run the installer without wanting to do any building or packaging,
the installer is located at:

```text
System Watch/target/installer/SystemWatch-1.0.0.msi
```

The generated installer includes:

- `SystemWatch.exe`
- bundled runtime libraries
- application resources
- required Java modules

If you also want to build and package this project, keep reading.

---


# Prerequisites

The following tools and software are required in order to build and package the project.

## GitHub Repository

All source code and build configuration files are located in the GitHub repository.

Clone the repository using:

```bash
git clone https://github.com/liam1202/Capping-Project-Spring2026.git
```

---

## IDE

**IntelliJ IDEA** is recommended because it provides built-in Maven support and preset run configurations that simplify the build process.

> If the Maven sidebar does not appear, right-click the `pom.xml` file inside the `System Watch/` directory and select:
>
> `Add as Maven Project`

---

## SDK

This project uses **OpenJDK 26**.

OpenJDK 26 can be downloaded and configured directly within IntelliJ IDEA.

---

## jpackage

`jpackage` is required to generate the distributable installer.

If using OpenJDK 26, `jpackage` should already be included automatically.

Verify installation with:

```bash
jpackage --version
```

If the command is not recognized, ensure that the Java `bin/` directory is correctly added to your system `PATH` environment variable.

---

## WiX Toolset

The **WiX Toolset** is required to generate the `.msi` installer.

Recommended version:

- WiX v3.14.1

Download:

- https://github.com/wixtoolset/wix3/releases/tag/wix3141rtm

---

# Maven Build Commands

The following Maven commands were used throughout the build workflow.

## Clean and Package

```bash
mvn clean package
```

This command:

- Deletes previous build artifacts
- Creates a fresh build environment
- Compiles all Java source code into `.class` files
- Packages the application and dependencies into a `.jar` file

---

## Build Runtime Image

```bash
mvn javafx:jlink
```

This command:

- Builds a custom Java runtime image
- Bundles required Java and JavaFX modules
- Removes the need for end users to manually install Java

---

## IntelliJ Alternative

If using IntelliJ IDEA, you can instead run the preset configuration:

```text
Build + Package
```

---

# Compilation and Target Directory

When the build process is executed, Maven automatically generates a `target/` directory.

This directory contains all compiled and packaged artifacts, including:

- `.class` files → compiled Java bytecode
- `.jar` files → packaged application and dependencies
- `.lst` and metadata files → intermediate Maven outputs
- `jlink` runtime image → custom Java runtime environment
- installer outputs → packaged distributables

The `target/` directory is regenerated each time Maven runs with the `clean` lifecycle.

---

# Packaging Into an Installer

After compilation, the application is packaged into a native Windows installer using `jpackage`.

This process:

- Wraps the application into a Windows executable
- Bundles the custom Java runtime image
- Generates a distributable `.msi` installer

The installer is located at:

```text
System Watch/target/installer/SystemWatch-1.0.0.msi
```

The generated installer includes:

- `SystemWatch.exe`
- bundled runtime libraries
- application resources
- required Java modules

---

# Maven jpackage Commands

The following commands are used to create the distributable installer.

## Verify Target Directory

```powershell
Get-ChildItem target
```

This confirms that the `target/` directory was successfully created.

> Make sure the command is executed from inside the `System Watch/` directory.

---

## Generate Installer

```bash
mvn -Pdist exec:exec@jpackage-app-image exec:exec@jpackage-msi
```

This command performs two packaging tasks:

1. Creates the application runtime image
2. Generates the final `.msi` installer

---

## IntelliJ Alternative

If using IntelliJ IDEA, you can instead run:

```text
Create Installer
```

---

# Installation Process

The generated installer allows users to install and run the application without any additional setup.

Users only need to launch:

```text
SystemWatch-1.0.0.msi
```

located in:

```text
System Watch/target/installer/
```

---

## Installer Features

The installer:

- Requires no separate Java installation
- Requires no JavaFX installation
- Bundles all dependencies automatically
- Uses a standard Windows installation workflow

During installation:

- The application is copied into a local installation directory (typically `Program Files`)
- A desktop shortcut is automatically created
- `SystemWatch.exe` is registered for easy access

---

# Running the Application

After installation, users can launch the application by:

- Double-clicking the desktop shortcut
- Launching the executable through Command Prompt

Example:

```bash
./SystemWatch.exe
```
