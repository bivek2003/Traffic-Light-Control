package trafficcontrol.device;

public final class VehicleDetector implements Device {
    private final String id;
    private boolean vehiclePresent;

    public VehicleDetector(String id) {
        if (id == null || !id.matches("detector-(north|south|east|west)-[1-3]")) {
            throw new IllegalArgumentException("invalid vehicle detector ID");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "VEHICLE_DETECTOR";
    }

    @Override
    public String getState() {
        return vehiclePresent ? "DETECTED" : "CLEAR";
    }

    public void detectVehicle() {
        vehiclePresent = true;
    }

    public void clearVehicle() {
        vehiclePresent = false;
    }

    @Override
    public void handleCommand(String action, String value) {
        throw new IllegalArgumentException("vehicle detectors do not accept commands");
    }
}
