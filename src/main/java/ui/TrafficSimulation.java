import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.scene.paint.Color;

/** Generates three-lane traffic and moves every car according to its light. */
public final class TrafficSimulation {
    private enum SignalPhase {
        ALL_RED, GREEN, YELLOW
    }

    public static final double WORLD = 800;
    public static final double CENTER = 400;
    public static final double LANE = 42;
    public static final double EDGE_A = CENTER - LANE * 3;
    public static final double EDGE_B = CENTER + LANE * 3;

    private final List<Vehicle> vehicles = new ArrayList<Vehicle>();
    private final Map<String, TrafficLight> lights;
    private final Map<String, VehicleDetector> detectors;
    private final Random random = new Random(7);
    private double spawnTimer;
    private SignalPhase signalPhase = SignalPhase.ALL_RED;
    private char activeApproach;
    private double signalTimer = 1.0;

    public TrafficSimulation(Map<String, TrafficLight> lights,
                             Map<String, VehicleDetector> detectors) {
        this.lights = lights;
        this.detectors = detectors;
        for (char direction : new char[]{'N', 'S', 'E', 'W'}) {
            addVehicle(direction);
        }
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void update(double seconds) {
        spawnTimer -= seconds;
        if (spawnTimer <= 0) {
            addVehicle("NSEW".charAt(random.nextInt(4)));
            spawnTimer = 0.9;
        }

        Iterator<Vehicle> iterator = vehicles.iterator();
        while (iterator.hasNext()) {
            Vehicle vehicle = iterator.next();
            move(vehicle, seconds);
            if (vehicle.x < -70 || vehicle.x > WORLD + 70
                    || vehicle.y < -70 || vehicle.y > WORLD + 70) {
                iterator.remove();
            }
        }
        updateDetectorsAndSignals(seconds);
    }

    private void updateDetectorsAndSignals(double seconds) {
        int[] demand = new int[4];
        for (Vehicle vehicle : vehicles) {
            if (approachingStop(vehicle)) {
                demand[directionIndex(vehicle.approach)]++;
            }
        }

        char[] directions = {'N', 'S', 'E', 'W'};
        for (int index = 0; index < directions.length; index++) {
            VehicleDetector detector = detectors.get(detectorId(directions[index]));
            String desired = demand[index] > 0 ? "DETECTED" : "CLEAR";
            if (!desired.equals(detector.getState())) {
                detector.apply(demand[index] > 0 ? "DETECT" : "CLEAR", "");
            }
        }

        signalTimer += seconds;
        char requested = busiestApproach(demand);
        if (signalPhase == SignalPhase.ALL_RED) {
            if (signalTimer >= 1.0 && requested != 0) {
                activeApproach = requested;
                setSignals(activeApproach, "GREEN");
                signalPhase = SignalPhase.GREEN;
                signalTimer = 0;
            }
            return;
        }

        if (signalPhase == SignalPhase.GREEN) {
            int activeDemand = demand[directionIndex(activeApproach)];
            int requestedDemand = requested == 0 ? 0 : demand[directionIndex(requested)];
            boolean changeRequested = requested != 0 && requested != activeApproach
                    && requestedDemand > activeDemand;
            if (signalTimer >= 10.0 || (signalTimer >= 4.0 && changeRequested)) {
                setSignals(activeApproach, "YELLOW");
                signalPhase = SignalPhase.YELLOW;
                signalTimer = 0;
            }
            return;
        }

        if (signalTimer >= 2.0) {
            setAllRed();
            signalPhase = SignalPhase.ALL_RED;
            activeApproach = 0;
            signalTimer = 0;
        }
    }

    private boolean approachingStop(Vehicle vehicle) {
        if (vehicle.turnProgress >= 0) {
            return false;
        }
        if (vehicle.approach == 'N') {
            return vehicle.y >= EDGE_A - 210 && vehicle.y <= EDGE_A;
        }
        if (vehicle.approach == 'S') {
            return vehicle.y <= EDGE_B + 210 && vehicle.y >= EDGE_B;
        }
        if (vehicle.approach == 'E') {
            return vehicle.x <= EDGE_B + 210 && vehicle.x >= EDGE_B;
        }
        return vehicle.x >= EDGE_A - 210 && vehicle.x <= EDGE_A;
    }

    private char busiestApproach(int[] demand) {
        char[] directions = {'N', 'S', 'E', 'W'};
        int best = 0;
        char selected = 0;
        for (int index = 0; index < demand.length; index++) {
            if (demand[index] > best) {
                best = demand[index];
                selected = directions[index];
            }
        }
        return selected;
    }

    private int directionIndex(char direction) {
        if (direction == 'N') return 0;
        if (direction == 'S') return 1;
        if (direction == 'E') return 2;
        return 3;
    }

    private String detectorId(char direction) {
        if (direction == 'N') return "detector-north";
        if (direction == 'S') return "detector-south";
        if (direction == 'E') return "detector-east";
        return "detector-west";
    }

    private void setSignals(char greenDirection, String color) {
        setAllRed();
        lights.get(lightId(greenDirection)).apply("SET_COLOR", color);
    }

    private void setAllRed() {
        for (TrafficLight light : lights.values()) {
            if (!"RED".equals(light.getState())) {
                light.apply("SET_COLOR", "RED");
            }
        }
    }

    public void addVehicle(char approach) {
        int lane = 1 + random.nextInt(3);
        if (entryOccupied(approach, lane)) {
            return;
        }
        Color[] colors = {
            Color.web("#cdd6dd"), Color.web("#93a5b2"),
            Color.web("#7c8b97"), Color.web("#b5c1ca"),
            Color.web("#5ec6d8")
        };
        Color color = colors[random.nextInt(colors.length)];
        double speed = 92 + random.nextDouble() * 25;
        double length = 31 + random.nextDouble() * 10;
        double choice = random.nextDouble();
        Vehicle.Maneuver maneuver = choice < 0.25 ? Vehicle.Maneuver.LEFT
                : choice < 0.50 ? Vehicle.Maneuver.RIGHT : Vehicle.Maneuver.STRAIGHT;
        double offset = LANE * (lane - 0.5);
        if (approach == 'N') {
            vehicles.add(new Vehicle('N', lane, maneuver, CENTER + offset, -30,
                    speed, length, 90, color));
        } else if (approach == 'S') {
            vehicles.add(new Vehicle('S', lane, maneuver, CENTER - offset, WORLD + 30,
                    speed, length, -90, color));
        } else if (approach == 'E') {
            vehicles.add(new Vehicle('E', lane, maneuver, WORLD + 30, CENTER + offset,
                    speed, length, 180, color));
        } else {
            vehicles.add(new Vehicle('W', lane, maneuver, -30, CENTER - offset,
                    speed, length, 0, color));
        }
    }

    private boolean entryOccupied(char approach, int lane) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.approach != approach || vehicle.lane != lane) {
                continue;
            }
            if ((approach == 'N' && vehicle.y < 70)
                    || (approach == 'S' && vehicle.y > WORLD - 70)
                    || (approach == 'E' && vehicle.x > WORLD - 70)
                    || (approach == 'W' && vehicle.x < 70)) {
                return true;
            }
        }
        return false;
    }

    private void move(Vehicle vehicle, double seconds) {
        double distance = vehicle.speed * seconds;
        if (vehicle.turnProgress >= 1) {
            vehicle.x += vehicle.exitDx * distance;
            vehicle.y += vehicle.exitDy * distance;
            return;
        }
        if (vehicle.turnProgress >= 0) {
            advanceTurn(vehicle, distance);
            return;
        }

        boolean green = "GREEN".equals(lights.get(lightId(vehicle.approach)).getState());
        if (vehicle.approach == 'N') {
            vehicle.y = advancePositive(vehicle.y, distance, EDGE_A - 28, green);
        } else if (vehicle.approach == 'S') {
            vehicle.y = advanceNegative(vehicle.y, distance, EDGE_B + 28, green);
        } else if (vehicle.approach == 'E') {
            vehicle.x = advanceNegative(vehicle.x, distance, EDGE_B + 28, green);
        } else {
            vehicle.x = advancePositive(vehicle.x, distance, EDGE_A - 28, green);
        }

        if (vehicle.maneuver != Vehicle.Maneuver.STRAIGHT && enteredIntersection(vehicle)) {
            beginTurn(vehicle);
        }
    }

    private boolean enteredIntersection(Vehicle vehicle) {
        if (vehicle.approach == 'N') return vehicle.y >= EDGE_A;
        if (vehicle.approach == 'S') return vehicle.y <= EDGE_B;
        if (vehicle.approach == 'E') return vehicle.x <= EDGE_B;
        return vehicle.x >= EDGE_A;
    }

    private void beginTurn(Vehicle vehicle) {
        double outgoing = LANE * 0.5;
        vehicle.turnProgress = 0;
        if (vehicle.approach == 'N') {
            vehicle.startX = vehicle.x;
            vehicle.startY = EDGE_A;
            if (vehicle.maneuver == Vehicle.Maneuver.RIGHT) {
                setCurve(vehicle, EDGE_A, EDGE_A, EDGE_A, CENTER + outgoing, -1, 0);
            } else {
                setCurve(vehicle, EDGE_B, EDGE_A, EDGE_B, CENTER - outgoing, 1, 0);
            }
        } else if (vehicle.approach == 'S') {
            vehicle.startX = vehicle.x;
            vehicle.startY = EDGE_B;
            if (vehicle.maneuver == Vehicle.Maneuver.RIGHT) {
                setCurve(vehicle, EDGE_B, EDGE_B, EDGE_B, CENTER - outgoing, 1, 0);
            } else {
                setCurve(vehicle, EDGE_A, EDGE_B, EDGE_A, CENTER + outgoing, -1, 0);
            }
        } else if (vehicle.approach == 'E') {
            vehicle.startX = EDGE_B;
            vehicle.startY = vehicle.y;
            if (vehicle.maneuver == Vehicle.Maneuver.RIGHT) {
                setCurve(vehicle, EDGE_B, EDGE_A, CENTER - outgoing, EDGE_A, 0, -1);
            } else {
                setCurve(vehicle, EDGE_B, EDGE_B, CENTER + outgoing, EDGE_B, 0, 1);
            }
        } else {
            vehicle.startX = EDGE_A;
            vehicle.startY = vehicle.y;
            if (vehicle.maneuver == Vehicle.Maneuver.RIGHT) {
                setCurve(vehicle, EDGE_A, EDGE_B, CENTER + outgoing, EDGE_B, 0, 1);
            } else {
                setCurve(vehicle, EDGE_A, EDGE_A, CENTER - outgoing, EDGE_A, 0, -1);
            }
        }
        vehicle.x = vehicle.startX;
        vehicle.y = vehicle.startY;
    }

    private void setCurve(Vehicle vehicle, double controlX, double controlY,
                          double endX, double endY, double exitDx, double exitDy) {
        vehicle.controlX = controlX;
        vehicle.controlY = controlY;
        vehicle.endX = endX;
        vehicle.endY = endY;
        vehicle.exitDx = exitDx;
        vehicle.exitDy = exitDy;
    }

    private void advanceTurn(Vehicle vehicle, double distance) {
        vehicle.turnProgress = Math.min(1, vehicle.turnProgress + distance / 190.0);
        double t = vehicle.turnProgress;
        double oneMinusT = 1 - t;
        vehicle.x = oneMinusT * oneMinusT * vehicle.startX
                + 2 * oneMinusT * t * vehicle.controlX + t * t * vehicle.endX;
        vehicle.y = oneMinusT * oneMinusT * vehicle.startY
                + 2 * oneMinusT * t * vehicle.controlY + t * t * vehicle.endY;

        double dx = 2 * oneMinusT * (vehicle.controlX - vehicle.startX)
                + 2 * t * (vehicle.endX - vehicle.controlX);
        double dy = 2 * oneMinusT * (vehicle.controlY - vehicle.startY)
                + 2 * t * (vehicle.endY - vehicle.controlY);
        vehicle.heading = Math.toDegrees(Math.atan2(dy, dx));
    }

    private double advancePositive(double position, double distance,
                                   double stop, boolean green) {
        if (!green && position <= stop) {
            return Math.min(position + distance, stop);
        }
        return position + distance;
    }

    private double advanceNegative(double position, double distance,
                                   double stop, boolean green) {
        if (!green && position >= stop) {
            return Math.max(position - distance, stop);
        }
        return position - distance;
    }

    private String lightId(char approach) {
        if (approach == 'N') return "light-north";
        if (approach == 'S') return "light-south";
        if (approach == 'E') return "light-east";
        return "light-west";
    }
}
