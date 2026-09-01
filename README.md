# Traffic-Light-Control

CS 460 class project implemented with Java and JavaFX. Java handles the controller, socket communication, device simulation, and tests. JavaFX displays a six-lane road with three lanes in each direction, opposing traffic, and a yellow center line.

## System architect deliverables

- [System architecture](docs/system-architecture.md)
- [Device interface specification](docs/device-interface-specification.md)
- [Integration plan](docs/integration-plan.md)

## Project structure

The project uses the standard `src/main/java` and `src/test/java` layout and
direct `javac` commands. This keeps the shared protocol and controller free of
external dependencies while the JavaFX module is developed separately.

The controller entry point is
`trafficcontrol.controller.TrafficController`. The obsolete empty root-level
`main.java` on the Multiplexor branch should not be merged.

## Build and test controller

Java 11 or newer is required. From the repository root:

```sh
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -ea -cp out trafficcontrol.controller.TrafficControllerLogicTest
java -ea -cp out trafficcontrol.controller.TrafficControllerSocketTest
```

No network connection or third-party test library is required. The test suite
checks protocol parsing, registration, vehicle and pedestrian requests, safe
yellow/all-red transitions, invalid events, device faults, socket routing, and
connection loss.

## Run the controller

Start the Multiplexor first, then run:

```sh
java -cp out trafficcontrol.controller.TrafficController [host] [port]
```

The defaults are `localhost` and port `5050`. The expected complete-system
startup order is:

1. Multiplexor
2. JavaFX simulator with its device simulator
3. Traffic controller
4. Test harness

On startup the controller registers as `controller` and commands every light
to red before processing events.

## Device simulator

Member 3's build, test, startup, and JavaFX connection instructions are in
[docs/device-simulator.md](docs/device-simulator.md).
