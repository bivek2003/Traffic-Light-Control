package trafficcontrol.controller;

import java.util.EnumSet;

public final class EmergencyRequestsTest {
    public static void main(String[] args) {
        EmergencyRequests requests = new EmergencyRequests();
        check(requests.nextDirection() == null, "should start empty");
        requests.update(EnumSet.of(Direction.WEST));
        requests.update(EnumSet.of(Direction.NORTH, Direction.WEST));
        check(requests.nextDirection() == Direction.WEST, "first arrival should stay first");
        requests.update(EnumSet.of(Direction.NORTH, Direction.WEST));
        requests.update(EnumSet.of(Direction.NORTH));
        check(requests.nextDirection() == Direction.NORTH, "cleared direction should leave");
        requests.update(EnumSet.noneOf(Direction.class));
        check(requests.nextDirection() == null, "repeat detections should not leave duplicates");

        // Directions arriving in one update use the agreed tie order.
        EnumSet<Direction> detected = EnumSet.allOf(Direction.class);
        requests.update(detected);
        Direction[] order = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction direction : order) {
            check(requests.nextDirection() == direction, "wrong simultaneous arrival order");
            detected.remove(direction);
            requests.update(detected);
        }
        check(requests.nextDirection() == null, "all detectors have cleared");

        requests.update(EnumSet.of(Direction.SOUTH));
        requests.update(EnumSet.of(Direction.SOUTH, Direction.EAST));
        requests.update(EnumSet.of(Direction.SOUTH));
        check(requests.nextDirection() == Direction.SOUTH, "waiting clear should not interrupt first");
        requests.update(EnumSet.noneOf(Direction.class));
        check(requests.nextDirection() == null, "cleared waiting request should be skipped");
        requests.update(EnumSet.of(Direction.EAST));
        check(requests.nextDirection() == Direction.EAST, "a cleared detector can activate again");
        System.out.println("EmergencyRequestsTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
