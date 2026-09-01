package trafficcontrol.device;

@FunctionalInterface
public interface DeviceStateListener {
    void stateChanged(String deviceId, String deviceType, String state);
}
