package trafficcontrol.controller;

public enum SignalGroup {
    NORTH_SOUTH,
    EAST_WEST;

    public SignalGroup opposite() {
        return this == NORTH_SOUTH ? EAST_WEST : NORTH_SOUTH;
    }

    public boolean contains(Direction direction) {
        return direction.group() == this;
    }
}
