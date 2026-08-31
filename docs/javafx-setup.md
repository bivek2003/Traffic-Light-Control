# JavaFX Setup

This guide installs JavaFX for the Traffic Flow Controller project. Member 4 needs it to build the traffic display. Other members only need it if they want to run the full system locally.

## Why JavaFX is a separate install

JavaFX was removed from the JDK after Java 10. Installing a JDK does not install JavaFX, so a plain `java` or `javac` command cannot compile or run the display until JavaFX is added.

Two ways to add it:

| Approach | Best for | Effort |
| --- | --- | --- |
| Maven (recommended) | Everyone on the team | The build tool downloads JavaFX automatically |
| Manual SDK | Courses that require plain `javac` | Each member downloads and configures paths by hand |

Use Maven unless the course requires otherwise. Maven downloads the correct JavaFX build for each operating system, so a Mac member and a Windows member run the same command with no changes.

## Requirements

- A JDK, version 24 or newer, for JavaFX 26.
- Internet access on the first build only. Maven caches JavaFX in `~/.m2/repository` afterwards.

Check the installed JDK:

```bash
java -version
```

Match the JavaFX version to the JDK:

| JDK | JavaFX version | Notes |
| --- | --- | --- |
| 26 | `26.0.2` | Verified working for this project |
| 25 | `25.0.4` | Long-term support release |
| 21 | `21.0.7` | Long-term support release |

JavaFX 26 is compiled for class file version 68, so it fails on any JDK older than 24. If a teammate has an older JDK, they either upgrade the JDK or drop to a matching JavaFX version from the table.

## Option A: Maven

### 1. Install Maven

macOS:

```bash
brew install maven
```

Windows, using Chocolatey, or by unpacking the archive from `https://maven.apache.org/download.cgi` and adding its `bin` folder to `PATH`:

```bash
choco install maven
```

Linux:

```bash
sudo apt install maven
```

Confirm the install, and check that the reported Java version is 24 or newer:

```bash
mvn -v
```

### 2. Declare JavaFX in `pom.xml`

