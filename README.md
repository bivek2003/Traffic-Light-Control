# Traffic-Light-Control

Team 5's CS 460 project, following the 26 September controller plan and
Design Diagram V5.5. Java 17 and JavaFX 17; no Maven or Gradle.

## Run the controller and simulation

```sh
bash ui/setup-javafx.sh
bash ui/run.sh
```

The setup script downloads JavaFX for macOS or Linux x64 into ignored `.deps/`.
Alternatively, set `PATH_TO_FX` to your JavaFX 17 SDK's `lib` folder.
See [JavaFX setup](ui/setup-javafx.md) for manual setup and troubleshooting.
No Multiplexor or separate server is needed for this simulator.

The entry point is `controller.Controller`. It selects a mode, completes one
bounded step on the main thread, and selects again. JavaFX displays the same
`SimulatedLabApi` outputs and supplies button/sensor inputs; it does not choose
light phases. The JavaFX application thread is required for rendering.

- Normal: NS left, NS through, EW left, EW through.
- Night: vehicle-actuated phases from 22:00 to 06:00, using the system clock.
- Pedestrian: all-red WALK, then STOP and clearance.
- Emergency: the detected road's through green, up to 60 seconds.

Check an emergency direction while the vehicle is detected; uncheck after it
passes. Request crossing latches a pedestrian request. Vehicle checkboxes add
lane demand; moving vehicles also activate sensors. Turning cars obey signals.
Requests wait until the current step ends. Closing the window also waits for
the current phase and clearance to finish.

## Check the build

```sh
bash ui/build.sh
bash ui/test.sh
bash ui/test.sh --gui
```

The last command opens the real GUI and checks complete timed scenarios, so
allow about six minutes and leave its controls alone. It writes a screenshot
to `out/controller-gui.png`. Timings are not shortened for tests.

## Current code

- `src/main/java/devices`: six-operation lab API and restricted views.
- `src/main/java/objects`: device objects, road/phase mapping and output guards.
- `src/main/java/procedures`: selection and the four documented step signatures.
- `src/main/java/controller`: main-thread dispatch and JavaFX startup.
- `src/main/java/sim`: shared simulator input/output state and recent event log.
- `src/main/java/config`, `util`: plan timings and inline delays.
- `ui/ControllerView.java`: controls and controller-driven intersection display.

[Controller notes](docs/controller-simulation.md) explain the access rules,
checks, and remaining hardware questions. [Emergency notes](docs/emergency-mode.md)
describe the emergency procedure and unresolved selection policies.

## Earlier socket implementation

`trafficcontrol.*`, `multiplexor.*`, and `ui.TrafficApp` remain for comparison
and regression checks. They are not the new controller's runtime or lab adapter.
The separate Git `prototype` branch has not been merged.

Historical references: [architecture](docs/system-architecture.md),
[device protocol](docs/device-interface-specification.md),
[integration](docs/integration-plan.md), [device simulator](docs/device-simulator.md).
Their socket startup instructions describe the earlier implementation.
