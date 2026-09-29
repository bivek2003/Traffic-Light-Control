package trafficcontrol.controller;

import java.util.EnumSet;

public final class PedestrianEmergencyTest {
    public static void main(String[] args) {
        PedestrianMode pedestrian = new PedestrianMode(1_000);
        EmergencyMode emergency = new EmergencyMode(new ControllerConfig(1, 1, 100, 200));

        pedestrian.request(Direction.NORTH);
        pedestrian.request(Direction.EAST);
        pedestrian.start(10);
        check(pedestrian.isCrossingActive(), "crossing must be active");
        check(pedestrian.getCrossingDirections().equals(EnumSet.of(Direction.NORTH, Direction.EAST)),
                "requests should be served together");
        check(!pedestrian.hasPendingRequest(), "started requests must leave the queue");

        emergency.update(10, EnumSet.of(Direction.SOUTH), pedestrian.isCrossingActive(), false);
        check(emergency.getStage() == EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN,
                "emergency must wait for the crossing");
        pedestrian.request(Direction.WEST);
        pedestrian.advance(1_009);
        check(pedestrian.isCrossingActive(), "crossing ended early");
        emergency.update(1_009, EnumSet.of(Direction.SOUTH), pedestrian.isCrossingActive(), false);
        check(emergency.getStage() == EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN,
                "emergency must not interrupt the active crossing");
        pedestrian.advance(1_010);
        check(!pedestrian.isCrossingActive(), "crossing did not finish");
        check(pedestrian.hasPendingRequest(), "new request must remain queued");

        emergency.update(1_010, EnumSet.of(Direction.SOUTH), pedestrian.isCrossingActive(), false);
        check(emergency.getStage() == EmergencyMode.Stage.ENTRY_ALL_RED,
                "emergency should begin clearance after the crossing");
        emergency.update(1_209, EnumSet.of(Direction.SOUTH), false, false);
        check(emergency.getStage() == EmergencyMode.Stage.ENTRY_ALL_RED,
                "emergency must finish all-red before green");
        emergency.update(1_210, EnumSet.of(Direction.SOUTH), false, false);
        check(emergency.getStage() == EmergencyMode.Stage.GREEN,
                "emergency should serve the detected road");
        emergency.update(1_211, EnumSet.noneOf(Direction.class), false, false);
        check(emergency.requiresControl(), "pedestrians must wait through emergency exit");
        emergency.update(1_311, EnumSet.noneOf(Direction.class), false, false);
        emergency.update(1_511, EnumSet.noneOf(Direction.class), false, false);
        check(!emergency.requiresControl(), "emergency must release control after clearance");
        pedestrian.start(2_000);
        check(pedestrian.getCrossingDirections().equals(EnumSet.of(Direction.WEST)),
                "next crossing should serve only the queued request");
        System.out.println("PedestrianEmergencyTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
