package util;

public final class Runtime {
    private Runtime() { }

    public static long millis() {
        return System.nanoTime() / 1_000_000;
    }

    // Finish the requested delay, then restore interruption for the controller.
    // This allows yellow/all-red clearance to finish before shutdown.
    public static void delay(long milliseconds) {
        if (milliseconds < 0) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        long started = millis();
        long remaining = milliseconds;
        boolean interrupted = false;
        while (remaining > 0) {
            try {
                Thread.sleep(remaining);
            } catch (InterruptedException error) {
                interrupted = true;
            }
            remaining = milliseconds - (millis() - started);
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
