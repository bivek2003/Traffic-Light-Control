package trafficcontrol.controller;

import java.util.Locale;

public enum Direction {
    NORTH,
    SOUTH,
    EAST,
    WEST;

    public SignalGroup group() {
        return this == NORTH || this == SOUTH
                ? SignalGroup.NORTH_SOUTH
                : SignalGroup.EAST_WEST;
    }

    public String lightId() {
        return "light-" + name().toLowerCase(Locale.ROOT);
    }

    public static Direction from(String source, String value) {
        Direction sourceDirection = findToken(source);
        Direction valueDirection = findToken(value);
        if (sourceDirection != null && valueDirection != null
                && sourceDirection != valueDirection) {
            throw new IllegalArgumentException("source direction does not match message value");
        }
        Direction direction = valueDirection != null ? valueDirection : sourceDirection;
        if (direction == null) {
            throw new IllegalArgumentException("message does not identify a road direction");
        }
        return direction;
    }

    private static Direction findToken(String text) {
        String[] tokens = text.toUpperCase(Locale.ROOT).split("[^A-Z]+");
        for (String token : tokens) {
            for (Direction direction : values()) {
                if (token.equals(direction.name())) {
                    return direction;
                }
            }
        }
        return null;
    }
}
