package trafficcontrol.controller;

import java.util.EnumSet;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class PedestrianMode {
    private final long crossingMillis;
    private final LongSupplier clock;
    private final EnumSet<Direction> pending = EnumSet.noneOf(Direction.class);
    private final EnumSet<Direction> crossing = EnumSet.noneOf(Direction.class);
    private long startedAt;

    public PedestrianMode(long crossingMillis) {
        this(crossingMillis, () -> System.nanoTime() / 1_000_000);
    }

    public PedestrianMode(long crossingMillis, LongSupplier clock) {
        if (crossingMillis <= 0) {
            throw new IllegalArgumentException("crossing duration must be positive");
        }
        this.crossingMillis = crossingMillis;
        this.clock = Objects.requireNonNull(clock);
    }

    public void start() {
        start(clock.getAsLong());
    }

    public void update() {
        advance(clock.getAsLong());
    }

    public void stop() {
        update();
        if (isCrossingActive()) {
            throw new IllegalStateException("pedestrian crossing must finish before stopping");
        }
    }

    public void request(Direction direction) {
        pending.add(Objects.requireNonNull(direction));
    }

    public boolean hasPendingRequest() {
        return !pending.isEmpty();
    }

    public void start(long now) {
        if (isCrossingActive() || pending.isEmpty()) {
            throw new IllegalStateException("crossing is active or no request is pending");
        }
        crossing.addAll(pending);
        pending.clear();
        startedAt = now;
    }

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
