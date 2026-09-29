package procedures;

import config.Config;
import objects.CarDetector;
import objects.Phase;
import objects.TrafficLights;

public final class NightTime {
    public void step(CarDetector cars, TrafficLights lights) {
        Phase phase = Config.MAJOR_ROAD.through();
        long duration = Config.NIGHT_MAJOR_GREEN;
        if (cars.waiting(Config.MINOR_ROAD.through())) {
            phase = Config.MINOR_ROAD.through();
            duration = Config.NIGHT_MINOR_GREEN;
        } else if (cars.waiting(Config.MINOR_ROAD.left())) {
            phase = Config.MINOR_ROAD.left();
            duration = Config.NIGHT_LEFT_GREEN;
        } else if (cars.waiting(Config.MAJOR_ROAD.left())) {
            phase = Config.MAJOR_ROAD.left();
            duration = Config.NIGHT_LEFT_GREEN;
        }
        Phases.serve(lights, phase, duration);
    }
}
