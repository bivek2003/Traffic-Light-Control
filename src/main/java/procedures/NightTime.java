package procedures;

import config.Config;
import objects.CarDetector;
import objects.Phase;
import objects.TrafficLights;

public final class NightTime {
    private int next;

    public void step(CarDetector cars, TrafficLights lights) {
        Phase[] order = {Config.MINOR_ROAD.through(), Config.MINOR_ROAD.left(),
                Config.MAJOR_ROAD.left(), Config.MAJOR_ROAD.through()};
        // Continue around the priority list so constant through demand cannot starve lefts.
        for (int offset = 0; offset < order.length; offset++) {
            int index = (next + offset) % order.length;
            Phase phase = order[index];
            if (phase != Config.MAJOR_ROAD.through() && !cars.waiting(phase)) continue;
            long duration = phase == phase.road.left() ? Config.NIGHT_LEFT_GREEN
                    : phase.road == Config.MAJOR_ROAD ? Config.NIGHT_MAJOR_GREEN : Config.NIGHT_MINOR_GREEN;
            Phases.serve(lights, phase, duration);
            next = (index + 1) % order.length;
            return;
        }
    }
}
