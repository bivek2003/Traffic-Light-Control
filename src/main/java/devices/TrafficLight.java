/** Prototype traffic-light device. */
public final class TrafficLight implements Device {
    private final String id;
    private String color = "RED";

    public TrafficLight(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getState() { return color; }

    public void apply(String action, String value) {
        if (!"SET_COLOR".equals(action)) {
            throw new IllegalArgumentException("traffic light requires SET_COLOR");
        }
        if (!"RED".equals(value) && !"YELLOW".equals(value) && !"GREEN".equals(value)) {
            throw new IllegalArgumentException("invalid light color " + value);
        }
        color = value;
    }
}
