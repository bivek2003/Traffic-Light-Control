# Device Interface Specification

## Java interface

Every simulated device implements this simple Java interface:

```java
public interface Device {
    String getId();
    String getType();
    String getState();
    void handleCommand(String action, String value);
}
```

Traffic lights, pedestrian buttons, vehicle detectors, and auxiliary devices provide their own implementation of these four methods.

## Socket communication

- The Multiplexor uses Java `ServerSocket`.
- The controller, devices, and test harness use Java `Socket`.
- `BufferedReader` reads one message per line.
- `PrintWriter` sends one message per line.
- The host and port are passed to the program when it starts.

No external communication library is required.

## Message format

Messages are simple text fields separated by `|`:

```text
TYPE|SOURCE|DESTINATION|ACTION|VALUE
```

Example:

```text
COMMAND|controller|light-north|SET_COLOR|GREEN
```

## Message types

| Type | Sender → receiver | Purpose |
| --- | --- | --- |
| `REGISTER` | Any client → Multiplexor | Registers the component ID. |
| `COMMAND` | Controller → device | Changes a device state. |
| `STATE` | Device → controller/JavaFX | Reports the current device state. |
| `EVENT` | Device → controller | Reports a button press or vehicle detection. |
| `ERROR` | Any component → sender | Reports an invalid message or destination. |

## Device contracts

### Traffic light head

Accepts `SET_COLOR` with `RED`, `YELLOW`, or `GREEN`. It returns its current color in a `STATE` message.

### Pedestrian push button

Sends a `PEDESTRIAN_REQUEST` event when pressed. It stores whether a request is active until the controller clears it.

### Vehicle detector

Sends `VEHICLE_DETECTED` when a vehicle arrives and `VEHICLE_CLEARED` when it leaves. Each detector includes its road direction and lane number from `1` to `3`, allowing the controller and JavaFX display to identify vehicles in the three lanes on either side of the yellow center line.

Example:

```text
EVENT|detector-north-2|controller|VEHICLE_DETECTED|NORTH_LANE_2
```

### Auxiliary device

Reports a simple status of `NORMAL`, `FAULT`, or `OFFLINE`.
