package procedures;

import config.Config;
import java.time.LocalTime;
import objects.EmergencyDetector;
import objects.PedInterface;

public final class ModeSelection {
    private Mode previous = Mode.NORMAL;

    public Mode select(PedInterface ped, EmergencyDetector em) {
        Mode selected;
        if (em.approach() != null) selected = Mode.EMERGENCY;
        else if (previous != Mode.PEDESTRIAN && ped.pedRequest()) selected = Mode.PEDESTRIAN;
        else {
            LocalTime time = LocalTime.now();
            boolean night = !time.isBefore(Config.NIGHT_START) || time.isBefore(Config.NIGHT_END);
            selected = night ? Mode.NIGHT : Mode.NORMAL;
        }
        previous = selected;
        return selected;
    }
}
