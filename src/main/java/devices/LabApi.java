package devices;

// These six operations match section 2 of the team plan.
public interface LabApi {
    boolean pedRequest();
    void pedClearRequest();
    void setPedLight(PedLightStatus status);
    boolean emergency(Direction direction);
    boolean carDetection(Direction direction, Lane lane);
    void setTrafficPattern(TrafficPattern pattern);
}
