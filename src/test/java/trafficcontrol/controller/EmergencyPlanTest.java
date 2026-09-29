package trafficcontrol.controller;

import java.util.EnumSet;

public final class EmergencyPlanTest {
    public static void main(String[] args) {
        testRoadMapping();
        testSameRoadDetection();
        testChangingApproachDoesNotResetHold();
        testHoldLimit();
        testLateGreenStartsNewHold();
        testCustomLimit();
        testLimitValidation();
        System.out.println("EmergencyPlanTest: 7 tests passed");
    }

    private static void testRoadMapping() {
        for (Direction direction : Direction.values()) {
            EmergencyMode mode = new EmergencyMode();
            mode.update(0, EnumSet.of(direction), false, false);
            SignalGroup expected = direction == Direction.NORTH || direction == Direction.SOUTH
                    ? SignalGroup.NORTH_SOUTH : SignalGroup.EAST_WEST;
            check(mode.getRoad() == expected, "wrong emergency road");
        }
        check(new EmergencyMode().getRoad() == null, "idle mode must not select a road");
    }

    private static void testSameRoadDetection() {
        EmergencyMode mode = new EmergencyMode();
        mode.update(0, EnumSet.of(Direction.NORTH), false, false);
        mode.update(4_000, EnumSet.of(Direction.SOUTH), false, false);
        mode.update(6_000, EnumSet.of(Direction.SOUTH), false, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "opposite approach lost its road");
        mode.update(6_500, EnumSet.of(Direction.SOUTH, Direction.EAST), false, false);
        check(mode.getRoad() == SignalGroup.NORTH_SOUTH, "other road interrupted green");
        mode.update(7_000, EnumSet.of(Direction.EAST), false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "other road held NS green");
        mode.update(11_000, EnumSet.of(Direction.EAST), false, false);
        mode.update(13_000, EnumSet.of(Direction.EAST), false, false);
        check(mode.getRoad() == null, "completed road was not released");
        mode.update(13_500, EnumSet.of(Direction.EAST), false, false);
        check(mode.getRoad() == SignalGroup.EAST_WEST, "waiting road was not selected");
    }

    private static void testChangingApproachDoesNotResetHold() {
        EmergencyMode mode = new EmergencyMode();
        mode.update(0, EnumSet.of(Direction.NORTH), false, false);
        mode.update(4_000, EnumSet.of(Direction.NORTH), false, false);
        mode.update(6_000, EnumSet.of(Direction.NORTH), false, false);
        mode.update(65_500, EnumSet.of(Direction.SOUTH), false, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "same-road replacement lost green");
        mode.update(66_000, EnumSet.of(Direction.SOUTH), false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "replacement reset hold limit");
    }

    private static void testHoldLimit() {
        EmergencyMode mode = new EmergencyMode();
        EnumSet<Direction> detected = EnumSet.of(Direction.WEST);
        mode.update(0, detected, false, false);
        mode.update(3_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.ENTRY_YELLOW, "yellow ended early");
        mode.update(4_000, detected, false, false);
        mode.update(5_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.ENTRY_ALL_RED, "all-red ended early");
        mode.update(6_000, detected, false, false);
        mode.update(65_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "hold limit started before green");
        // A stuck detector must still leave green at the configured maximum.
        mode.update(66_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "stuck detector held green");
        mode.update(69_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "exit yellow ended early");
        mode.update(70_000, detected, false, false);
        mode.update(71_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "exit all-red ended early");
        mode.update(72_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.IDLE, "bounded service did not finish");
        check(mode.requiresControl(), "active detector must remain visible for team policy");
        detected.clear();
        mode.update(72_500, detected, false, false);
        check(!mode.requiresControl(), "cleared detector prevented handoff");
        detected.add(Direction.NORTH);
        mode.update(73_000, detected, false, false);
        mode.update(77_000, detected, false, false);
        mode.update(79_000, detected, false, false);
        mode.update(79_500, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "previous hold timer was reused");
    }

    private static void testLateGreenStartsNewHold() {
        EmergencyMode mode = new EmergencyMode();
        EnumSet<Direction> detected = EnumSet.of(Direction.EAST);
        mode.update(0, detected, false, false);
        mode.update(4_000, detected, false, false);
        mode.update(50_000, detected, false, false);
        mode.update(109_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "late green lost its hold time");
        mode.update(110_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "late green missed its limit");
    }

    private static void testCustomLimit() {
        EmergencyMode mode = new EmergencyMode(new ControllerConfig(1, 1, 4_000, 2_000), 1_000);
        mode.start(Direction.SOUTH, 0, false);
        mode.advance(4_000, true, false);
        mode.advance(6_000, true, false);
        mode.advance(6_999, true, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "custom hold ended early");
        mode.advance(7_000, true, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_YELLOW, "custom hold was ignored");
    }

    private static void testLimitValidation() {
        for (long invalid : new long[] {0, -1}) {
            try {
                new EmergencyMode(new ControllerConfig(1, 1, 4_000, 2_000), invalid);
                throw new AssertionError("accepted invalid hold limit");
            } catch (IllegalArgumentException expected) {
                // Invalid timing must fail before the mode runs.
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
