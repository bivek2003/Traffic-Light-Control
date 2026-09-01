# Setting up and running the JavaFX display

The traffic display is the JavaFX simulator, step 3 in the startup order in the
root README. It draws the intersection, animates the traffic and the people
crossing, and shows the protocol messages as they happen.

Plain JavaFX. No Maven, no Gradle, no FXML, and no stylesheet.

## 1. Check your Java

JavaFX 26 is compiled for class file version 68, so it will not load on an
older runtime.

```sh
java -version
```

**You need JDK 24 or newer for the display.** The controller and the
multiplexor still build on Java 11; this requirement applies only here.

If your JDK is older, install a current one from https://adoptium.net or
https://www.oracle.com/java/technologies/downloads/ before going on.

## 2. Install the JavaFX SDK

JavaFX was removed from the JDK after Java 10, so installing Java does not
install JavaFX. It has to be downloaded separately.

1. Go to https://gluonhq.com/products/javafx/
2. Choose **JavaFX 26.0.2**, your operating system, your architecture, and
   type **SDK**
3. Unpack it to `~/javafx/javafx-sdk-26.0.2`

Pick the right architecture or the native libraries will not load:

| Machine | Download |
| --- | --- |
| Mac, Apple silicon | macOS aarch64 |
| Mac, Intel | macOS x64 |
| Windows | Windows x64 |
| Linux | Linux x64 |

The scripts look in `~/javafx/javafx-sdk-26.0.2` by default. To keep the SDK
somewhere else, point `PATH_TO_FX` at its `lib` folder:

```sh
export PATH_TO_FX=/path/to/javafx-sdk-26.0.2/lib
```

Add that line to `~/.zshrc` or `~/.bashrc` to make it stick.

## 3. Check the SDK is there

```sh
ls ~/javafx/javafx-sdk-26.0.2/lib
```

You should see `javafx.controls.jar`, `javafx.graphics.jar`, `javafx.base.jar`
and, on macOS, a set of `.dylib` files. The native libraries are the part that
has to match your machine.

## 4. Run it

From the repository root:

```sh
./ui/run.sh
```

That compiles anything that changed and starts the display. To compile without
running:

```sh
./ui/build.sh
```

Both scripts wrap these two commands, so you can run them by hand:

```sh
javac --module-path $PATH_TO_FX --add-modules javafx.controls -d out ui/*.java
java --module-path $PATH_TO_FX --add-modules javafx.controls \
     --enable-native-access=javafx.graphics -cp out ui.TrafficApp
```

On Windows, use `%PATH_TO_FX%` and separate paths with `;` rather than `:`.

`--enable-native-access=javafx.graphics` only silences a warning. JavaFX loads
its own native libraries, and without the flag Java 24 and newer print four
lines about restricted methods every time the display starts. It runs either
way.

There is nothing to copy beside the classes. Colours and fonts are set through
the JavaFX API in `Theme.java`, so the display depends on nothing but JavaFX.

Output goes to `out/`, the same folder the controller build uses, and `out/` is
already in `.gitignore`.

## 5. Using it

| Control | What it does |
| --- | --- |
| Pause / Play | Stops and starts the clock |
| Step | Advances one twelfth of a second, for looking at a single frame |
| Speed | Runs the simulation faster or slower |
| Traffic | How often new vehicles arrive |
| Theme | Switches the window between light and dark |
| N / S / E / W | Presses that pedestrian button |

Press a pedestrian button and a group crosses that side. If the road is already
red they set off at once. If it is green the request is held, the button shows
`WAIT`, and the green is cut to two seconds so nobody waits out a whole cycle.
The button shows `WALK` while they are on the crossing, and the light stays red
until the last one is clear.

Tick `detectors` above the message log to include vehicle detector events. They
are hidden by default because there are a great many of them.

## Troubleshooting

**`Error: JavaFX runtime components are missing`**

The program ran without the module path. Use `./ui/run.sh`, or add
`--module-path` and `--add-modules` as shown above.

**`UnsupportedClassVersionError`**

Your JDK is older than JavaFX 26 needs. Check `java -version` and install
JDK 24 or newer.

**`Module javafx.controls not found`**

`PATH_TO_FX` is wrong, or it points at the SDK folder rather than at its `lib`
folder. It must end in `/lib`.

**`Graphics Device initialization failed`, or a crash on startup**

The SDK does not match your machine. Delete it and download the build for your
operating system and architecture again.

**`ui/run.sh: Permission denied`**

```sh
chmod +x ui/run.sh ui/build.sh
```

## Editor setup

VS Code and IntelliJ work out the classpath from a build file, and this project
does not have one, so they mark every `javafx` import as unresolved even though
the code compiles. The code is fine; the editor cannot see the SDK.

For VS Code, create `.vscode/settings.json`:

```json
{
  "java.project.sourcePaths": ["."],
  "java.project.outputPath": "out",
  "java.project.referencedLibraries": [
    "/absolute/path/to/javafx-sdk-26.0.2/lib/javafx.base.jar",
    "/absolute/path/to/javafx-sdk-26.0.2/lib/javafx.graphics.jar",
    "/absolute/path/to/javafx-sdk-26.0.2/lib/javafx.controls.jar",
    "/absolute/path/to/javafx-sdk-26.0.2/lib/javafx.fxml.jar"
  ]
}
```

Then run **Java: Clean Java Language Server Workspace** from the command
palette. Those paths are absolute, so the file cannot be shared. We each need
our own, which is why `.vscode/` is in `.gitignore`.

For IntelliJ, add the SDK's `lib` folder as a project library, and put
`--module-path <lib> --add-modules javafx.controls` in the run configuration's
VM options.

## Why this is not under src/main/java

Everything else compiles with a plain `javac` and no module path:

```sh
javac -d out $(find src/main/java src/test/java -name '*.java')
```

JavaFX needs `--module-path` and `--add-modules` on every command. If these
files sat under `src/main/java`, that command would try to compile them and
fail. So the display keeps its own folder and its own script, which is what the
root README means by the JavaFX module being developed separately. Happy to
move it if the team would rather have it elsewhere.

## What is in this folder

| File | What it does |
| --- | --- |
| `TrafficApp.java` | Starts the app, builds the window, runs the animation loop |
| `IntersectionView.java` | Draws the road, the markings, the vehicles and the people |
| `Simulation.java` | The signal cycle, the traffic, the crossings, and the messages |
| `Vehicle.java` | One vehicle on one approach |
| `Pedestrian.java` | One person on one crossing |
| `Palette.java` | Colours used on the canvas |
| `Theme.java` | Colours and fonts for the window, and the light and dark palettes |
| `build.sh`, `run.sh` | The two commands above, with the SDK check |

Lanes count outward from the yellow, so `NORTH_LANE_1` is the lane against the
centre line and `NORTH_LANE_3` is the kerb lane. The interface document does not
say which way round this goes, so I picked one. It is a one line change if the
team wants it the other way.

## Connecting it to the rest of the system

The display starts the device simulator in the same JavaFX process. Start the
Multiplexor, run `./ui/run.sh localhost 5050`, and then start the controller.
JavaFX sends button and detector events through the device simulator and shows
the device states returned from controller commands.

## To do

- [x] draw the six lanes and the yellow centre line
- [x] draw the four signal heads
- [x] vehicles that queue at a red and move off on green
- [x] pedestrian buttons, with people who actually cross
- [x] show the messages the display would send
- [x] connect to the Multiplexor through the device simulator
- [x] follow controller light states and use all-red fail-safe
