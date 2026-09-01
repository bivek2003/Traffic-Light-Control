# Device Simulator

The device simulator is plain Java. It does not require Maven, Gradle, JavaFX,
or another library. It owns traffic lights, pedestrian buttons, vehicle
detectors, and the auxiliary system device.

## Build

Run this from the repository root:

```sh
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
```

Java 11 or newer is required.

## Test

```sh
java -ea -cp out trafficcontrol.device.DeviceStateTest
java -ea -cp out trafficcontrol.device.DeviceSimulatorLogicTest
java -ea -cp out trafficcontrol.device.DeviceSystemTest
```

`DeviceSystemTest` starts the real Multiplexor and controller. It checks
registration, command routing, a vehicle event, a green-light response, and
the all-red fail-safe after controller loss.

## Start the complete Java system

Compile first, then use three terminal windows in this order:

```sh
java -cp out multiplexor.Multiplexor 5050
```

```sh
java -cp out trafficcontrol.device.DeviceSimulatorMain localhost 5050
```

```sh
java -cp out trafficcontrol.controller.TrafficController localhost 5050
```

The simulator registers `device-hub` and every individual device ID. This
allows the Multiplexor to deliver controller commands addressed to IDs such as
`light-north` and `button-east`.

## JavaFX connection

The device module stays independent from JavaFX. The display connects through
the simulator's public methods:

- `addStateListener` receives every device state change.
- `pressPedestrian` sends a pedestrian request.
- `vehicleDetected` and `vehicleCleared` send detector events.
- `setAuxiliaryStatus` sends `NORMAL`, `FAULT`, or `OFFLINE`.

The JavaFX thread should register a state listener and update its display with
`Platform.runLater`. The shared protocol uses `YELLOW`; the current display
branch calls the same color `AMBER`, so the display must map or rename that
value during integration.

## Fail-safe behavior

The simulator changes every traffic light to red when:

- the Multiplexor socket closes;
- the Multiplexor reports that the controller disconnected; or
- a periodic auxiliary status message reports that `controller` is no longer
  a connected destination.
