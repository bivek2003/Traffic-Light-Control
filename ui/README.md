# Traffic display

The JavaFX display for the traffic controller project. This is the JavaFX
simulator, step 3 in the startup order in the root README.

It draws the intersection: two roads crossing, six lanes on each, three lanes
in each direction, with a double yellow centre line between the opposing
traffic. Vehicles queue at a red and move off on green, pedestrians cross when
a button is pressed, and every message the display would put on the wire is
shown as it happens.

Plain JavaFX. No Maven, no Gradle, no FXML.

## Why this is not under src/main/java

Everything else compiles with a plain `javac` and no module path:

```sh
javac -d out $(find src/main/java src/test/java -name '*.java')
```

JavaFX needs `--module-path` and `--add-modules` on every command. If these
files sat under `src/main/java` that command would try to compile them and
fail, so the display is kept in its own folder and built by its own script.
This is what the root README means by the JavaFX module being developed
separately. Happy to move it if the team would rather have it somewhere else.

## Requirements

The controller needs Java 11 or newer. **The display needs JDK 24 or newer**,
because JavaFX 26 is compiled for class file version 68 and will not load on
anything older.

```sh
java -version
```

## Installing JavaFX

JavaFX is not part of the JDK, so it has to be installed separately.

1. Download the SDK for your machine from https://gluonhq.com/products/javafx/
2. Unpack it to `~/javafx/javafx-sdk-26.0.2`

If you keep it elsewhere, point at its `lib` folder instead:

```sh
export PATH_TO_FX=/path/to/javafx-sdk-26.0.2/lib
```

## Running it

From the repository root:

```sh
./ui/run.sh
```

That compiles if anything changed, then starts the display. To compile without
running:

```sh
./ui/build.sh
```

Both scripts only wrap these two commands, so you can run them by hand:

```sh
javac --module-path $PATH_TO_FX --add-modules javafx.controls -d out ui/*.java
java --module-path $PATH_TO_FX --add-modules javafx.controls -cp out ui.TrafficApp
```

There is no stylesheet and nothing to copy. Colours and fonts are set through
the JavaFX API in `Theme.java`, so the display depends on nothing but JavaFX.

Output goes to `out/`, the same folder the controller build uses, and `out/` is
already in `.gitignore`.

## Using it

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

## If your editor shows errors

VS Code and IntelliJ work out the classpath from a build file, and this project
does not have one, so they report every `javafx` import as unresolved even
though the code compiles. The code is fine; the editor cannot see the SDK.

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
our own, which is why `.vscode/` is already in `.gitignore`.

## What is in here

| File | What it does |
| --- | --- |
| `TrafficApp.java` | Starts the app, builds the window, runs the animation loop |
| `IntersectionView.java` | Draws the road, the markings, the vehicles and the people |
| `Simulation.java` | The signal cycle, the traffic, the crossings, and the messages |
| `Vehicle.java` | One vehicle on one approach |
| `Pedestrian.java` | One person on one crossing |
| `Palette.java` | Colours used on the canvas |
| `Theme.java` | Colours and fonts for the window, and the light and dark palettes |

## How it works

Everything is drawn in a fixed 800 by 800 world and scaled to whatever size the
canvas is, so no measurement in the drawing code changes when the window does.
The canvas sizes itself from the screen at startup, so it fills a large display
without falling off a small one.

Vehicles and pedestrians both hold their position as a single number: the
distance travelled along their approach, or across their crossing. One travel
coordinate instead of x and y means all four approaches share the same movement
code, and the stop line works out to the same value for every one of them.

Approaching traffic meets the stop bar, then the crosswalk, then the junction,
in that order. `Simulation.STOP_T` is derived from where the stop bar is drawn
rather than typed in separately, so the paint and the place vehicles actually
stop cannot drift apart.

Traffic leaving the junction crosses the far side of the intersection just as a
red begins, so people wait at the kerb until that has cleared, and the signal
will not advance to a green while anybody is still on the road.

## Lane numbering

Lanes count outward from the yellow, so `NORTH_LANE_1` is the lane against the
centre line and `NORTH_LANE_3` is the kerb lane. The interface document does not
say which way round this goes, so I picked one. It is a one line change if the
team wants it the other way.

## Connecting it to the rest of the system

The display runs its own signal cycle for now so it can be worked on by itself.
At integration that is replaced by real messages:

- `Simulation.applyColour` is the only place a light changes colour, so it
  becomes the handler for an incoming `COMMAND` from the controller.
- `Simulation.MessageSink` already receives every outgoing message, so it gets
  pointed at a socket instead of at the log panel.
- The display registers like any other program, `REGISTER|display|mux|CONNECT|`,
  and waits for the multiplexor to reply before sending anything.
- The multiplexor listens on port 5050.

Both of those seams should use `trafficcontrol.protocol.Message` rather than the
strings the display formats today, now that a shared protocol class exists on
main. I left that out of this pull request to keep it to the display alone.

## To do

- [x] draw the six lanes and the yellow centre line
- [x] draw the four signal heads
- [x] vehicles that queue at a red and move off on green
- [x] pedestrian buttons, with people who actually cross
- [x] show the messages the display would send
- [ ] use `trafficcontrol.protocol.Message` instead of formatting strings
- [ ] connect to the multiplexor instead of running the cycle locally
- [ ] fall back to all red if the controller disconnects
