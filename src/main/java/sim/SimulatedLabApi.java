package sim;

import devices.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import objects.Phase;

// The GUI changes inputs; only controller procedures change outputs.
public final class SimulatedLabApi implements LabApi {
    private final boolean[] emergencies = new boolean[4];
    private final boolean[][] manualCars = new boolean[4][3];
    private final boolean[][] movingCars = new boolean[4][3];
    private final List<String> events = new ArrayList<>();
    private boolean requested;
    private boolean open = true;
    private TrafficPattern pattern = TrafficPattern.ALL_RED;
    private PedLightStatus pedestrian = PedLightStatus.STOP;
    private String mode = "Starting";

    public synchronized boolean pedRequest() { return requested; }
    public synchronized void pedClearRequest() { requested = false; record("Request cleared"); }

    public synchronized void setPedLight(PedLightStatus status) {
        Objects.requireNonNull(status);
        if (status == PedLightStatus.WALK && pattern != TrafficPattern.ALL_RED) {
            throw new IllegalStateException("WALK requires ALL_RED");
        }
        pedestrian = status;
        record("Pedestrian " + status);
    }

    public synchronized boolean emergency(Direction direction) {
        return emergencies[direction.ordinal()];
    }

    public synchronized boolean carDetection(Direction direction, Lane lane) {
        return manualCars[direction.ordinal()][lane.ordinal()]
                || movingCars[direction.ordinal()][lane.ordinal()];
    }

    public synchronized void setTrafficPattern(TrafficPattern next) {
        Objects.requireNonNull(next);
        if (pedestrian == PedLightStatus.WALK && next != TrafficPattern.ALL_RED) {
            throw new IllegalStateException("traffic must remain red during WALK");
        }
        boolean legal = next == pattern;
        for (Phase phase : Phase.values()) {
            legal |= pattern == TrafficPattern.ALL_RED && next == phase.green;
            legal |= pattern == phase.green && next == phase.yellow;
            legal |= pattern == phase.yellow && next == TrafficPattern.ALL_RED;
        }
        if (!legal) throw new IllegalStateException("illegal pattern: " + pattern + " -> " + next);
        pattern = next;
        record("Traffic " + next);
    }

    public synchronized void pressPedestrian() { requested = true; record("Pedestrian request"); }
    public synchronized void setEmergency(Direction direction, boolean active) {
        emergencies[direction.ordinal()] = active;
        record("Emergency " + direction + " " + active);
    }

    public synchronized void setCar(Direction direction, Lane lane, boolean active) {
        manualCars[direction.ordinal()][lane.ordinal()] = active;
    }

    public synchronized void setMovingCars(boolean[][] present) {
        for (int d = 0; d < 4; d++) System.arraycopy(present[d], 0, movingCars[d], 0, 3);
    }

    public synchronized boolean isOpen() { return open; }
    public synchronized void close() { open = false; mode = "Stopping after this phase"; }
    public synchronized void setMode(String value) { mode = value; record("Mode " + value); }
    public synchronized State state() {
        return new State(pattern, pedestrian, requested, mode, List.copyOf(events));
    }

    private void record(String value) {
        events.add(java.time.LocalTime.now().withNano(0) + "  " + value);
        if (events.size() > 80) events.remove(0);
    }

    public record State(TrafficPattern pattern, PedLightStatus pedestrian,
                        boolean requested, String mode, List<String> events) { }
}
