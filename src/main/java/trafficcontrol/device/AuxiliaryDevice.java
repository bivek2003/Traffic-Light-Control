package trafficcontrol.device;

public final class AuxiliaryDevice implements Device {
    private final String id;
    private String status = "NORMAL";

    public AuxiliaryDevice(String id) {
        if (id == null || !id.matches("aux-[a-z0-9]+(?:-[a-z0-9]+)*")) {
            throw new IllegalArgumentException("invalid auxiliary device ID");
        }
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "AUXILIARY_DEVICE";
    }

    @Override
    public String getState() {
        return status;
    }

    public void setStatus(String status) {
        if (!"NORMAL".equals(status) && !"FAULT".equals(status) && !"OFFLINE".equals(status)) {
            throw new IllegalArgumentException("invalid auxiliary device status");
        }
        this.status = status;
    }

    @Override
    public void handleCommand(String action, String value) {
        throw new IllegalArgumentException("auxiliary devices do not accept commands");
    }
}
