package trafficcontrol.controller;

import java.util.Objects;
import java.util.EnumSet;

// Tracks emergency entry, service, and exit; it does not send light commands.
public final class EmergencyMode {
    public enum Stage {
        IDLE, WAIT_FOR_PEDESTRIAN, ENTRY_YELLOW, ENTRY_ALL_RED,
        GREEN, EXIT_YELLOW, EXIT_ALL_RED, FAULT
    }

    private final ControllerConfig config;
    private final EmergencyRequests requests = new EmergencyRequests();
    private Stage stage = Stage.IDLE;
    private Direction direction;
    private long stageStartedAt;

    public EmergencyMode(ControllerConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    // Use this entry point for integration, with a full snapshot on every update.
    public void update(long now, EnumSet<Direction> detected,
            boolean pedestrianCrossing, boolean anyFaultActive) {
        Objects.requireNonNull(detected);
        requests.update(detected);
        setFault(anyFaultActive, now);
        if (stage == Stage.IDLE) {
            Direction next = requests.nextDirection();
            if (next != null) {
                start(next, now, pedestrianCrossing);
            }
        } else {
            advance(now, detected.contains(direction), pedestrianCrossing);
        }
        // A completed exit stays IDLE until the next update selects a request.
    }

    // Keep other modes stopped while a request waits or clearance is unfinished.
    public boolean requiresControl() {
        return stage != Stage.IDLE || requests.nextDirection() != null;
    }

    // An active pedestrian crossing already has all traffic stopped at red.
    public void start(Direction requested, long now, boolean pedestrianCrossing) {
        if (stage != Stage.IDLE) {
            throw new IllegalStateException("emergency mode is still busy");
        }
        direction = Objects.requireNonNull(requested);
        stage = pedestrianCrossing ? Stage.WAIT_FOR_PEDESTRIAN : Stage.ENTRY_YELLOW;
        stageStartedAt = now;
    }

    // detected is the current reading for the served direction, not any direction.
    public void advance(long now, boolean detected, boolean pedestrianCrossing) {
        if (stage == Stage.WAIT_FOR_PEDESTRIAN && !pedestrianCrossing) {
            stage = Stage.ENTRY_ALL_RED;
            stageStartedAt = now;
        } else if (stage == Stage.ENTRY_YELLOW
                && now - stageStartedAt >= config.getYellowMillis()) {
            stage = pedestrianCrossing ? Stage.WAIT_FOR_PEDESTRIAN : Stage.ENTRY_ALL_RED;
            stageStartedAt = now;
        } else if (stage == Stage.ENTRY_ALL_RED) {
            if (pedestrianCrossing) {
                stage = Stage.WAIT_FOR_PEDESTRIAN;
            } else if (now - stageStartedAt >= config.getAllRedMillis()) {
                // A detector that cleared during entry must never receive green.
                stage = detected ? Stage.GREEN : Stage.IDLE;
                if (!detected) {
                    direction = null;
                }
            }
        } else if (stage == Stage.GREEN && !detected) {
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

    // The controller reports whether ANY fault is active, before advancing time.
    public void setFault(boolean active, long now) {
        if (active) {
            stage = Stage.FAULT;
            direction = null;
        } else if (stage == Stage.FAULT) {
            // Recovery must finish all-red before the controller selects again.
            stage = Stage.EXIT_ALL_RED;
            stageStartedAt = now;
        }
    }

    public Stage getStage() {
        return stage;
    }

    public Direction getDirection() {
        return direction;
    }
}
