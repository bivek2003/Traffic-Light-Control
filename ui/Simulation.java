package ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The traffic model behind the display: the signal cycle, the vehicles and
 * the messages they generate.
 *
 * <p>This runs the signal cycle locally so the display can be developed and
 * demonstrated on its own. At integration the cycle is replaced by COMMAND
 * messages arriving from the controller through the multiplexor, and
 * {@link #applyColour} is the single place that has to change.
 */
public class Simulation {

    /**
     * Receives every message the display would put on the wire. The last
     * argument marks detector traffic, which is high volume and is filtered
     * out of the log by default.
     */
    public interface MessageSink {
        void message(String type, String source, String destination,
                     String action, String value, boolean detector);
    }

    // ---- geometry -------------------------------------------------------
    // One logical 800 x 800 world. The view scales it to whatever size it is
    // given, so every number here stays a round, readable value.

    /** Width and height of the world. */
    public static final double WORLD = 800;
    /** Centre of the intersection. */
    public static final double C = 400;
    /** Lane width. */
    public static final double LANE = 26;
    /** Half a carriageway: three lanes. */
    public static final double HALF = 3 * LANE;
    /** Near and far edges of the carriageway. */
    public static final double EDGE_A = C - HALF, EDGE_B = C + HALF;
    // ---- crossing geometry ---------------------------------------------
    // Measured outward from the edge of the junction. Approaching traffic
    // meets the stop bar first, then the crosswalk, then the junction, so a
    // vehicle waiting at the bar is always behind the people crossing.

    /** Clear space between the junction and the crosswalk. */
    public static final double CROSS_INSET = 6;
    /** Width of the crosswalk itself. */
    public static final double CROSS_WIDTH = 26;
    /** Clear space between the crosswalk and the stop bar. */
    public static final double BAR_GAP = 5;
    /** Thickness of the painted stop bar. */
    public static final double BAR_WIDTH = 5;

    /** Centre of the crosswalk, out from the junction edge. */
    public static final double CROSS_MID = CROSS_INSET + CROSS_WIDTH / 2;
    /** Near and far faces of the stop bar, out from the junction edge. */
    public static final double BAR_NEAR = CROSS_INSET + CROSS_WIDTH + BAR_GAP;
    public static final double BAR_FAR = BAR_NEAR + BAR_WIDTH;

    /** Travel coordinate at which an approach meets the junction. */
    private static final double JUNCTION_T = 392;

    /**
     * Stop line, in travel coordinates, identical on all four approaches.
     * Derived from the bar rather than typed in, so the paint and the place
     * vehicles actually stop cannot drift apart.
     */
    public static final double STOP_T = JUNCTION_T - BAR_FAR;
    /** A vehicle is finished once it travels this far. */
    public static final double EXIT_T = 940;

    public static final char[] DIRS = {'N', 'S', 'E', 'W'};

    /**
     * Turns a travel coordinate into a world position and heading:
     * x, y, and the rotation in degrees.
     */
    public static double[] place(char dir, double t, int lane) {
        double off = LANE * (lane - 0.5);
        return switch (dir) {
            case 'N' -> new double[]{C + off, 870 - t, -90};
            case 'S' -> new double[]{C - off, -70 + t, 90};
            case 'E' -> new double[]{-70 + t, C + off, 0};
            default  -> new double[]{870 - t, C - off, 180};
        };
    }

    /** Centre of the crosswalk on one side, in world coordinates. */
    public static double crossingBand(char side) {
        return switch (side) {
            case 'N' -> EDGE_A - CROSS_MID;
            case 'S' -> EDGE_B + CROSS_MID;
            case 'E' -> EDGE_B + CROSS_MID;
            default  -> EDGE_A - CROSS_MID;
        };
    }

    // ---- signal cycle ---------------------------------------------------

    private record Phase(String ns, String ew, double seconds, String label) { }

    private static final Phase[] PHASES = {
        new Phase("GREEN", "RED",   7.0, "North-south green"),
        new Phase("AMBER", "RED",   2.0, "North-south amber"),
        new Phase("RED",   "RED",   1.2, "All red"),
        new Phase("RED",   "GREEN", 7.0, "East-west green"),
        new Phase("RED",   "AMBER", 2.0, "East-west amber"),
        new Phase("RED",   "RED",   1.2, "All red")
    };

    private int phase = 0;
    private double phaseElapsed = 0;

    private final Map<Character, String> colours = new LinkedHashMap<>();
    private final Map<Character, Boolean> pedestrianWaiting = new LinkedHashMap<>();
    private final Map<Character, Boolean> pedestrianReady = new LinkedHashMap<>();

    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<Pedestrian> pedestrians = new ArrayList<>();
    private final Map<Character, Boolean> wasWalking = new LinkedHashMap<>();
    private final Random random = new Random();
    private final MessageSink sink;

    private double spawnClock = 0;
    private double density = 1.0;
    private boolean deviceControlled;

    public Simulation(MessageSink sink) {
        this.sink = sink;
        for (char d : DIRS) {
            colours.put(d, "RED");
            pedestrianWaiting.put(d, false);
            pedestrianReady.put(d, false);
            wasWalking.put(d, false);
        }
        applyPhase();
        seedTraffic();
    }

    // ---- device names ---------------------------------------------------

    public static String headId(char dir) {
        return switch (dir) {
            case 'N' -> "light-north";
            case 'S' -> "light-south";
            case 'E' -> "light-east";
            default  -> "light-west";
        };
    }

    public static String buttonId(char dir) {
        return switch (dir) {
            case 'N' -> "button-north";
            case 'S' -> "button-south";
            case 'E' -> "button-east";
            default  -> "button-west";
        };
    }

    public static String roadName(char dir) {
        return switch (dir) {
            case 'N' -> "NORTH";
            case 'S' -> "SOUTH";
            case 'E' -> "EAST";
            default  -> "WEST";
        };
    }

    // ---- accessors used by the view -------------------------------------

    public String colourOf(char dir)      { return colours.get(dir); }
    public boolean pedestrianWaiting(char dir) { return pedestrianWaiting.get(dir); }
    /** True while somebody is actually on that crosswalk. */
    public boolean pedestrianWalking(char dir) {
        for (Pedestrian p : pedestrians) {
            if (p.side == dir) {
                return true;
            }
        }
        return false;
    }

    public List<Pedestrian> pedestrians() {
        return pedestrians;
    }
    public String phaseLabel() {
        if (!deviceControlled) {
            return PHASES[phase].label();
        }
        if ("RED".equals(colours.get('N')) && "RED".equals(colours.get('S'))
                && "RED".equals(colours.get('E')) && "RED".equals(colours.get('W'))) {
            return "All red";
        }
        if (!"RED".equals(colours.get('N')) || !"RED".equals(colours.get('S'))) {
            String colour = !"RED".equals(colours.get('N'))
                    ? colours.get('N') : colours.get('S');
            return "North-south " + colour.toLowerCase();
        }
        String colour = !"RED".equals(colours.get('E'))
                ? colours.get('E') : colours.get('W');
        return "East-west " + colour.toLowerCase();
    }
    public List<Vehicle> vehicles()       { return vehicles; }
    public void setDensity(double d)      { this.density = d; }

    public void useDeviceControl() {
        deviceControlled = true;
    }

    // ---- the loop -------------------------------------------------------

    public void advance(double dt) {
        phaseElapsed += dt;
        if (!deviceControlled && phaseElapsed >= PHASES[phase].seconds()) {
            int next = (phase + 1) % PHASES.length;
            // Hold the red rather than turn it green under somebody's feet.
            if (!crossingWouldBlock(next)) {
                phaseElapsed = 0;
                phase = next;
                applyPhase();
                releasePendingRequests();
            }
        }
        movePedestrians(dt);
        maybeSpawn(dt);
        moveVehicles(dt);
    }

    /**
     * True if moving to the given phase would give a green to a road somebody
     * is still walking across.
     */
    private boolean crossingWouldBlock(int next) {
        boolean acrossNS = false, acrossEW = false;
        for (Pedestrian p : pedestrians) {
            // Counts those still waiting to set off as well as those already out.
            if (p.entered && !p.onRoad()) {
                continue;
            }
            if (p.side == 'N' || p.side == 'S') {
                acrossNS = true;
            } else {
                acrossEW = true;
            }
        }
        if (acrossNS && !"RED".equals(PHASES[next].ns())) {
            return true;
        }
        return acrossEW && !"RED".equals(PHASES[next].ew());
    }

    private void applyPhase() {
        for (char d : DIRS) {
            String next = (d == 'N' || d == 'S') ? PHASES[phase].ns() : PHASES[phase].ew();
            applyColour(d, next);
        }
    }

    /**
     * Sets one head's colour and announces it. At integration this becomes the
     * handler for an incoming COMMAND message rather than something the
     * display decides for itself.
     */
    private void applyColour(char dir, String colour) {
        if (colour.equals(colours.get(dir))) {
            return;
        }
        colours.put(dir, colour);
        emit("COMMAND", "controller", headId(dir), "SET_COLOR", colour, false);
        emit("STATE", headId(dir), "controller", "COLOR", colour, false);
    }

    public void applyDeviceState(String id, String type, String state) {
        if ("TRAFFIC_LIGHT".equals(type)) {
            char dir = directionFromId(id);
            colours.put(dir, "YELLOW".equals(state) ? "AMBER" : state);
            startReadyCrossing(dir);
        } else if ("PEDESTRIAN_BUTTON".equals(type)) {
            char dir = directionFromId(id);
            if ("REQUESTED".equals(state)) {
                pedestrianWaiting.put(dir, true);
                pedestrianReady.put(dir, false);
            } else if ("IDLE".equals(state)) {
                pedestrianReady.put(dir, pedestrianWaiting.get(dir));
                startReadyCrossing(dir);
            }
        }
    }

    private char directionFromId(String id) {
        if (id.contains("north")) return 'N';
        if (id.contains("south")) return 'S';
        if (id.contains("east")) return 'E';
        if (id.contains("west")) return 'W';
        throw new IllegalArgumentException("unknown device direction: " + id);
    }

    private void startReadyCrossing(char dir) {
        if (pedestrianReady.get(dir) && "RED".equals(colours.get(dir))
                && !pedestrianWalking(dir)) {
            startCrossing(dir);
        }
    }

    private void emit(String type, String src, String dst,
                      String action, String value, boolean detector) {
        if (sink != null) {
            sink.message(type, src, dst, action, value, detector);
        }
    }

    // ---- pedestrians ----------------------------------------------------

    /**
     * A button press. If that road is already stopped the group steps off the
     * kerb straight away; otherwise the request is held, and the green running
     * at the time is cut short so nobody waits out a full cycle.
     */
    public void pressPedestrian(char dir) {
        if (pedestrianWaiting.get(dir) || pedestrianWalking(dir)) {
            return;
        }
        emit("EVENT", buttonId(dir), "controller", "PEDESTRIAN_REQUEST", "PRESSED", false);

        if (deviceControlled) {
            pedestrianWaiting.put(dir, true);
            return;
        }

        if ("RED".equals(colours.get(dir))) {
            startCrossing(dir);
        } else {
            pedestrianWaiting.put(dir, true);
            cutGreenShort();
        }
    }

    /** Leaves at most a couple of seconds of the running phase. */
    private void cutGreenShort() {
        double remaining = PHASES[phase].seconds() - phaseElapsed;
        if (remaining > 2.0) {
            phaseElapsed = PHASES[phase].seconds() - 2.0;
        }
    }

    /** Puts a small group of people onto one crosswalk. */
    private void startCrossing(char dir) {
        pedestrianWaiting.put(dir, false);
        pedestrianReady.put(dir, false);
        int people = 2 + random.nextInt(3);
        for (int i = 0; i < people; i++) {
            pedestrians.add(new Pedestrian(
                    dir,
                    random.nextBoolean(),
                    40 + random.nextDouble() * 16,
                    -7.5 + random.nextDouble() * 15,
                    -i * 0.09));
        }
        wasWalking.put(dir, true);
        emit("STATE", buttonId(dir), "controller", "PEDESTRIAN_REQUEST", "WALK", false);
    }

    /** After a phase change, let anybody waiting on a now-red road set off. */
    private void releasePendingRequests() {
        for (char d : DIRS) {
            if (pedestrianWaiting.get(d) && "RED".equals(colours.get(d))) {
                startCrossing(d);
            }
        }
    }

    /**
     * True when no vehicle is on or about to sweep through this crossing.
     * Only traffic on the road being crossed can matter, which is why the
     * other carriageway is skipped.
     */
    private boolean crossingClear(char side) {
        boolean acrossNS = (side == 'N' || side == 'S');
        double band = crossingBand(side);

        // Only traffic leaving the junction through this crossing can reach
        // it while the signal is red, and that is the traffic heading the same
        // way as the crossing's own side. The corridor spans the whole
        // junction, because a vehicle held in the middle of it will come out
        // through here once it gets moving.
        double low, high;
        if (side == 'N' || side == 'W') {     // leaves towards the lower edge
            low = band - 25;
            high = EDGE_B + 20;
        } else {                               // leaves towards the higher edge
            low = EDGE_A - 20;
            high = band + 25;
        }

        for (Vehicle v : vehicles) {
            if (v.dir != side) {
                continue;
            }
            double[] q = place(v.dir, v.t, v.lane);
            double pos = acrossNS ? q[1] : q[0];
            if (pos > low && pos < high) {
                return false;
            }
        }
        return true;
    }

    private void movePedestrians(double dt) {
        Iterator<Pedestrian> it = pedestrians.iterator();
        while (it.hasNext()) {
            Pedestrian p = it.next();

            // Wait on the kerb until the road ahead is actually empty. Traffic
            // leaving the junction crosses the far side of the intersection
            // just as a red begins.
            if (!p.entered && !crossingClear(p.side)) {
                p.gait += dt * 3.5;
                continue;
            }

            p.progress += (p.speed / Pedestrian.SPAN) * dt;
            p.gait += dt * 9.5;
            if (p.onRoad()) {
                p.entered = true;
            }
            if (p.finished()) {
                it.remove();
            }
        }
        // Report each crossing once, as the last person steps clear.
        for (char d : DIRS) {
            boolean now = pedestrianWalking(d);
            if (wasWalking.get(d) && !now) {
                emit("STATE", buttonId(d), "controller", "PEDESTRIAN_REQUEST", "CLEARED", false);
            }
            wasWalking.put(d, now);
        }
    }

    // ---- vehicles -------------------------------------------------------

    /**
     * Puts some traffic on the roads so the first frame is not an empty
     * junction. Seeded lane by lane with a real gap between each vehicle;
     * dropping them at random positions lands cars on top of one another.
     */
    private void seedTraffic() {
        for (char d : DIRS) {
            for (int lane = 1; lane <= 3; lane++) {
                double t = random.nextDouble() * 70;
                while (t < 330) {
                    Vehicle v = newVehicle(d, lane);
                    v.t = t;
                    vehicles.add(v);
                    // Longest vehicle is 42, and following distance is 9.
                    t += 95 + random.nextDouble() * 130;
                }
            }
        }
    }

    private Vehicle newVehicle(char dir, int lane) {
        double length = 30 + random.nextDouble() * 12;
        double top = 108 + random.nextDouble() * 34;
        javafx.scene.paint.Color colour = random.nextDouble() < 0.12
                ? Palette.CAR_ACCENT
                : Palette.CAR[random.nextInt(Palette.CAR.length)];
        return new Vehicle(dir, lane, length, top, colour);
    }

    private void maybeSpawn(double dt) {
        spawnClock -= dt;
        if (spawnClock > 0) {
            return;
        }
        spawnClock = 0.55 / density;
        char dir = DIRS[random.nextInt(4)];
        int lane = 1 + random.nextInt(3);
        if (!entryBlocked(dir, lane)) {
            vehicles.add(newVehicle(dir, lane));
        }
    }

    private boolean entryBlocked(char dir, int lane) {
        for (Vehicle v : vehicles) {
            if (v.dir == dir && v.lane == lane && v.t < 90) {
                return true;
            }
        }
        return false;
    }

    /** Green always lets a vehicle through; amber only if it is too close to pull up. */
    private boolean mayProceed(char dir, double gap) {
        String c = colours.get(dir);
        if ("GREEN".equals(c)) {
            return true;
        }
        if ("AMBER".equals(c)) {
            return gap < 34;
        }
        return false;
    }

    /** Distance from this vehicle's front bumper to the rear of the one ahead. */
    private double leaderGap(Vehicle v) {
        double best = Double.MAX_VALUE;
        for (Vehicle o : vehicles) {
            if (o == v || o.dir != v.dir || o.lane != v.lane || o.t <= v.t) {
                continue;
            }
            double gap = o.rear() - v.front();
            if (gap < best) {
                best = gap;
            }
        }
        return best;
    }

    private void moveVehicles(double dt) {
        for (Vehicle v : vehicles) {
            double frontGap = STOP_T - v.front();

            if (!v.counted && frontGap < 70 && frontGap > 0) {
                v.counted = true;
                emit("EVENT",
                     "detector-" + Character.toLowerCase(v.dir) + "-" + v.lane,
                     "controller", "VEHICLE_DETECTED",
                     roadName(v.dir) + "_LANE_" + v.lane, true);
            }
            if (v.counted && !v.detectorCleared && frontGap < -20) {
                v.detectorCleared = true;
                emit("EVENT",
                     "detector-" + Character.toLowerCase(v.dir) + "-" + v.lane,
                     "controller", "VEHICLE_CLEARED",
                     roadName(v.dir) + "_LANE_" + v.lane, true);
            }

            // How far this vehicle may advance this frame.
            //
            // Vehicles held at a red stop a whisker short of the bar rather
            // than exactly on it. A vehicle resting with frontGap == 0 reads
            // as already across on the next frame, and accelerates through
            // the red.
            double room = Double.MAX_VALUE;
            if (frontGap > 0 && !mayProceed(v.dir, frontGap)) {
                room = Math.max(0, frontGap - 0.8);
            }
            double ahead = leaderGap(v) - 9;
            if (ahead < room) {
                room = ahead;
            }

            double target = room <= 0.5 ? 0 : v.topSpeed;
            // Ease toward the target so queues compress rather than snap.
            v.speed += (target - v.speed) * Math.min(1, dt * 7);
            if (v.speed < 0.4) {
                v.speed = 0;
            }

            double advance = v.speed * dt;
            if (advance > room) {
                advance = Math.max(0, room);
            }
            v.t += advance;
        }
        vehicles.removeIf(v -> v.t >= EXIT_T);
    }
}
