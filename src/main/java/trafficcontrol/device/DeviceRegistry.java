package trafficcontrol.device;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DeviceRegistry {
    private final Map<String, Device> devices = new LinkedHashMap<>();

    public void add(Device device) {
        if (device == null) {
            throw new IllegalArgumentException("device must not be null");
        }
        if (devices.putIfAbsent(device.getId(), device) != null) {
            throw new IllegalArgumentException("duplicate device ID: " + device.getId());
        }
    }

    public Device get(String id) {
        Device device = devices.get(id);
        if (device == null) {
            throw new IllegalArgumentException("unknown device ID: " + id);
        }
        return device;
    }

    public <T extends Device> T get(String id, Class<T> type) {
        Device device = get(id);
        if (!type.isInstance(device)) {
            throw new IllegalArgumentException("wrong device type for ID: " + id);
        }
        return type.cast(device);
    }

    public Collection<Device> all() {
        return Collections.unmodifiableCollection(devices.values());
    }

    public List<TrafficLight> trafficLights() {
        List<TrafficLight> lights = new ArrayList<>();
        for (Device device : devices.values()) {
            if (device instanceof TrafficLight) {
                lights.add((TrafficLight) device);
            }
        }
        return lights;
    }

    public static DeviceRegistry createStandard() {
        DeviceRegistry registry = new DeviceRegistry();
        String[] directions = {"north", "south", "east", "west"};
        for (String direction : directions) {
            registry.add(new TrafficLight("light-" + direction));
            registry.add(new PedestrianButton("button-" + direction));
            for (int lane = 1; lane <= 3; lane++) {
                registry.add(new VehicleDetector("detector-" + direction + "-" + lane));
            }
        }
        registry.add(new AuxiliaryDevice("aux-system"));
        return registry;
    }
}
