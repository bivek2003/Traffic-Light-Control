package objects;

import devices.Direction;
import devices.LabApiViews;
import java.util.Objects;

public final class EmergencyDetector {
    private final LabApiViews.Emergency api;

    public EmergencyDetector(LabApiViews.Emergency api) {
        this.api = Objects.requireNonNull(api);
    }

    public boolean emergency(Direction direction) { return api.emergency(direction); }

    // N, S, E, W is the temporary tie order until the team chooses a policy.
    public Direction approach() {
        for (Direction direction : Direction.values()) {
            if (emergency(direction)) return direction;
        }
        return null;
    }

    public boolean emergency(Road road) {
        return road == Road.NS ? emergency(Direction.N) || emergency(Direction.S)
                : emergency(Direction.E) || emergency(Direction.W);
    }
}
