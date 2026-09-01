/** Prototype vehicle-detector device. */
public final class VehicleDetector implements Device {
    private final String id;
    private boolean detected;

    public VehicleDetector(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getState() { return detected ? "DETECTED" : "CLEAR"; }

    public void apply(String action, String value) {
        if ("DETECT".equals(action)) {
            detected = true;
        } else if ("CLEAR".equals(action)) {
            detected = false;
        } else {
            throw new IllegalArgumentException("vehicle detector requires DETECT or CLEAR");
        }
    }
}
