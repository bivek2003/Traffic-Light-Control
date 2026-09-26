package trafficcontrol.controller;

import java.util.Objects;

// Tracks emergency service and exit timing; it does not send light commands.
public final class EmergencyMode {
    public enum Stage { IDLE, GREEN, EXIT_YELLOW, EXIT_ALL_RED }

    private final ControllerConfig config;
    private Stage stage = Stage.IDLE;
    private Direction direction;
    private long stageStartedAt;

    public EmergencyMode(ControllerConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    // Call only after entry clearance and any active pedestrian crossing finish.
    public void startAfterClearance(Direction requested) {
        if (stage != Stage.IDLE) {
            throw new IllegalStateException("emergency mode is still busy");
        }
        direction = Objects.requireNonNull(requested);
        stage = Stage.GREEN;
    }

    // detected is the current reading for the served direction, not any direction.
    public void advance(long now, boolean detected) {
        if (stage == Stage.GREEN && !detected) {
            stage = Stage.EXIT_YELLOW;
            stageStartedAt = now;
        } else if (stage == Stage.EXIT_YELLOW
                && now - stageStartedAt >= config.getYellowMillis()) {
            stage = Stage.EXIT_ALL_RED;
            stageStartedAt = now;
        } else if (stage == Stage.EXIT_ALL_RED
                && now - stageStartedAt >= config.getAllRedMillis()) {
            stage = Stage.IDLE;
            direction = null;
        }
    }

    public Stage getStage() {
        return stage;
    }

    public Direction getDirection() {
        return direction;
    }
}
