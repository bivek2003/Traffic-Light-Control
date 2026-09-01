package trafficcontrol.device;

import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;
import trafficcontrol.protocol.ProtocolException;

public final class DeviceMessageHandler {
    private final DeviceSimulatorLogic logic;

    public DeviceMessageHandler(DeviceSimulatorLogic logic) {
        this.logic = logic;
    }

    public Message handle(String connectionId, String line) {
        final Message message;
        try {
            message = Message.parse(line);
        } catch (ProtocolException error) {
            return Message.error(connectionId, "controller", "BAD_MESSAGE", error.getMessage());
        }

        if (message.getType() == MessageType.ERROR) {
            if ("UNKNOWN_DESTINATION".equals(message.getAction())
                    && "controller".equals(message.getValue())) {
                logic.failSafeAllRed();
            }
            System.err.println("[devices] " + message.toLine());
            return null;
        }

        try {
            if (message.getType() == MessageType.COMMAND) {
                return logic.acceptCommand(connectionId, message);
            }
            if (isControllerDisconnect(connectionId, message)) {
                logic.failSafeAllRed();
                return null;
            }
            throw new IllegalArgumentException("unsupported device message");
        } catch (IllegalArgumentException error) {
            return Message.error(connectionId, message.getSource(),
                    "UNSUPPORTED_MESSAGE", error.getMessage());
        }
    }

    private boolean isControllerDisconnect(String connectionId, Message message) {
        return "device-hub".equals(connectionId)
                && message.getType() == MessageType.EVENT
                && "mux".equals(message.getSource())
                && "device-hub".equals(message.getDestination())
                && "COMPONENT_DISCONNECTED".equals(message.getAction())
                && "controller".equals(message.getValue());
    }
}
