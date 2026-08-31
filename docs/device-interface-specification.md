# Device Interface Specification

## Java interface

Every simulated device implements the shared
`trafficcontrol.device.Device` interface in `src/main/java`:

```java
package trafficcontrol.device;

public interface Device {
    String getId();
    String getType();
    String getState();
    void handleCommand(String action, String value);
}
```

Traffic lights, pedestrian buttons, vehicle detectors, and auxiliary devices
provide their own implementations.

## Socket communication

- The Multiplexor uses Java `ServerSocket`.
- The controller, devices, and test harness use Java `Socket`.
- `BufferedReader` reads one message per line.
- `PrintWriter` sends one message per line.
- Socket text is UTF-8 and every message ends with a newline.
- Clients accept host and port as startup arguments.
- The default host is `localhost` and the default port is `5050`.

No external communication library is required.

## Message format

Messages are text fields separated by `|`:

```text
TYPE|SOURCE|DESTINATION|ACTION|VALUE
```

Example:

```text
COMMAND|controller|light-north|SET_COLOR|GREEN
```

Messages contain exactly five fields. `TYPE`, `SOURCE`, `DESTINATION`, and
`ACTION` cannot be empty. `VALUE` may be empty. A field cannot contain `|`, a
carriage return, or a newline. Message types are uppercase and must be one of
the values below.

## Message types

| Type | Sender → receiver | Purpose |
| --- | --- | --- |
| `REGISTER` | Any client → Multiplexor | Registers one immutable component ID. |
| `COMMAND` | Controller → device | Changes a device state. |
| `STATE` | Device → controller or JavaFX | Reports current device state. |
| `EVENT` | Device → controller | Reports a button press or vehicle detection. |
| `ERROR` | Any component → sender | Reports an invalid message or destination. |

## Component IDs

Every socket connection has one unique ID. Standard IDs are:

| Component | ID pattern or value |
| --- | --- |
| Multiplexor | `mux` |
| Controller | `controller` |
| Device simulator gateway | `device-hub` |
| JavaFX display, if connected by socket | `javafx` |
| Test harness | `test-harness` |
| Traffic lights | `light-north`, `light-south`, `light-east`, `light-west` |
| Pedestrian buttons | `button-north`, `button-south`, `button-east`, `button-west` |
| Vehicle detectors | `detector-<direction>-<lane>`, lane `1` through `3` |
| Auxiliary devices | `aux-<name>` |

IDs use lowercase ASCII words separated by hyphens. Each physical or
simulated device ID must be unique.

## Registration

A client registers before sending any other message. The controller uses:

```text
REGISTER|controller|mux|CONNECT|CONTROLLER
```

`VALUE` identifies the component role: `CONTROLLER`, `DEVICE_HUB`, `JAVAFX`,
or `TEST_HARNESS`. Successful registration returns:

```text
STATE|mux|controller|REGISTERED|OK
```

The registered ID is immutable for the connection's lifetime. A second
`REGISTER` from the same connection returns `ALREADY_REGISTERED`. If another
connection owns the requested ID, the Multiplexor returns `DUPLICATE_ID` and
does not replace the existing connection. For every later message, `SOURCE`
must equal the registered ID; otherwise the Multiplexor returns
`SOURCE_MISMATCH` and does not route the message.

## Device contracts

### Traffic light head

Accepts `SET_COLOR` with `RED`, `YELLOW`, or `GREEN`. It returns its current
color in a `STATE` message.

### Pedestrian push button

Sends a `PEDESTRIAN_REQUEST` event when pressed. It stores whether a request
is active until the controller clears it. The controller clears a served
request with:

```text
COMMAND|controller|button-east|CLEAR_REQUEST|
```

### Vehicle detector

Sends `VEHICLE_DETECTED` when a vehicle arrives and `VEHICLE_CLEARED` when it
leaves. Each detector includes its road direction and lane number from `1` to
`3`.

```text
EVENT|detector-north-2|controller|VEHICLE_DETECTED|NORTH_LANE_2
```

### Auxiliary device

Reports `NORMAL`, `FAULT`, or `OFFLINE` using the `STATUS` action:

```text
STATE|aux-weather|controller|STATUS|FAULT
```

## State delivery to JavaFX

`DESTINATION` always contains exactly one ID; the Multiplexor does not
broadcast. A device sends its socket `STATE` to the controller. The device
simulator also updates JavaFX through the direct simulator connection shown
in the architecture diagram. If JavaFX later becomes a separate socket
process, the device hub sends a second message addressed to `javafx`.

## Controller timing and safety

- The controller starts with every light red.
- North/south and east/west are the two non-conflicting signal groups.
- Demand is served north/south first and then alternates groups when both wait.
- A requested group receives green only after an all-red interval.
- Green changes to yellow, then all-red, before opposing green.
- Defaults are 5 seconds minimum green, 15 seconds maximum green, 2 seconds
  yellow, and 1 second all-red.
- Pedestrian requests remain active until their direction is served.
- Auxiliary `FAULT` or `OFFLINE` immediately forces all lights red.

When the controller connection closes, the Multiplexor sends:

```text
EVENT|mux|device-hub|COMPONENT_DISCONNECTED|controller
```

The device simulator—not the Multiplexor—owns the fail-safe action and sets
all traffic lights red. Loss of the Multiplexor socket triggers the same
device-side fail-safe. This preserves the rule that the Multiplexor does not
make traffic-control decisions.

After an unexpected Multiplexor disconnect, the controller exits with an
error after local cleanup. Deployment or the test harness may restart it; the
controller does not reconnect silently while devices are in fail-safe mode.

## Routing and error behavior

The Multiplexor validates field count, message type, registration, registered
source, and connected destination. Invalid messages return:

```text
ERROR|mux|<sender-id>|<reason>|<detail>
```

`ACTION` is a stable reason code. `VALUE` is a short detail with `|` replaced
by `/`. Before registration, the destination is `unknown`.

| Reason | Meaning |
| --- | --- |
| `BAD_MESSAGE` | Invalid field count, type, or required field |
| `NOT_REGISTERED` | A non-registration message arrived first |
| `ALREADY_REGISTERED` | A connection attempted to register twice |
| `DUPLICATE_ID` | Another live connection owns the requested ID |
| `SOURCE_MISMATCH` | `SOURCE` differs from the registered ID |
| `UNKNOWN_DESTINATION` | No live connection owns `DESTINATION` |
| `UNSUPPORTED_MESSAGE` | Receiver does not support the action or value |

The Multiplexor routes messages and connection notifications but never stores
traffic-light timing or state.