Place this file in the project root:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>edu.cs460</groupId>
  <artifactId>traffic-light-control</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.release>26</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <javafx.version>26.0.2</javafx.version>
    <main.class>edu.cs460.traffic.ui.TrafficApp</main.class>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-controls</artifactId>
      <version>${javafx.version}</version>
    </dependency>
    <dependency>
      <groupId>org.openjfx</groupId>
      <artifactId>javafx-fxml</artifactId>
      <version>${javafx.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.15.0</version>
      </plugin>
      <plugin>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-maven-plugin</artifactId>
        <version>0.0.8</version>
        <configuration>
          <mainClass>${main.class}</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

`javafx-controls` pulls in `javafx-base` and `javafx-graphics` automatically. Add `javafx-media` only if the display needs sound.

### 3. Use the standard source layout

Maven expects this structure. The `main.class` value above must match the real package and class name:

```text
Traffic-Light-Control/
├── pom.xml
└── src/
    ├── main/java/edu/cs460/traffic/ui/TrafficApp.java
    ├── main/resources/
    └── test/java/
```

### 4. Build and run

```bash
mvn clean compile
mvn javafx:run
```

The first build downloads JavaFX; later builds use the cache.

### 5. Confirm the correct platform build

Maven picks the native build from the operating system. Check that a platform-specific jar was downloaded:

```bash
ls ~/.m2/repository/org/openjfx/javafx-base/26.0.2/
```

The listing must include a classified jar such as `javafx-base-26.0.2-mac-aarch64.jar`. Classifiers are `mac-aarch64` for Apple Silicon, `mac` for Intel Macs, `win` for Windows, and `linux` for Linux.

The unclassified `javafx-base-26.0.2.jar` is only a few hundred bytes. That is expected: it is a placeholder, and the real code lives in the classified jar.

## Option B: Manual SDK

Use this only if the course forbids build tools.

1. Download the JavaFX SDK for your operating system and architecture from `https://gluonhq.com/products/javafx/`.
2. Unpack it and note the path to its `lib` directory.
3. Store that path in a variable so the commands stay short:

```bash
export PATH_TO_FX=/path/to/javafx-sdk-26.0.2/lib
```

Compile:

```bash
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml \
      -d out $(find src -name "*.java")
```

Run:

```bash
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml \
     -cp out edu.cs460.traffic.ui.TrafficApp
```

On Windows use `%PATH_TO_FX%` and separate paths with `;` instead of `:`.

Every member must download the SDK for their own operating system, and the `--module-path` flag is required on every command. Losing these flags is the most common cause of the runtime error below.

## Checking the JavaFX version

JavaFX has no system-wide command. Unlike `java -version`, there is nothing to type that reports a global JavaFX version, because JavaFX is a per-project dependency rather than a machine-wide install. Use these four checks instead, in order.

### 1. Confirm the JDK does not bundle JavaFX

```bash
java --list-modules | grep javafx
```

Expect no output. An empty result is correct for Java 11 and newer, and confirms JavaFX must come from Maven or a manual SDK.

### 2. Check the version Maven downloaded

```bash
ls ~/.m2/repository/org/openjfx/javafx-base/
```

The directory name is the version, for example `26.0.2`. If the folder does not exist, no build has downloaded JavaFX yet. Run `mvn clean compile` first.

Manual SDK users check the folder name instead, such as `javafx-sdk-26.0.2`.

### 3. Start the toolkit and print the version

Checks 1 and 2 only prove that files exist. This check proves JavaFX actually runs, because it loads the native graphics library for the platform.

Inside a project, add this line to any JavaFX class and run `mvn javafx:run`:

```java
System.out.println(System.getProperty("javafx.runtime.version"));
```

To check without a project, save this file as `FxCheck.java`:

```java
import javafx.application.Platform;

public class FxCheck {
    public static void main(String[] args) {
        Platform.startup(() -> {
            System.out.println("JavaFX runtime : " + System.getProperty("javafx.runtime.version"));
            System.out.println("Java runtime   : " + System.getProperty("java.version"));
            System.out.println("Toolkit start  : OK");
            Runtime.getRuntime().halt(0);
        });
    }
}
```

Run it against the JavaFX `lib` folder, or against a folder holding the jars from the Maven cache:

```bash
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml \
     --enable-native-access=javafx.graphics FxCheck.java
```

Expected output:

```text
JavaFX runtime : 26.0.2+3
Java runtime   : 26.0.2.1
Toolkit start  : OK
```

`Toolkit start : OK` is the line that matters. It means the native library loaded and the graphics stack started, not merely that jars are present on disk.

The program calls `Runtime.getRuntime().halt(0)` rather than `Platform.exit()`. On macOS, `Platform.exit()` from a `Platform.startup()` block prints the version correctly and then crashes during shutdown with `NSApp with wrong _running count`. The crash is harmless but looks alarming, so `halt` keeps the check clean. Application code that extends `Application` is unaffected and should still use `Platform.exit()`.

### 4. Check the version the build resolves

```bash
mvn dependency:tree -Dincludes=org.openjfx
```

Sample output on Apple Silicon:

```text
\- org.openjfx:javafx-controls:jar:26.0.2:compile
   +- org.openjfx:javafx-controls:jar:mac-aarch64:26.0.2:compile
   \- org.openjfx:javafx-graphics:jar:26.0.2:compile
      +- org.openjfx:javafx-graphics:jar:mac-aarch64:26.0.2:compile
      \- org.openjfx:javafx-base:jar:26.0.2:compile
         \- org.openjfx:javafx-base:jar:mac-aarch64:26.0.2:compile
```

Each module appears twice: once as the placeholder jar and once with the platform classifier. Both lines are expected. This is the fastest way to spot a teammate running a different version, or a machine that resolved the wrong platform build.

## Do not add `module-info.java`

Many JavaFX tutorials add a `module-info.java` file. This project does not use one. Every new package would need an `exports` or `opens` entry, which creates repeated merge conflicts when five members edit one repository. The Maven plugin sets the module path correctly without it.

## IDE setup

Import the project as a Maven project and the IDE reads `pom.xml` for the JavaFX paths. No further configuration is needed.

- **IntelliJ IDEA:** File → Open → select `pom.xml` → Open as Project.
- **VS Code:** install the Extension Pack for Java, then open the project folder.
- **Eclipse:** File → Import → Maven → Existing Maven Projects.

Set the project JDK to 24 or newer if the IDE warns about the language level.

## Troubleshooting

**`Error: JavaFX runtime components are missing`**

The application ran without the JavaFX module path. Use `mvn javafx:run` rather than running the class directly, or add the `--module-path` and `--add-modules` flags from Option B.

**`UnsupportedClassVersionError`**

The JDK is older than JavaFX requires. Check `java -version` and either upgrade the JDK or choose a matching JavaFX version from the table above.

**`Module javafx.controls not found`**

The dependency is missing from `pom.xml`, or the manual `--add-modules` list is incomplete.

**`Graphics Device initialization failed`**

Maven resolved the wrong platform build, usually after copying a `~/.m2` folder between machines. Delete the JavaFX cache and rebuild:

```bash
rm -rf ~/.m2/repository/org/openjfx
mvn clean compile
```

**The build cannot download JavaFX**

The first build needs internet access. On a restricted network, build once elsewhere and copy `~/.m2/repository/org/openjfx`, but only between machines with the same operating system and architecture.

## Verified configuration

This setup was confirmed working on the following combination:

| Item | Value |
| --- | --- |
| Operating system | macOS on Apple Silicon (arm64) |
| JDK | 26.0.2.1 |
| Maven | 3.9.16 |
| JavaFX | 26.0.2 |
| Native build resolved | `mac-aarch64`, automatically |
| Run command | `mvn javafx:run` |
