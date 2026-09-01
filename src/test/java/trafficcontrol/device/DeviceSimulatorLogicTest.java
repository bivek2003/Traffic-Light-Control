package trafficcontrol.device;

import java.util.ArrayList;
import java.util.List;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;

public final class DeviceSimulatorLogicTest {
    private static int testsRun;

    public static void main(String[] args) {
        testStandardDevices();
        testLightCommandAndStateResponse();
        testPedestrianAndVehicleEvents();
        testAuxiliaryStatus();
        testProtocolErrors();
        testDisconnectFailSafe();
        System.out.println("DeviceSimulatorLogicTest: " + testsRun + " tests passed");
    }

    private static void testStandardDevices() {
        DeviceSimulatorLogic logic = new DeviceSimulatorLogic();
        check(logic.getDevices().size() == 21, "standard device count changed");
        check(logic.getDevice("light-west") instanceof TrafficLight,
                "west traffic light is missing");
        check(logic.getDevice("detector-east-3") instanceof VehicleDetector,
                "east lane 3 detector is missing");
        testsRun++;
    }

    private static void testLightCommandAndStateResponse() {
        DeviceSimulatorLogic logic = new DeviceSimulatorLogic();
        List<String> changes = new ArrayList<>();
        logic.addStateListener((id, type, state) -> changes.add(id + "=" + state));

        Message command = new Message(MessageType.COMMAND, "controller",
                "light-north", "SET_COLOR", "GREEN");
        Message state = logic.acceptCommand("light-north", command);

        check("STATE|light-north|controller|COLOR|GREEN".equals(state.toLine()),
                "traffic light state response is wrong");
        check(changes.contains("light-north=GREEN"), "JavaFX listener missed light state");
        testsRun++;
    }

    private static void testPedestrianAndVehicleEvents() {
        DeviceSimulatorLogic logic = new DeviceSimulatorLogic();

        Message pedestrian = logic.pressPedestrian("button-east");
        check("EVENT|button-east|controller|PEDESTRIAN_REQUEST|EAST"
                .equals(pedestrian.toLine()), "pedestrian event is wrong");

        Message detected = logic.vehicleDetected("detector-south-2");
        check("EVENT|detector-south-2|controller|VEHICLE_DETECTED|SOUTH_LANE_2"
                .equals(detected.toLine()), "vehicle detected event is wrong");

        Message cleared = logic.vehicleCleared("detector-south-2");
        check("EVENT|detector-south-2|controller|VEHICLE_CLEARED|SOUTH_LANE_2"
                .equals(cleared.toLine()), "vehicle cleared event is wrong");
        testsRun++;
    }

    private static void testAuxiliaryStatus() {
        DeviceSimulatorLogic logic = new DeviceSimulatorLogic();
        Message status = logic.setAuxiliaryStatus("aux-system", "FAULT");
        check("STATE|aux-system|controller|STATUS|FAULT".equals(status.toLine()),
                "auxiliary status message is wrong");
        check("FAULT".equals(logic.getDevice("aux-system").getState()),
                "auxiliary status did not change");
        testsRun++;
    }

    private static void testProtocolErrors() {
        DeviceSimulatorLogic logic = new DeviceSimulatorLogic();
        DeviceMessageHandler handler = new DeviceMessageHandler(logic);

        Message bad = handler.handle("light-north", "not-a-message");
        check(bad.getType() == MessageType.ERROR, "bad message did not return ERROR");
        check("BAD_MESSAGE".equals(bad.getAction()), "wrong bad message reason");

        Message unsupported = handler.handle("button-east",
                "EVENT|controller|button-east|UNKNOWN|");
        check("UNSUPPORTED_MESSAGE".equals(unsupported.getAction()),
                "unsupported message did not return ERROR");
        testsRun++;
    }

    private static void testDisconnectFailSafe() {
        DeviceSimulatorLogic logic = new DeviceSimulatorLogic();
        DeviceMessageHandler handler = new DeviceMessageHandler(logic);
        logic.acceptCommand("light-east", new Message(MessageType.COMMAND,
                "controller", "light-east", "SET_COLOR", "GREEN"));

        handler.handle("device-hub",
                "EVENT|mux|device-hub|COMPONENT_DISCONNECTED|controller");
        assertAllRed(logic);

        logic.acceptCommand("light-west", new Message(MessageType.COMMAND,
                "controller", "light-west", "SET_COLOR", "GREEN"));
        handler.handle("light-west",
                "ERROR|mux|light-west|UNKNOWN_DESTINATION|controller");
        assertAllRed(logic);
        testsRun++;
    }

    private static void assertAllRed(DeviceSimulatorLogic logic) {
        String[] directions = {"north", "south", "east", "west"};
        for (String direction : directions) {
            check("RED".equals(logic.getDevice("light-" + direction).getState()),
                    direction + " light is not fail-safe red");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
