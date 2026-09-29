package devices;

// Objects receive only the API operations allowed by the access diagram.
public final class LabApiViews {
    private LabApiViews() { }

    public interface Ped {
        boolean pedRequest();
        void pedClearRequest();
        void setPedLight(PedLightStatus status);
    }

    public interface Emergency {
        boolean emergency(Direction direction);
    }

    public interface Cars {
        boolean carDetection(Direction direction, Lane lane);
    }

    public interface Lights {
        void setTrafficPattern(TrafficPattern pattern);
    }

    public static Ped ped(LabApi api) {
        return new Ped() {
            public boolean pedRequest() { return api.pedRequest(); }
            public void pedClearRequest() { api.pedClearRequest(); }
            public void setPedLight(PedLightStatus status) { api.setPedLight(status); }
        };
    }

    public static Emergency emergency(LabApi api) {
        return api::emergency;
    }

    public static Cars cars(LabApi api) {
        return api::carDetection;
    }

    public static Lights lights(LabApi api) {
        return api::setTrafficPattern;
    }
}
