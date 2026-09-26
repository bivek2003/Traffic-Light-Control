package trafficcontrol.controller;

public final class EmergencyEntryTest {
    public static void main(String[] args) {
        testEntryTiming();
        testPedestrianWait();
        testCrossingReportedDuringYellow();
        testCancelledRequest();
        testLateUpdate();
        testRestartDuringEntry();
        System.out.println("EmergencyEntryTest: 6 tests passed");
    }

    private static EmergencyMode createMode() {
        return new EmergencyMode(new ControllerConfig(1, 1, 4_000, 5_000));
    }

    private static void testEntryTiming() {
        for (Direction direction : Direction.values()) {
            EmergencyMode mode = createMode();
            mode.start(direction, 0, false);
            check(mode, EmergencyMode.Stage.ENTRY_YELLOW);
            mode.advance(3_999, true, false);
            check(mode, EmergencyMode.Stage.ENTRY_YELLOW);
            mode.advance(4_000, true, false);
            check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
            mode.advance(8_999, true, false);
            check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
            mode.advance(9_000, true, false);
            check(mode, EmergencyMode.Stage.GREEN);
            if (mode.getDirection() != direction) {
                throw new AssertionError("entry changed the selected direction");
            }
        }
    }

    private static void testPedestrianWait() {
        EmergencyMode mode = createMode();
        mode.start(Direction.NORTH, 0, true);
        mode.advance(60_000, true, true);
        check(mode, EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN);
        mode.advance(60_001, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(65_000, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        // If a crossing is still active, begin a fresh clearance after it ends.
        mode.advance(65_001, true, true);
        check(mode, EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN);
        mode.advance(70_000, true, false);
        mode.advance(74_999, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(75_000, true, false);
        check(mode, EmergencyMode.Stage.GREEN);
    }

    private static void testCrossingReportedDuringYellow() {
        EmergencyMode mode = createMode();
        mode.start(Direction.EAST, 0, false);
        mode.advance(4_000, true, true);
        check(mode, EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN);
        mode.advance(9_000, true, true);
        check(mode, EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN);
        mode.advance(10_000, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(14_999, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(15_000, true, false);
        check(mode, EmergencyMode.Stage.GREEN);
    }

    private static void testCancelledRequest() {
        EmergencyMode mode = createMode();
        mode.start(Direction.EAST, 0, false);
        mode.advance(4_000, false, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(9_000, false, false);
        check(mode, EmergencyMode.Stage.IDLE);
        if (mode.getDirection() != null) {
            throw new AssertionError("cancelled direction was kept");
        }
        mode.start(Direction.WEST, 10_000, true);
        mode.advance(20_000, false, true);
        check(mode, EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN);
        mode.advance(21_000, false, false);
        mode.advance(26_000, false, false);
        check(mode, EmergencyMode.Stage.IDLE);
    }

    private static void testLateUpdate() {
        EmergencyMode mode = createMode();
        mode.start(Direction.SOUTH, 0, false);
        mode.advance(50_000, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(54_999, true, false);
        check(mode, EmergencyMode.Stage.ENTRY_ALL_RED);
        mode.advance(55_000, true, false);
        check(mode, EmergencyMode.Stage.GREEN);
    }

    private static void testRestartDuringEntry() {
        EmergencyMode mode = createMode();
        mode.start(Direction.WEST, 0, false);
        try {
            mode.start(Direction.NORTH, 1, false);
            throw new AssertionError("a second request interrupted entry");
        } catch (IllegalStateException expected) {
            check(mode, EmergencyMode.Stage.ENTRY_YELLOW);
        }
        if (mode.getDirection() != Direction.WEST) {
            throw new AssertionError("restart replaced the first direction");
        }
    }

    private static void check(EmergencyMode mode, EmergencyMode.Stage expected) {
        if (mode.getStage() != expected) {
            throw new AssertionError("expected " + expected + " but got " + mode.getStage());
        }
    }
}
