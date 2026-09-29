package procedures;

import config.Config;
import objects.Phase;
import objects.TrafficLights;

public final class NormalTrafficControl {
    private int next;

    public void step(TrafficLights lights) {
        Phase phase = Phase.values()[next];
        long duration = phase == phase.road.left() ? Config.LEFT_GREEN : Config.THROUGH_GREEN;
        Phases.serve(lights, phase, duration);
        next = (next + 1) % Phase.values().length;
    }
}
