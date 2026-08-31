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
javac -d out $(find src/main/java -name '*.java')
javac -cp out -d out $(find src/test/java -name '*.java' ! -path 'src/test/java/ui/*')
java -ea -cp out trafficcontrol.controller.TrafficControllerLogicTest
java -ea -cp out trafficcontrol.controller.TrafficControllerSocketTest
java -ea -cp out trafficcontrol.testharness.TestHarnessScriptTest
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

## Run test harness

The entry point is `trafficcontrol.testharness.TestHarness`. It
connects to the Java Multiplexor, registers as `test-harness`, runs scripted
socket test cases, validates responses, and prints a pass/fail report.

Compile first:

```sh
mkdir -p out
javac -d out $(find src/main/java -name '*.java')
javac -cp out -d out $(find src/test/java -name '*.java' ! -path 'src/test/java/ui/*')
```

Run the default Multiplexor smoke test:

```sh
java -cp out trafficcontrol.testharness.TestHarness
```

Run explicit scripts:

```sh
java -cp out trafficcontrol.testharness.TestHarness localhost 5050 \
  test-scripts/test-multiplexer-smoke.tfs \
  test-scripts/test-controller-invalid-source.tfs
```

Script files support `SEND`, `SEND_RAW`, `EXPECT`, `EXPECT_FIELDS`, `WAIT`,
and `TIMEOUT`. `EXPECT_FIELDS` compares the five protocol fields and allows
`*` as a wildcard field.
