package objects;

import devices.LabApiViews;
import devices.PedLightStatus;
import java.util.Objects;

public final class PedInterface {
    private final LabApiViews.Ped api;

    public PedInterface(LabApiViews.Ped api) {
        this.api = Objects.requireNonNull(api);
        api.setPedLight(PedLightStatus.STOP);
    }

    public boolean pedRequest() { return api.pedRequest(); }
    public void pedClearRequest() { api.pedClearRequest(); }
    public void setPedLight(PedLightStatus status) { api.setPedLight(status); }
}
