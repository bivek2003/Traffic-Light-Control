package trafficcontrol.controller;

public final class EmergencyModeTest {
    public static void main(String[] args) {
        EmergencyMode mode = new EmergencyMode(new ControllerConfig(1, 1, 4_000, 5_000));
        check(mode.getStage() == EmergencyMode.Stage.IDLE, "should start idle");
        for (Direction direction : Direction.values()) {
            long now = 100_000L * (direction.ordinal() + 1);
            mode.startAfterClearance(direction);
            mode.advance(now, true);
            check(mode.getStage() == EmergencyMode.Stage.GREEN, "active detector must hold green");
            mode.advance(now, false);
            mode.advance(now + 3_999, true);
            check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "must finish yellow");
            check(mode.getDirection() == direction, "must retain served direction during exit");
            try {
                mode.startAfterClearance(direction);
                throw new AssertionError("must reject a restart during clearance");
            } catch (IllegalStateException expected) {
                // A new request must wait for clearance to finish.
            }
            mode.advance(now + 4_000, false);
            check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "yellow should end");
            mode.advance(now + 8_999, false);
            check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "must finish all-red");
            mode.advance(now + 9_000, false);
            check(mode.getStage() == EmergencyMode.Stage.IDLE, "should release after clearance");
            check(mode.getDirection() == null, "completed direction should clear");
        }
        mode.startAfterClearance(Direction.NORTH);
        mode.advance(500_000, false);
        mode.advance(550_000, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "late tick must not skip all-red");
        mode.advance(554_999, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "all-red starts when entered");
        System.out.println("EmergencyModeTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
