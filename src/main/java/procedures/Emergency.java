package procedures;

import config.Config;
import devices.Direction;
import objects.EmergencyDetector;
import objects.Road;
import objects.TrafficLights;
import util.Runtime;

public final class Emergency {
    public void step(EmergencyDetector em, TrafficLights lights) {
        Direction direction = em.approach();
        if (direction == null) return;
        Road road = Road.from(direction);
        lights.green(road.through());
        long started = Runtime.millis();
        try {
            while (em.emergency(road) && !Thread.currentThread().isInterrupted()) {
                long remaining = Config.EMERGENCY_MAX_HOLD - (Runtime.millis() - started);
                if (remaining <= 0) break;
                Runtime.delay(Math.min(Config.EMERGENCY_POLL, remaining));
            }
        } finally {
            Phases.finish(lights);
        }
    }
}
