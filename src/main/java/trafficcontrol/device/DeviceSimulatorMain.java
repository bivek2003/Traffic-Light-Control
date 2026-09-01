package trafficcontrol.device;

import java.io.IOException;
import trafficcontrol.controller.ControllerConfig;
import trafficcontrol.protocol.ProtocolException;

public final class DeviceSimulatorMain {
    private DeviceSimulatorMain() {
    }

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : ControllerConfig.DEFAULT_HOST;
        final int port;
        try {
            port = args.length >= 2
                    ? Integer.parseInt(args[1]) : ControllerConfig.DEFAULT_PORT;
            if (port <= 0 || port > 65_535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException error) {
            System.err.println("Port must be a number from 1 through 65535");
            return;
        }

        try (DeviceSimulator simulator = new DeviceSimulator()) {
            Runtime.getRuntime().addShutdownHook(new Thread(simulator::close));
            simulator.addStateListener((id, type, state) ->
                    System.out.println("[devices] " + id + " = " + state));
            simulator.start(host, port);
            simulator.awaitStop();
        } catch (IOException | ProtocolException error) {
            System.err.println("[devices] " + error.getMessage());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
