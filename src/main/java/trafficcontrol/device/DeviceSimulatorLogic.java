package trafficcontrol.device;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;

public final class DeviceSimulatorLogic {
    private final DeviceRegistry registry;
    private final List<DeviceStateListener> stateListeners = new CopyOnWriteArrayList<>();
    private final DeviceCommandRouter commandRouter;

    public DeviceSimulatorLogic() {
        this(DeviceRegistry.createStandard());
    }

    public DeviceSimulatorLogic(DeviceRegistry registry) {
        this.registry = registry;
        this.commandRouter = new DeviceCommandRouter(registry, this::publishState);
    }

    public void addStateListener(DeviceStateListener listener) {
        if (listener != null) {
            stateListeners.add(listener);
        }
    }

    public Message acceptCommand(String connectionId, Message command) {
        return commandRouter.handle(connectionId, command);
    }

    public Message pressPedestrian(String buttonId) {
        PedestrianButton button = registry.get(buttonId, PedestrianButton.class);
        button.press();
        publishState(button.getId(), button.getType(), button.getState());
        return new Message(MessageType.EVENT, buttonId, "controller",
                "PEDESTRIAN_REQUEST", directionFrom(buttonId));
    }

    public Message vehicleDetected(String detectorId) {
        VehicleDetector detector = registry.get(detectorId, VehicleDetector.class);
        detector.detectVehicle();
        publishState(detector.getId(), detector.getType(), detector.getState());
        return vehicleEvent(detectorId, "VEHICLE_DETECTED");
    }

    public Message vehicleCleared(String detectorId) {
        VehicleDetector detector = registry.get(detectorId, VehicleDetector.class);
        detector.clearVehicle();
        publishState(detector.getId(), detector.getType(), detector.getState());
        return vehicleEvent(detectorId, "VEHICLE_CLEARED");
    }

    public Message setAuxiliaryStatus(String deviceId, String status) {
        AuxiliaryDevice device = registry.get(deviceId, AuxiliaryDevice.class);
        device.setStatus(status);
        publishState(device.getId(), device.getType(), device.getState());
        return new Message(MessageType.STATE, deviceId, "controller", "STATUS", status);
    }

    public void failSafeAllRed() {
        for (TrafficLight light : registry.trafficLights()) {
            light.handleCommand("SET_COLOR", "RED");
            publishState(light.getId(), light.getType(), light.getState());
        }
    }

    public Device getDevice(String id) {
        return registry.get(id);
    }

    private Message vehicleEvent(String detectorId, String action) {
        String[] parts = detectorId.split("-");
        String value = parts[1].toUpperCase(Locale.ROOT) + "_LANE_" + parts[2];
        return new Message(MessageType.EVENT, detectorId, "controller", action, value);
    }

    private String directionFrom(String deviceId) {
        String[] parts = deviceId.split("-");
        return parts[1].toUpperCase(Locale.ROOT);
    }

    private void publishState(String id, String type, String state) {
        for (DeviceStateListener listener : stateListeners) {
            listener.stateChanged(id, type, state);
        }
    }
}
