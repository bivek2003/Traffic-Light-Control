package trafficcontrol.controller;

public final class EmergencyFaultTest {
    public static void main(String[] args) {
        for (EmergencyMode.Stage stage : EmergencyMode.Stage.values()) {
            EmergencyMode mode = atStage(stage);
            check(mode.getStage() == stage, "wrong starting stage");
            mode.setFault(true, 20_000);
            mode.advance(100_000, true, false);
            check(mode.getStage() == EmergencyMode.Stage.FAULT, "fault must stop progression");
            check(mode.getDirection() == null, "fault must discard the served direction");
            try {
                mode.start(Direction.WEST, 100_000, false);
                throw new AssertionError("fault allowed a new emergency");
            } catch (IllegalStateException expected) {
                // The controller must wait for recovery clearance.
            }
            mode.setFault(true, 100_000);
            mode.setFault(false, 110_000);
            mode.advance(114_999, true, false);
            check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "recovery ended early");
            mode.setFault(false, 114_999);
            mode.advance(115_000, true, false);
            check(mode.getStage() == EmergencyMode.Stage.IDLE, "recovery should release control");
            // A still-active detector cannot automatically restore the old green.
            mode.advance(116_000, true, false);
            check(mode.getStage() == EmergencyMode.Stage.IDLE, "must select a fresh request");
            mode.start(Direction.EAST, 120_000, false);
            check(mode.getStage() == EmergencyMode.Stage.ENTRY_YELLOW, "must redo entry");
        }
        EmergencyMode mode = atStage(EmergencyMode.Stage.GREEN);
        mode.setFault(true, 20_000);
        mode.setFault(false, 30_000);
        mode.setFault(true, 34_999);
        mode.advance(35_000, false, false);
        check(mode.getStage() == EmergencyMode.Stage.FAULT, "new fault must interrupt recovery");
        mode.setFault(false, 40_000);
        mode.advance(44_999, false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "clearance must restart");
        mode.advance(45_000, false, false);
        check(mode.getStage() == EmergencyMode.Stage.IDLE, "second recovery did not finish");
        System.out.println("EmergencyFaultTest: all stages and repeated fault recovery passed");
    }

    private static EmergencyMode atStage(EmergencyMode.Stage stage) {
        EmergencyMode mode = new EmergencyMode(new ControllerConfig(1, 1, 4_000, 5_000));
        if (stage == EmergencyMode.Stage.IDLE) return mode;
        if (stage == EmergencyMode.Stage.FAULT) {
            mode.setFault(true, 0);
            return mode;
        }
        mode.start(Direction.NORTH, 0, stage == EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN);
        if (mode.getStage() == stage) return mode;
        mode.advance(4_000, true, false);
        if (mode.getStage() == stage) return mode;
        mode.advance(9_000, true, false);
        if (mode.getStage() == stage) return mode;
        mode.advance(10_000, false, false);
        if (mode.getStage() == stage) return mode;
        mode.advance(14_000, false, false);
        return mode;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
