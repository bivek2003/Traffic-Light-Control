package trafficcontrol.controller;

import java.util.EnumSet;

public final class EmergencySequenceTest {
    public static void main(String[] args) {
        testArrivalOrder();
        testSimultaneousRequests();
        testWaitingRequestClears();
        testFaultAndPedestrianHandoff();
        testEntryCancellationWithWaiting();
        testFaultWithoutRequests();
        System.out.println("EmergencySequenceTest: 6 tests passed");
    }

    private static EmergencyMode createMode() {
        return new EmergencyMode(new ControllerConfig(1, 1, 4_000, 5_000));
    }

    private static void testArrivalOrder() {
        EmergencyMode mode = createMode();
        EnumSet<Direction> detected = EnumSet.noneOf(Direction.class);
        mode.update(0, detected, false, false);
        check(!mode.requiresControl(), "empty idle mode must release control");
        detected.add(Direction.WEST);
        mode.update(1, detected, false, false);
        detected.add(Direction.NORTH);
        mode.update(4_001, detected, false, false);
        mode.update(9_001, detected, false, false);
        check(mode.getDirection() == Direction.WEST, "first arrival lost priority");
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "first request was not served");
        detected.remove(Direction.WEST);
        mode.update(10_000, detected, false, false);
        check(mode.getDirection() == Direction.WEST, "exit switched to waiting direction");
        mode.update(14_000, detected, false, false);
        mode.update(19_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.IDLE, "exit did not finish");
        check(mode.requiresControl(), "waiting emergency must prevent normal mode handoff");
        mode.update(19_001, detected, false, false);
        check(mode.getDirection() == Direction.NORTH, "next request was not selected");
        check(mode.getStage() == EmergencyMode.Stage.ENTRY_YELLOW, "next request skipped entry");
    }

    private static void testSimultaneousRequests() {
        EmergencyMode mode = createMode();
        EnumSet<Direction> detected = EnumSet.allOf(Direction.class);
        long now = 0;
        Direction[] approaches = {Direction.NORTH, Direction.EAST};
        for (Direction direction : approaches) {
            mode.update(now, detected, false, false);
            check(mode.getRoad() == direction.group(), "wrong road selected");
            mode.update(now + 4_000, detected, false, false);
            mode.update(now + 9_000, detected, false, false);
            check(mode.getStage() == EmergencyMode.Stage.GREEN, "entry did not reach green");
            detected.removeIf(approach -> approach.group() == direction.group());
            mode.update(now + 10_000, detected, false, false);
            mode.update(now + 14_000, detected, false, false);
            mode.update(now + 19_000, detected, false, false);
            check(mode.requiresControl() == !detected.isEmpty(), "wrong handoff status");
            now += 20_000;
        }
    }

    private static void testWaitingRequestClears() {
        EmergencyMode mode = createMode();
        EnumSet<Direction> detected = EnumSet.of(Direction.SOUTH);
        mode.update(0, detected, false, false);
        detected.add(Direction.EAST);
        mode.update(4_000, detected, false, false);
        detected.remove(Direction.EAST);
        mode.update(9_000, detected, false, false);
        check(mode.getDirection() == Direction.SOUTH, "waiting clear interrupted service");
        detected.clear();
        mode.update(10_000, detected, false, false);
        mode.update(14_000, detected, false, false);
        mode.update(19_000, detected, false, false);
        check(!mode.requiresControl(), "cleared waiting request prevented handoff");
        mode.update(20_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.IDLE, "cleared request was served");
    }

    private static void testFaultAndPedestrianHandoff() {
        EmergencyMode mode = createMode();
        EnumSet<Direction> detected = EnumSet.of(Direction.NORTH);
        mode.update(0, detected, true, false);
        check(mode.getStage() == EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN, "crossing was cut short");
        mode.update(10_000, detected, true, true);
        check(mode.getStage() == EmergencyMode.Stage.FAULT, "fault must override crossing wait");
        check(mode.requiresControl(), "fault must block other traffic modes");
        detected.clear();
        detected.add(Direction.WEST);
        mode.update(20_000, detected, false, true);
        mode.update(30_000, detected, false, false);
        mode.update(34_999, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.EXIT_ALL_RED, "recovery ended early");
        mode.update(35_000, detected, false, false);
        check(mode.requiresControl(), "request received during fault was lost");
        mode.update(35_001, detected, true, false);
        check(mode.getDirection() == Direction.WEST, "recovery restored stale direction");
        check(mode.getStage() == EmergencyMode.Stage.WAIT_FOR_PEDESTRIAN, "new crossing ignored");
        mode.update(40_000, detected, false, false);
        mode.update(45_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.GREEN, "recovery request was not served");
        detected.clear();
        mode.update(46_000, detected, false, false);
        mode.update(50_000, detected, false, false);
        mode.update(55_000, detected, false, false);
        check(!mode.requiresControl(), "finished recovery request did not release control");
    }

    private static void testEntryCancellationWithWaiting() {
        EmergencyMode mode = createMode();
        EnumSet<Direction> detected = EnumSet.of(Direction.NORTH);
        mode.update(0, detected, false, false);
        detected.add(Direction.EAST);
        mode.update(4_000, detected, false, false);
        detected.remove(Direction.NORTH);
        mode.update(9_000, detected, false, false);
        check(mode.getStage() == EmergencyMode.Stage.IDLE, "cancelled entry received green");
        check(mode.requiresControl(), "cancellation lost the waiting request");
        mode.update(9_001, detected, false, false);
        check(mode.getDirection() == Direction.EAST, "waiting direction was not selected");
        check(mode.getStage() == EmergencyMode.Stage.ENTRY_YELLOW, "replacement skipped entry");
        detected.clear();
        mode.update(13_001, detected, false, false);
        mode.update(18_001, detected, false, false);
        check(!mode.requiresControl(), "cancelled replacement did not release control");
    }

    private static void testFaultWithoutRequests() {
        EmergencyMode mode = createMode();
        EnumSet<Direction> detected = EnumSet.noneOf(Direction.class);
        mode.update(0, detected, false, true);
        check(mode.requiresControl(), "fault needs control even without emergencies");
        mode.update(10_000, detected, false, false);
        mode.update(14_999, detected, false, false);
        check(mode.requiresControl(), "empty recovery ended early");
        mode.update(15_000, detected, false, false);
        check(!mode.requiresControl(), "empty recovery did not release control");
        check(mode.getDirection() == null, "empty recovery invented a direction");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
