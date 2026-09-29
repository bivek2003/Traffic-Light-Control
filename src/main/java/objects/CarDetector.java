package objects;

import devices.Direction;
import devices.Lane;
import devices.LabApiViews;
import java.util.Objects;

public final class CarDetector {
    private final LabApiViews.Cars api;

    public CarDetector(LabApiViews.Cars api) {
        this.api = Objects.requireNonNull(api);
    }

    public boolean carDetection(Direction direction, Lane lane) {
        return api.carDetection(direction, lane);
    }

    public boolean waiting(Phase phase) {
        boolean left = phase == phase.road.left();
        for (Direction direction : Direction.values()) {
            if (Road.from(direction) != phase.road) continue;
            if (left && carDetection(direction, Lane.L)) return true;
            if (!left && (carDetection(direction, Lane.C) || carDetection(direction, Lane.R))) {
                return true;
            }
        }
        return false;
    }
}
