package procedures;

import config.Config;
import objects.Phase;
import objects.TrafficLights;
import util.Runtime;

public final class Phases {
    private Phases() { }

    public static void serve(TrafficLights lights, Phase phase, long greenTime) {
        lights.green(phase);
        try {
            Runtime.delay(greenTime);
        } finally {
            finish(lights);
        }
    }

    public static void finish(TrafficLights lights) {
        lights.yellow();
        Runtime.delay(Config.YELLOW);
        lights.allRed();
        Runtime.delay(Config.ALL_RED);
    }
}
