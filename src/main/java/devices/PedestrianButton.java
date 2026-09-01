/** Prototype pedestrian-button device. */
public final class PedestrianButton implements Device {
    private final String id;
    private boolean pressed;

    public PedestrianButton(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getState() { return pressed ? "PRESSED" : "IDLE"; }

    public void apply(String action, String value) {
        if ("PRESS".equals(action)) {
            pressed = true;
        } else if ("RESET".equals(action)) {
            pressed = false;
        } else {
            throw new IllegalArgumentException("pedestrian button requires PRESS or RESET");
        }
    }
}
