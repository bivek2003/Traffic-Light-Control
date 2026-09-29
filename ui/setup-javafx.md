# JavaFX setup

Use JDK 17 or newer and JavaFX 17. The team plan targets Java 17; builds use
`--release 17`. The previous JavaFX 26 instructions are no longer required.

## macOS and Linux x64

From the repository root:

```sh
java -version
bash ui/setup-javafx.sh
bash ui/run.sh
```

The setup script downloads OpenJFX 17.0.16 base, graphics and controls jars
with the matching native libraries from Maven Central. They stay in ignored
`.deps/javafx-17.0.16`, not Git. It does not change your installed JDK.

## Existing SDK or Windows

Download a JavaFX 17 SDK for your operating system and CPU from
https://gluonhq.com/products/javafx/ and set `PATH_TO_FX` to its `lib` folder.
On macOS/Linux the build/run scripts use that variable instead of `.deps`.

Windows PowerShell, from the repository root:

```powershell
$env:PATH_TO_FX = 'C:\javafx-sdk-17\lib'
New-Item -ItemType Directory -Force out
$sources = Get-ChildItem src/main/java,ui -Recurse -Filter *.java
javac --release 17 --module-path $env:PATH_TO_FX --add-modules javafx.controls -d out $sources.FullName
java --module-path $env:PATH_TO_FX --add-modules javafx.controls -cp out controller.Controller
```

## Checks and common errors

- `bash ui/build.sh` compiles without opening a window.
- `bash ui/test.sh` runs object/access, simulation and legacy regression checks.
- Timed GUI scenarios are tested locally; their runner is pending the next batch.
- Missing `javafx.controls`: check `PATH_TO_FX` points at the SDK's `lib`.
- Native-library/graphics startup error: check SDK operating system and CPU.
- Class version error: check Java 17+ and a compatible JavaFX 17 SDK.
- JDK 24 may print JavaFX's `sun.misc.Unsafe` deprecation warning; this is not
  a controller failure. The controller does not use that API.

For an IDE, add `src/main/java` and `ui` as source roots and the three JavaFX
jars as libraries. Use the same module-path options when launching
`controller.Controller`. Keep machine-specific IDE paths out of Git.
