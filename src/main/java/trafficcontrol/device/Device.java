package trafficcontrol.device;

/** Shared contract implemented by every simulated field device. */
public interface Device {
    String getId();

    String getType();

    String getState();

    void handleCommand(String action, String value);
}
