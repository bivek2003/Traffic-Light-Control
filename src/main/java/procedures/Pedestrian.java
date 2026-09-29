package procedures;

import config.Config;
import devices.PedLightStatus;
import objects.PedInterface;
import util.Runtime;

public final class Pedestrian {
    public void step(PedInterface ped) {
        ped.setPedLight(PedLightStatus.WALK);
        try {
            Runtime.delay(Config.WALK);
            ped.pedClearRequest();
        } finally {
            ped.setPedLight(PedLightStatus.STOP);
            Runtime.delay(Config.PED_CLEARANCE);
        }
    }
}
