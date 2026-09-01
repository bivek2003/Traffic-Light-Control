package trafficcontrol.device;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class DeviceSystemTest {
    private static final long TIMEOUT_MILLIS = 8_000;

    public static void main(String[] args) throws Exception {
        int port = availablePort();
        Process multiplexor = startProcess("multiplexor.Multiplexor", String.valueOf(port));
        Process controller = null;

        try (DeviceSimulator simulator = new DeviceSimulator()) {
            waitForPort(port);
            simulator.start("localhost", port);
            controller = startProcess("trafficcontrol.controller.TrafficController",
                    "localhost", String.valueOf(port));

            waitFor(() -> allLightsAre(simulator, "RED"),
                    "controller did not initialize all lights red");
            Thread.sleep(1_100);
            simulator.vehicleDetected("detector-north-1");
            waitFor(() -> "GREEN".equals(
                    simulator.getDevice("light-north").getState()),
                    "vehicle event did not produce a north green light");

            controller.destroyForcibly();
            controller.waitFor();
            waitFor(() -> allLightsAre(simulator, "RED"),
                    "controller loss did not trigger all-red fail-safe");
        } finally {
            if (controller != null && controller.isAlive()) {
                controller.destroyForcibly();
            }
            multiplexor.destroyForcibly();
            multiplexor.waitFor();
        }

        System.out.println("DeviceSystemTest: registration, routing, events, and fail-safe passed");
    }

    private static Process startProcess(String mainClass, String... arguments) throws IOException {
        String java = System.getProperty("java.home") + "/bin/java";
        String classPath = System.getProperty("java.class.path");
        List<String> command = new ArrayList<>();
        command.add(java);
        command.add("-cp");
        command.add(classPath);
        command.add(mainClass);
        for (String argument : arguments) {
            command.add(argument);
        }
        return new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start();
    }

    private static int availablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void waitForPort(int port) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            try {
                Socket socket = new Socket("localhost", port);
                socket.close();
                return;
            } catch (IOException error) {
                Thread.sleep(50);
            }
        }
        throw new AssertionError("Multiplexor did not start");
    }

    private static boolean allLightsAre(DeviceSimulator simulator, String color) {
        String[] directions = {"north", "south", "east", "west"};
        for (String direction : directions) {
            if (!color.equals(simulator.getDevice("light-" + direction).getState())) {
                return false;
            }
        }
        return true;
    }

    private static void waitFor(BooleanSupplier condition, String failure) throws Exception {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError(failure);
    }
}
