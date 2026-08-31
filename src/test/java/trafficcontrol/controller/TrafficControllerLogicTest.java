package trafficcontrol.controller;

import java.util.List;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;
import trafficcontrol.protocol.ProtocolException;

/** Dependency-free unit tests. Run with the commands documented in README.md. */
public final class TrafficControllerLogicTest {
    private static int testsRun;

    public static void main(String[] args) throws Exception {
        testProtocolValidation();
        testRegistrationProtocol();
        testInitialStateIsAllRed();
        testVehicleRequestGetsGreen();
        testConflictingDirectionsUseSafeTransitions();
        testPedestrianRequestIsServedAndCleared();
        testVehicleClearRemovesDemand();
        testDeviceFaultForcesAllRed();
        testInvalidAndMisaddressedEventsAreRejected();
        System.out.println("TrafficControllerLogicTest: " + testsRun + " tests passed");
    }

    private static ControllerConfig testConfig() {
        return new ControllerConfig(100, 500, 50, 20);
    }

    private static void testProtocolValidation() throws Exception {
        Message parsed = Message.parse("EVENT|detector-north-1|controller|VEHICLE_DETECTED|NORTH_LANE_1");
        check(parsed.getType() == MessageType.EVENT, "valid message type should parse");
        expectProtocolError("BOGUS|source|controller|ACTION|value");
        expectProtocolError("EVENT|source|controller|ACTION");
        expectProtocolError("EVENT||controller|ACTION|value");
        testsRun++;
    }

    private static void testRegistrationProtocol() throws Exception {
        check("REGISTER|controller|mux|CONNECT|CONTROLLER".equals(
                TrafficController.registrationMessage().toLine()), "registration format changed");
        Message acknowledgement = Message.parse("STATE|mux|controller|REGISTERED|OK");
        check(TrafficController.isRegistrationAcknowledgement(acknowledgement),
                "valid registration acknowledgement was rejected");
        check(!TrafficController.isRegistrationAcknowledgement(
                Message.parse("STATE|mux|controller|REGISTERED|FAILED")),
                "failed registration was accepted");
        testsRun++;
    }

    private static void testInitialStateIsAllRed() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        assertAllLights(logic.initialCommands(), SignalColor.RED);
        check(logic.getStage() == TrafficControllerLogic.Stage.ALL_RED,
                "controller must start all-red");
        testsRun++;
    }

    private static void testVehicleRequestGetsGreen() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        List<Message> commands = logic.accept(event("detector-north-1", "VEHICLE_DETECTED",
                "NORTH_LANE_1"), 20);
        assertGroupColors(commands, SignalGroup.NORTH_SOUTH, SignalColor.GREEN);
        check(logic.getActiveGroup() == SignalGroup.NORTH_SOUTH, "north/south should be active");
        testsRun++;
    }

    private static void testConflictingDirectionsUseSafeTransitions() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        logic.accept(event("detector-north-1", "VEHICLE_DETECTED", "NORTH_LANE_1"), 20);
        check(logic.accept(event("detector-east-1", "VEHICLE_DETECTED", "EAST_LANE_1"), 30)
                .isEmpty(), "green must last for the minimum duration");
        assertGroupColors(logic.advance(120), SignalGroup.NORTH_SOUTH, SignalColor.YELLOW);
        check(logic.getStage() == TrafficControllerLogic.Stage.YELLOW, "yellow stage missing");
        assertAllLights(logic.advance(170), SignalColor.RED);
        check(logic.getStage() == TrafficControllerLogic.Stage.ALL_RED, "all-red stage missing");
        assertGroupColors(logic.advance(190), SignalGroup.EAST_WEST, SignalColor.GREEN);
        testsRun++;
    }

    private static void testPedestrianRequestIsServedAndCleared() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        List<Message> messages = logic.accept(
                event("button-west", "PEDESTRIAN_REQUEST", "WEST"), 20);
        assertGroupColors(messages, SignalGroup.EAST_WEST, SignalColor.GREEN);
        check(hasMessage(messages, "button-west", "CLEAR_REQUEST", ""),
                "served pedestrian request was not cleared");
        testsRun++;
    }

    private static void testVehicleClearRemovesDemand() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        logic.accept(event("detector-south-2", "VEHICLE_DETECTED", "SOUTH_LANE_2"), 1);
        logic.accept(event("detector-south-2", "VEHICLE_CLEARED", "SOUTH_LANE_2"), 2);
        check(logic.advance(20).isEmpty(), "cleared vehicle should not receive a green phase");
        testsRun++;
    }

    private static void testDeviceFaultForcesAllRed() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        logic.accept(event("detector-north-1", "VEHICLE_DETECTED", "NORTH_LANE_1"), 20);
        Message fault = new Message(MessageType.STATE, "aux-weather", "controller", "STATUS", "FAULT");
        assertAllLights(logic.accept(fault, 30), SignalColor.RED);
        check(logic.hasFaults(), "fault should be latched");
        check(logic.advance(1_000).isEmpty(), "faulted controller must remain all-red");
        Message normal = new Message(MessageType.STATE, "aux-weather", "controller", "STATUS", "NORMAL");
        logic.accept(normal, 1_001);
        check(!logic.hasFaults(), "NORMAL should clear the device fault");
        testsRun++;
    }

    private static void testInvalidAndMisaddressedEventsAreRejected() {
        TrafficControllerLogic logic = new TrafficControllerLogic(testConfig(), 0);
        expectIllegalArgument(() -> logic.accept(
                event("detector-north-1", "UNKNOWN_EVENT", "NORTH"), 20));
        Message wrongDestination = new Message(MessageType.EVENT, "detector-north-1", "javafx",
                "VEHICLE_DETECTED", "NORTH_LANE_1");
        expectIllegalArgument(() -> logic.accept(wrongDestination, 20));
        expectIllegalArgument(() -> logic.accept(
                event("unknown-north", "VEHICLE_DETECTED", "NORTH_LANE_1"), 20));
        expectIllegalArgument(() -> logic.accept(
                event("detector-north-1", "VEHICLE_DETECTED", "EAST_LANE_1"), 20));
        testsRun++;
    }

    private static Message event(String source, String action, String value) {
        return new Message(MessageType.EVENT, source, "controller", action, value);
    }

    private static void assertAllLights(List<Message> messages, SignalColor color) {
        for (Direction direction : Direction.values()) {
            check(hasMessage(messages, direction.lightId(), "SET_COLOR", color.name()),
                    direction + " was not set to " + color);
        }
    }

    private static void assertGroupColors(List<Message> messages, SignalGroup active,
            SignalColor activeColor) {
        for (Direction direction : Direction.values()) {
            SignalColor expected = active.contains(direction) ? activeColor : SignalColor.RED;
            check(hasMessage(messages, direction.lightId(), "SET_COLOR", expected.name()),
                    direction + " expected " + expected);
        }
    }

    private static boolean hasMessage(List<Message> messages, String destination,
            String action, String value) {
        for (Message message : messages) {
            if (destination.equals(message.getDestination())
                    && action.equals(message.getAction()) && value.equals(message.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static void expectProtocolError(String line) {
        try {
            Message.parse(line);
            throw new AssertionError("expected protocol error for: " + line);
        } catch (ProtocolException expected) {
            // Expected.
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
