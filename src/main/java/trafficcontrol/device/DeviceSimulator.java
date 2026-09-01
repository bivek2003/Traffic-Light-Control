package trafficcontrol.device;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;
import trafficcontrol.protocol.ProtocolException;

public final class DeviceSimulator implements AutoCloseable, DeviceConnection.Listener {
    public static final String HUB_ID = "device-hub";

    private final DeviceSimulatorLogic logic;
    private final DeviceMessageHandler messageHandler;
    private final Map<String, DeviceConnection> connections = new LinkedHashMap<>();
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final ScheduledExecutorService statusTimer =
            Executors.newSingleThreadScheduledExecutor();

    private volatile boolean started;
    private volatile boolean closing;

    public DeviceSimulator() {
        this(new DeviceSimulatorLogic());
    }

    public DeviceSimulator(DeviceSimulatorLogic logic) {
        this.logic = logic;
        this.messageHandler = new DeviceMessageHandler(logic);
    }

    public synchronized void start(String host, int port) throws IOException, ProtocolException {
        if (started) {
            throw new IllegalStateException("device simulator is already running");
        }
        started = true;
        try {
            startConnection(HUB_ID, host, port);
            for (Device device : logic.getDevices()) {
                startConnection(device.getId(), host, port);
            }
        } catch (IOException | ProtocolException error) {
            close();
            throw error;
        }
        statusTimer.scheduleAtFixedRate(this::sendStatusSafely,
                1, 1, TimeUnit.SECONDS);
        System.out.println("[devices] registered hub and " + logic.getDevices().size()
                + " devices");
    }

    private void startConnection(String id, String host, int port)
            throws IOException, ProtocolException {
        DeviceConnection connection = new DeviceConnection(id, host, port, this);
        connection.start();
        connections.put(id, connection);
    }

    public void addStateListener(DeviceStateListener listener) {
        logic.addStateListener(listener);
    }

    public void pressPedestrian(String buttonId) {
        sendFrom(buttonId, logic.pressPedestrian(buttonId));
    }

    public void vehicleDetected(String detectorId) {
        sendFrom(detectorId, logic.vehicleDetected(detectorId));
    }

    public void vehicleCleared(String detectorId) {
        sendFrom(detectorId, logic.vehicleCleared(detectorId));
    }

    public void setAuxiliaryStatus(String deviceId, String status) {
        sendFrom(deviceId, logic.setAuxiliaryStatus(deviceId, status));
    }

    public Device getDevice(String id) {
        return logic.getDevice(id);
    }

    public void awaitStop() throws InterruptedException {
        stopped.await();
    }

    private void sendFrom(String sourceId, Message message) {
        DeviceConnection connection = connections.get(sourceId);
        if (connection == null) {
            throw new IllegalStateException("device is not connected: " + sourceId);
        }
        try {
            connection.send(message);
        } catch (IOException error) {
            logic.failSafeAllRed();
            close();
            throw new IllegalStateException(error.getMessage(), error);
        }
    }

    private void sendStatusSafely() {
        if (closing) {
            return;
        }
        Device auxiliary = logic.getDevice("aux-system");
        Message status = new Message(MessageType.STATE, auxiliary.getId(),
                "controller", "STATUS", auxiliary.getState());
        try {
            sendFrom(auxiliary.getId(), status);
        } catch (IllegalStateException error) {
            System.err.println("[devices] status update failed: " + error.getMessage());
        }
    }

    @Override
    public void lineReceived(DeviceConnection connection, String line) {
        Message response = messageHandler.handle(connection.getId(), line);
        if (response != null) {
            try {
                connection.send(response);
            } catch (IOException error) {
                connectionLost(connection);
            }
        }
    }

    @Override
    public void connectionLost(DeviceConnection connection) {
        if (!closing) {
            System.err.println("[devices] entering all-red fail-safe");
            logic.failSafeAllRed();
            close();
        }
    }

    @Override
    public synchronized void close() {
        if (closing) {
            return;
        }
        closing = true;
        statusTimer.shutdownNow();
        for (DeviceConnection connection : connections.values()) {
            connection.close();
        }
        connections.clear();
        stopped.countDown();
    }
}
