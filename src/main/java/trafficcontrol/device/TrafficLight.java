package trafficcontrol.device;

public final class TrafficLight implements Device {
    private final String id;
    private String color = "RED";

    public TrafficLight(String id) {
        if (id == null || !id.matches("light-(north|south|east|west)")) {
            throw new IllegalArgumentException("invalid traffic light ID");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "TRAFFIC_LIGHT";
    }

    @Override
    public String getState() {
        return color;
    }

    @Override
    public void handleCommand(String action, String value) {
        if (!"SET_COLOR".equals(action)) {
            throw new IllegalArgumentException("unsupported traffic light command");
        }
        if (!"RED".equals(value) && !"YELLOW".equals(value) && !"GREEN".equals(value)) {
            throw new IllegalArgumentException("invalid traffic light color");
        }
        color = value;
    }
}
