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
