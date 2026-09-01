package trafficcontrol.controller;

/*
@author Bivek Panthi
*/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import trafficcontrol.protocol.Message;

public final class TrafficControllerSocketTest {
    public static void main(String[] args) throws Exception {
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();

        try (ServerSocket server = new ServerSocket(0)) {
            Thread fakeMultiplexor = new Thread(
                    () -> serveController(server, serverFailure), "fake-multiplexor");
            fakeMultiplexor.start();

            ControllerConfig timings = new ControllerConfig(100, 500, 50, 1);
            boolean connectionLossReported = false;
            try (TrafficController controller = new TrafficController(
                    "localhost", server.getLocalPort(), timings)) {
                try {
                    controller.run();
                } catch (IOException expected) {
                    connectionLossReported = expected.getMessage().contains("connection closed");
                }
            }

            fakeMultiplexor.join(5_000);
            check(!fakeMultiplexor.isAlive(), "fake Multiplexor did not finish");
            if (serverFailure.get() != null) {
                throw new AssertionError("fake Multiplexor failed", serverFailure.get());
            }
            check(connectionLossReported, "controller did not report connection loss");
        }

        System.out.println("TrafficControllerSocketTest: registration, routing, and disconnect passed");
    }

    private static void serveController(ServerSocket server, AtomicReference<Throwable> failure) {
        try (Socket client = server.accept()) {
            client.setSoTimeout(5_000);
            BufferedReader input = new BufferedReader(new InputStreamReader(
                    client.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter output = new PrintWriter(new OutputStreamWriter(
                    client.getOutputStream(), StandardCharsets.UTF_8), true);

            check("REGISTER|controller|mux|CONNECT|CONTROLLER".equals(input.readLine()),
                    "controller sent an invalid registration");
            output.println("STATE|mux|controller|REGISTERED|OK");
            assertFourColors(input, "RED", null);

            output.println("EVENT|detector-north-1|controller|VEHICLE_DETECTED|NORTH_LANE_1");
            assertFourColors(input, "GREEN", SignalGroup.NORTH_SOUTH);
            // Closing this socket verifies that TrafficController.run reports connection loss.
        } catch (Throwable error) {
            failure.set(error);
        }
    }

    private static void assertFourColors(BufferedReader input, String activeColor,
            SignalGroup activeGroup) throws Exception {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < Direction.values().length; index++) {
            Message message = Message.parse(input.readLine());
            check("SET_COLOR".equals(message.getAction()), "expected SET_COLOR command");
            Direction direction = directionForLight(message.getDestination());
            String expected = activeGroup != null && activeGroup.contains(direction)
                    ? activeColor : "RED";
            check(expected.equals(message.getValue()),
                    direction + " expected " + expected + " but received " + message.getValue());
            seen.add(message.getDestination());
        }
        check(seen.size() == Direction.values().length, "controller did not command every light");
    }

    private static Direction directionForLight(String lightId) {
        for (Direction direction : Direction.values()) {
            if (direction.lightId().equals(lightId)) {
                return direction;
            }
        }
        throw new AssertionError("unknown light ID: " + lightId);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
