package trafficcontrol.controller;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.Queue;

// Tracks emergency detectors only. The controller still decides when lights change.
public final class EmergencyRequests {
    private final Queue<Direction> waiting = new ArrayDeque<>();

    // Pass all currently active directions, including ones already waiting.
    public void update(EnumSet<Direction> detected) {
        waiting.removeIf(direction -> !detected.contains(direction));
        // Direction.values() gives N, S, E, W order for new detections together.
        for (Direction direction : Direction.values()) {
            if (detected.contains(direction) && !waiting.contains(direction)) {
                waiting.add(direction);
            }
        }
    }

    // Null means no emergency is waiting. This does not grant a green light.
    public Direction nextDirection() {
        return waiting.peek();
    }
}
