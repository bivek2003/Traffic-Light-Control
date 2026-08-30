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
