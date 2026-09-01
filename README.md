# Traffic Light Control Prototype

A small Java/JavaFX prototype built around the required workflow:

```text
Main test harness -> Multiplexor -> socket -> JavaFX digital twin
```

`Main` reads `scripts/test-devices.txt`. The `Multiplexor` routes each command
over one socket to `DigitalTwinSimulator`, which owns the device objects and
returns their new state. The JavaFX window also generates moving traffic; cars
randomly travel straight, turn left, or turn right. Every maneuver stops at red
and yellow lights and proceeds only when its approach is green. Vehicle
detection is embedded in the simulation: approaching traffic updates hidden
detector devices and automatically selects the next signal to serve. The UI
therefore shows neither manual signal-head controls nor detector controls.

## Source structure

```text
src/main/java/
├── Main.java
├── Multiplexor.java
├── Message.java
├── ui/
│   ├── DigitalTwinSimulator.java
│   ├── IntersectionView.java
│   ├── TrafficSimulation.java
│   └── Vehicle.java
└── devices/
    ├── Device.java
    ├── TrafficLight.java
    ├── PedestrianButton.java
    └── VehicleDetector.java

scripts/
└── test-devices.txt
```

The source files deliberately use the default package to keep this classroom
prototype small while retaining the requested folders.

## Build

Set `PATH_TO_FX` to the JavaFX SDK `lib` directory, then run:

```sh
mkdir -p out
javac --module-path "$PATH_TO_FX" --add-modules javafx.controls \
  -d out $(find src/main/java -name '*.java')
```

## Run

Open three terminals in the project directory.

Terminal 1 — start the router:

```sh
java -cp out Multiplexor
```

Terminal 2 — start the JavaFX digital twin:

```sh
java --module-path "$PATH_TO_FX" --add-modules javafx.controls \
  -cp out DigitalTwinSimulator localhost 5050
```

Terminal 3 — execute the device script with `Main`:

```sh
java -cp out Main localhost 5050 scripts/test-devices.txt
```

The test passes when every sent command receives the expected device state.

## Script format

```text
SEND TYPE|SOURCE|DESTINATION|ACTION|VALUE
EXPECT TYPE|SOURCE|DESTINATION|ACTION|VALUE
WAIT milliseconds
```

The included script tests all four traffic lights, all four pedestrian buttons,
and all four vehicle detectors.
