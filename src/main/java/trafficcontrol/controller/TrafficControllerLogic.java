package trafficcontrol.controller;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import trafficcontrol.protocol.Message;
import trafficcontrol.protocol.MessageType;

/**
 * Testable traffic-signal state machine. All transitions pass through yellow
 * and an all-red interval, so conflicting approaches are never green together.
 */
public final class TrafficControllerLogic {
    public enum Stage {
        ALL_RED,
        GREEN,
        YELLOW
    }

    private final ControllerConfig config;
    private final EnumSet<Direction> vehicles = EnumSet.noneOf(Direction.class);
    private final Map<Direction, Set<String>> pedestrianRequests =
            new EnumMap<>(Direction.class);
    private final Set<String> faultedDevices = new HashSet<>();

    private Stage stage = Stage.ALL_RED;
    private SignalGroup activeGroup;
    private SignalGroup preferredNext = SignalGroup.NORTH_SOUTH;
    private long stageStartedAt;

    public TrafficControllerLogic(ControllerConfig config, long startedAt) {
        this.config = config;
        this.stageStartedAt = startedAt;
        for (Direction direction : Direction.values()) {
            pedestrianRequests.put(direction, new HashSet<>());
        }
    }

    public synchronized List<Message> initialCommands() {
        return colorCommands(null, SignalColor.RED);
    }

    public synchronized List<Message> accept(Message message, long now) {
        if (!"controller".equals(message.getDestination())) {
            throw new IllegalArgumentException("message is not addressed to controller");
        }

        if (message.getType() == MessageType.EVENT) {
            handleEvent(message);
        } else if (message.getType() == MessageType.STATE) {
            List<Message> failSafeCommands = handleState(message, now);
            if (!failSafeCommands.isEmpty()) {
                return failSafeCommands;
            }
        } else {
            throw new IllegalArgumentException("controller accepts only EVENT or STATE messages");
        }
        return advance(now);
    }

    private void handleEvent(Message message) {
        Direction direction;
        switch (message.getAction()) {
            case "VEHICLE_DETECTED":
                requireSource(message.getSource(), "detector-(north|south|east|west)-[1-3]",
                        "vehicle event");
                direction = Direction.from(message.getSource(), message.getValue());
                vehicles.add(direction);
                break;
            case "VEHICLE_CLEARED":
                requireSource(message.getSource(), "detector-(north|south|east|west)-[1-3]",
                        "vehicle event");
                direction = Direction.from(message.getSource(), message.getValue());
                vehicles.remove(direction);
                break;
            case "PEDESTRIAN_REQUEST":
                requireSource(message.getSource(), "button-(north|south|east|west)",
                        "pedestrian event");
                direction = Direction.from(message.getSource(), message.getValue());
                pedestrianRequests.get(direction).add(message.getSource());
                break;
            default:
                throw new IllegalArgumentException("unsupported event action: " + message.getAction());
        }
    }

    private List<Message> handleState(Message message, long now) {
        if (!"STATUS".equals(message.getAction())) {
            return new ArrayList<>();
        }

        requireSource(message.getSource(), "aux-[a-z0-9]+(?:-[a-z0-9]+)*", "status message");

        if ("FAULT".equals(message.getValue()) || "OFFLINE".equals(message.getValue())) {
            boolean firstReport = faultedDevices.add(message.getSource());
            return firstReport ? forceAllRed(now) : new ArrayList<>();
        }
        if ("NORMAL".equals(message.getValue())) {
            faultedDevices.remove(message.getSource());
            return new ArrayList<>();
        }
        throw new IllegalArgumentException("unsupported device status: " + message.getValue());
    }

    private static void requireSource(String source, String pattern, String description) {
        if (!source.matches(pattern)) {
            throw new IllegalArgumentException("invalid source for " + description + ": " + source);
        }
    }

    public synchronized List<Message> advance(long now) {
        if (!faultedDevices.isEmpty()) {
            return new ArrayList<>();
        }

        long elapsed = now - stageStartedAt;
        switch (stage) {
            case ALL_RED:
                SignalGroup requested = selectRequestedGroup();
                if (requested != null && elapsed >= config.getAllRedMillis()) {
                    activeGroup = requested;
                    preferredNext = requested.opposite();
                    stage = Stage.GREEN;
                    stageStartedAt = now;
                    List<Message> commands = colorCommands(activeGroup, SignalColor.GREEN);
                    commands.addAll(clearServedPedestrianRequests(activeGroup));
                    return commands;
                }
                break;
            case GREEN:
                boolean opposingDemand = hasDemand(activeGroup.opposite());
                if (elapsed >= config.getMaximumGreenMillis()
                        || (opposingDemand && elapsed >= config.getMinimumGreenMillis())) {
                    stage = Stage.YELLOW;
                    stageStartedAt = now;
                    return colorCommands(activeGroup, SignalColor.YELLOW);
                }
                break;
            case YELLOW:
                if (elapsed >= config.getYellowMillis()) {
                    stage = Stage.ALL_RED;
                    activeGroup = null;
                    stageStartedAt = now;
                    return colorCommands(null, SignalColor.RED);
                }
                break;
            default:
                throw new IllegalStateException("unknown controller stage");
        }
        return new ArrayList<>();
    }

    public synchronized List<Message> forceAllRed(long now) {
        stage = Stage.ALL_RED;
        activeGroup = null;
        stageStartedAt = now;
        return colorCommands(null, SignalColor.RED);
    }

    private SignalGroup selectRequestedGroup() {
        boolean preferredDemand = hasDemand(preferredNext);
        boolean otherDemand = hasDemand(preferredNext.opposite());
        if (preferredDemand) {
            return preferredNext;
        }
        return otherDemand ? preferredNext.opposite() : null;
    }

    private boolean hasDemand(SignalGroup group) {
        for (Direction direction : Direction.values()) {
            if (group.contains(direction)
                    && (vehicles.contains(direction) || !pedestrianRequests.get(direction).isEmpty())) {
                return true;
            }
        }
        return false;
    }

    private List<Message> clearServedPedestrianRequests(SignalGroup group) {
        List<Message> messages = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (!group.contains(direction)) {
                continue;
            }
            for (String buttonId : pedestrianRequests.get(direction)) {
                messages.add(Message.command(buttonId, "CLEAR_REQUEST", ""));
            }
            pedestrianRequests.get(direction).clear();
        }
        return messages;
    }

    private List<Message> colorCommands(SignalGroup greenOrYellowGroup, SignalColor activeColor) {
        List<Message> messages = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            SignalColor color = greenOrYellowGroup != null && greenOrYellowGroup.contains(direction)
                    ? activeColor : SignalColor.RED;
            messages.add(Message.command(direction.lightId(), "SET_COLOR", color.name()));
        }
        return messages;
    }

    public synchronized Stage getStage() {
        return stage;
    }

    public synchronized SignalGroup getActiveGroup() {
        return activeGroup;
    }

    public synchronized boolean hasFaults() {
        return !faultedDevices.isEmpty();
    }
}
