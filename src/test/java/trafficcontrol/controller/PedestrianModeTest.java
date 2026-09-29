package trafficcontrol.controller;

import java.util.EnumSet;

public final class PedestrianModeTest {
    public static void main(String[] args) {
        testInvalidCalls();
        testCrossingTimingAndRequests();
        System.out.println("PedestrianModeTest: 2 scenarios passed");
    }

    private static void testInvalidCalls() {
        expect(IllegalArgumentException.class, () -> new PedestrianMode(0));
        expect(IllegalArgumentException.class, () -> new PedestrianMode(-1));
        PedestrianMode mode = new PedestrianMode(100);
        check(!mode.isCrossingActive() && !mode.hasPendingRequest(), "must start idle");
        expect(IllegalStateException.class, () -> mode.start(0));
        expect(NullPointerException.class, () -> mode.request(null));
        mode.advance(10);
        check(!mode.isCrossingActive(), "idle advance must not start a crossing");
        mode.request(Direction.NORTH);
        mode.start(20);
        mode.request(Direction.WEST);
        expect(IllegalStateException.class, () -> mode.start(30));
        check(mode.getCrossingDirections().equals(EnumSet.of(Direction.NORTH)),
                "rejected start must not change the active directions");
        mode.advance(120);
        check(!mode.isCrossingActive(), "rejected start must not reset the timer");
        check(mode.hasPendingRequest(), "rejected start must not consume queued requests");
    }

    private static void testCrossingTimingAndRequests() {
        PedestrianMode mode = new PedestrianMode(100);
        mode.request(Direction.NORTH);
        mode.request(Direction.NORTH);
        mode.request(Direction.EAST);
        check(!mode.isCrossingActive(), "pending requests are not active crossings");
        mode.start(1_000);
        check(!mode.hasPendingRequest(), "started requests must leave the queue");
        check(mode.getCrossingDirections().equals(EnumSet.of(Direction.NORTH, Direction.EAST)),
                "duplicate requests must share one crossing");
        mode.getCrossingDirections().clear();
        check(mode.isCrossingActive(), "returned directions must be a defensive copy");
        mode.request(Direction.NORTH);
        mode.request(Direction.WEST);
        mode.advance(1_099);
        check(mode.isCrossingActive(), "crossing must last its full duration");
        mode.advance(1_100);
        check(!mode.isCrossingActive(), "crossing must end at the duration boundary");
        check(mode.hasPendingRequest(), "requests during crossing must be retained");
        mode.advance(5_000);
        check(!mode.isCrossingActive(), "queued requests must wait for mode selection");
        mode.start(5_000);
        check(mode.getCrossingDirections().equals(EnumSet.of(Direction.NORTH, Direction.WEST)),
                "next crossing must serve only requests made during the previous crossing");
        mode.advance(9_000);
        check(!mode.isCrossingActive() && !mode.hasPendingRequest(), "late tick must finish");
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw error;
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
