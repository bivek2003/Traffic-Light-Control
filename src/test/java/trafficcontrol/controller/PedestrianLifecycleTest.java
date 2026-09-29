package trafficcontrol.controller;

import java.util.EnumSet;

public final class PedestrianLifecycleTest {
    public static void main(String[] args) {
        long[] now = {10};
        PedestrianMode pedestrian = new PedestrianMode(100, () -> now[0]);
        EmergencyMode emergency = new EmergencyMode(new ControllerConfig(1, 1, 10, 20));
        pedestrian.stop();
        pedestrian.request(Direction.NORTH);
        pedestrian.start();
        emergency.update(now[0], EnumSet.of(Direction.WEST), pedestrian.isCrossingActive(), false);
        check(emergency.getStage() == EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN,
                "active crossing must be reported to Emergency");
        pedestrian.request(Direction.SOUTH);
        now[0] = 109;
        try {
            pedestrian.stop();
            throw new AssertionError("stop must reject an unfinished crossing");
        } catch (IllegalStateException expected) {
            check(pedestrian.isCrossingActive(), "early stop must preserve crossing state");
        }
        now[0] = 110;
        pedestrian.update();
        pedestrian.stop();
        pedestrian.stop();
        check(!pedestrian.isCrossingActive(), "completed crossing must release control");
        check(pedestrian.hasPendingRequest(), "stop must not erase queued requests");
        emergency.update(now[0], EnumSet.of(Direction.WEST), pedestrian.isCrossingActive(), false);
        check(emergency.getStage() == EmergencyMode.Stage.ENTRY_ALL_RED,
                "Emergency must retain its entry clearance");
        now[0] = 130;
        emergency.update(now[0], EnumSet.of(Direction.WEST), false, false);
        check(emergency.getStage() == EmergencyMode.Stage.GREEN, "Emergency should reach green");
        emergency.update(131, EnumSet.noneOf(Direction.class), false, false);
        emergency.update(141, EnumSet.noneOf(Direction.class), false, false);
        emergency.update(161, EnumSet.noneOf(Direction.class), false, false);
        check(!emergency.requiresControl(), "Emergency must finish before another crossing");
        now[0] = 162;
        pedestrian.start();
        now[0] = 262;
        pedestrian.stop();
        check(!pedestrian.isCrossingActive(), "stop at the boundary must finish crossing");
        check(!pedestrian.hasPendingRequest(), "second crossing must consume queued request");
        System.out.println("PedestrianLifecycleTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
