package trafficcontrol.controller;

import java.util.EnumSet;
import java.util.Objects;

// Independent prototype; device commands and mode selection belong to the caller.
public final class PedestrianMode {
    private final long crossingMillis;
    private final EnumSet<Direction> pending = EnumSet.noneOf(Direction.class);
    private final EnumSet<Direction> crossing = EnumSet.noneOf(Direction.class);
    private long startedAt;

    public PedestrianMode(long crossingMillis) {
        if (crossingMillis <= 0) {
            throw new IllegalArgumentException("crossing duration must be positive");
        }
        this.crossingMillis = crossingMillis;
    }

    public void request(Direction direction) {
        pending.add(Objects.requireNonNull(direction));
    }

    public boolean hasPendingRequest() {
        return !pending.isEmpty();
    }

    // Start only after vehicle clearance and when Emergency does not require control.
    // The caller holds all vehicle lights red throughout the crossing.
    public void start(long now) {
        if (isCrossingActive() || pending.isEmpty()) {
            throw new IllegalStateException("crossing is active or no request is pending");
        }
        crossing.addAll(pending);
        pending.clear();
        startedAt = now;
    }

    // Use increasing millisecond times. Apply STOP on completion before selecting a mode.
    public void advance(long now) {
        if (isCrossingActive() && now - startedAt >= crossingMillis) {
            crossing.clear();
        }
    }

    public boolean isCrossingActive() {
        return !crossing.isEmpty();
    }

    public EnumSet<Direction> getCrossingDirections() {
        return crossing.clone();
    }
}
