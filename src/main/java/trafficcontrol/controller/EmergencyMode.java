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
    private final long maximumHoldMillis;
    private final EmergencyRequests requests = new EmergencyRequests();
    private Stage stage = Stage.IDLE;
    private Direction direction;
    private long stageStartedAt;

    // Team plan defaults; move these to the shared Config during integration.
    public EmergencyMode() {
        this(new ControllerConfig(30_000, 30_000, 4_000, 2_000));
    }

    public EmergencyMode(ControllerConfig config) {
        this(config, 60_000);
    }

    public EmergencyMode(ControllerConfig config, long maximumHoldMillis) {
        this.config = Objects.requireNonNull(config);
        if (maximumHoldMillis <= 0) {
            throw new IllegalArgumentException("emergency maximum hold must be positive");
        }
        this.maximumHoldMillis = maximumHoldMillis;
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
            advance(now, roadDetected(detected), pedestrianCrossing);
        }
        // A completed exit stays IDLE until the next update selects a request.
    }

    // Keep other modes stopped while a request waits or clearance is unfinished.
    public boolean requiresControl() {
        return stage != Stage.IDLE || requests.nextDirection() != null;
    }

    // Either approach on the selected road keeps its through phase requested.
    private boolean roadDetected(EnumSet<Direction> detected) {
        SignalGroup road = getRoad();
        for (Direction approaching : detected) {
            if (approaching.group() == road) {
                return true;
            }
        }
        return false;
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

    // detected means an emergency is present on either approach of the served road.
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
                stageStartedAt = now;
                if (!detected) {
                    direction = null;
                }
            }
        } else if (stage == Stage.GREEN
                && (!detected || now - stageStartedAt >= maximumHoldMillis)) {
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

    // The lab supports NS/EW patterns, not one emergency lane at a time.
    public SignalGroup getRoad() {
        return direction == null ? null : direction.group();
    }
}
