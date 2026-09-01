package trafficcontrol.device;

import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;

public final class DeviceCommandRouter {
    private final DeviceRegistry registry;
    private final DeviceStateListener stateListener;

    public DeviceCommandRouter(DeviceRegistry registry, DeviceStateListener stateListener) {
        this.registry = registry;
        this.stateListener = stateListener;
    }

    public Message handle(String connectionId, Message command) {
        if (command.getType() != MessageType.COMMAND) {
            throw new IllegalArgumentException("device accepts only COMMAND messages");
        }
        if (!connectionId.equals(command.getDestination())) {
            throw new IllegalArgumentException("command sent to the wrong device connection");
        }

        Device device = registry.get(command.getDestination());
        device.handleCommand(command.getAction(), command.getValue());
        stateListener.stateChanged(device.getId(), device.getType(), device.getState());
        return stateMessage(device, command.getSource());
    }

    private Message stateMessage(Device device, String destination) {
        String action = "STATE";
        if (device instanceof TrafficLight) {
            action = "COLOR";
        } else if (device instanceof PedestrianButton) {
            action = "REQUEST";
        } else if (device instanceof AuxiliaryDevice) {
            action = "STATUS";
        }
        return new Message(MessageType.STATE, device.getId(), destination,
                action, device.getState());
    }
}
