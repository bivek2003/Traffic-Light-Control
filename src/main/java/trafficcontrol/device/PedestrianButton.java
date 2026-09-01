package trafficcontrol.device;

public final class PedestrianButton implements Device {
    private final String id;
    private boolean requestActive;

    public PedestrianButton(String id) {
        if (id == null || !id.matches("button-(north|south|east|west)")) {
            throw new IllegalArgumentException("invalid pedestrian button ID");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "PEDESTRIAN_BUTTON";
    }

    @Override
    public String getState() {
        return requestActive ? "REQUESTED" : "IDLE";
    }

    public void press() {
        requestActive = true;
    }

    @Override
    public void handleCommand(String action, String value) {
        if (!"CLEAR_REQUEST".equals(action) || value == null || !value.isEmpty()) {
            throw new IllegalArgumentException("unsupported pedestrian button command");
        }
        requestActive = false;
    }
}
