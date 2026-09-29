package objects;

import devices.LabApiViews;
import devices.TrafficPattern;
import java.util.Objects;

public final class TrafficLights {
    private final LabApiViews.Lights api;
    private TrafficPattern pattern = TrafficPattern.ALL_RED;
    private Phase active;
    private Phase last;

    public TrafficLights(LabApiViews.Lights api) {
        this.api = Objects.requireNonNull(api);
        api.setTrafficPattern(TrafficPattern.ALL_RED);
    }

    public void green(Phase phase) {
        Objects.requireNonNull(phase);
        if (!isAllRed()) throw new IllegalStateException("green requires ALL_RED");
        api.setTrafficPattern(phase.green);
        active = phase;
        pattern = phase.green;
    }

    public void yellow() {
        if (active == null || pattern != active.green) {
            throw new IllegalStateException("yellow requires an active green");
        }
        api.setTrafficPattern(active.yellow);
        pattern = active.yellow;
    }

    public void allRed() {
        if (!isAllRed() && (active == null || pattern != active.yellow)) {
            throw new IllegalStateException("green must pass through yellow first");
        }
        api.setTrafficPattern(TrafficPattern.ALL_RED);
        if (active != null) last = active;
        active = null;
        pattern = TrafficPattern.ALL_RED;
    }

    public boolean isAllRed() { return pattern == TrafficPattern.ALL_RED; }
    public Phase lastPhase() { return last; }
}
