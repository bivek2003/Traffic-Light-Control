package ui;

import devices.PedLightStatus;
import objects.Phase;
import java.util.ArrayList;
import java.util.List;

public final class TrafficSpacingTest {
    public static void main(String[] args) {
        for (char direction : Simulation.DIRS) {
            for (int lane = 1; lane <= 3; lane++) queue(direction, lane);
        }
        intersection();
        System.out.println("TrafficSpacingTest: all 12 queues keep space through green and turns");
    }

    private static void intersection() {
        Simulation sim = new Simulation(null);
        sim.vehicles().clear();
        sim.setDensity(0);
        for (char dir : Simulation.DIRS) {
            for (int lane = 1; lane <= 3; lane++) {
                for (int i = 0; i < 6; i++) {
                    Vehicle.Maneuver turn = lane == 1 ? Vehicle.Maneuver.LEFT
                            : lane == 3 ? Vehicle.Maneuver.RIGHT : Vehicle.Maneuver.STRAIGHT;
                    Vehicle car = new Vehicle(dir, lane, 42, 108 + i * 6, Palette.CAR_ACCENT, turn);
                    car.t = Simulation.STOP_T - 22 - i * 64;
                    sim.vehicles().add(car);
                }
            }
        }
        for (Phase phase : Phase.values()) {
            for (var pattern : new devices.TrafficPattern[]{phase.green, phase.yellow,
                    devices.TrafficPattern.ALL_RED}) {
                sim.applyLabState(pattern, PedLightStatus.STOP);
                int greenFrames = phase == phase.road.left() ? 200 : 600;
                int frames = pattern == phase.green ? greenFrames : pattern == phase.yellow ? 80 : 40;
                for (int frame = 0; frame < frames; frame++) {
                    sim.advance(0.05);
                    List<Vehicle> cars = sim.vehicles();
                    for (int i = 0; i < cars.size(); i++) {
                        for (int j = i + 1; j < cars.size(); j++) {
                            double[] p = Simulation.place(cars.get(i)), q = Simulation.place(cars.get(j));
                            if (Math.hypot(p[0] - q[0], p[1] - q[1]) < 70) {
                                Vehicle a = cars.get(i), b = cars.get(j);
                                check(separate(a, b, 4), "busy overlap " + pattern + " " + a.dir + a.lane
                                        + "@" + a.t + " with " + b.dir + b.lane + "@" + b.t);
                            }
                        }
                    }
                }
            }
        }
        check(sim.vehicles().isEmpty(), "busy intersection did not empty");
    }

    private static void queue(char direction, int lane) {
        Simulation sim = new Simulation(null);
        sim.vehicles().clear();
        sim.setDensity(0);
        boolean ns = direction == 'N' || direction == 'S';
        Phase phase = lane == 1 ? (ns ? Phase.NS_LEFT : Phase.EW_LEFT)
                : (ns ? Phase.NS_THROUGH : Phase.EW_THROUGH);
        sim.applyLabState(devices.TrafficPattern.ALL_RED, PedLightStatus.STOP);
        List<Vehicle> cars = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Vehicle.Maneuver turn = lane == 1 ? Vehicle.Maneuver.LEFT
                    : lane == 3 ? Vehicle.Maneuver.RIGHT : Vehicle.Maneuver.STRAIGHT;
            Vehicle car = new Vehicle(direction, lane, 42, i == 0 ? 60 : 142,
                    Palette.CAR_ACCENT, turn);
            car.t = Simulation.STOP_T - 22 - i * 64;
            cars.add(car);
            sim.vehicles().add(car);
        }
        for (int i = 0; i < 150; i++) sim.advance(0.02);
        for (Vehicle car : cars) check(car.front() < Simulation.STOP_T, "ran red");
        sim.applyLabState(phase.green, PedLightStatus.STOP);
        for (int frame = 0; frame < 1_500; frame++) {
            sim.advance(0.02);
            for (int i = 1; i < cars.size(); i++) {
                Vehicle leader = cars.get(i - 1), follower = cars.get(i);
                if (!sim.vehicles().contains(leader) || !sim.vehicles().contains(follower)) continue;
                check(leader.rear() - follower.front() >= 9 - 0.001,
                        direction + " lane " + lane + " lost its following gap");
                check(separate(leader, follower, 9), direction + " lane " + lane + " bodies overlap");
            }
        }
        check(sim.vehicles().isEmpty(), direction + " lane " + lane + " never cleared green");
        if (lane == 1) {
            double[] finalPosition = Simulation.place(cars.get(0));
            double[] entry = Simulation.place(direction, 0, lane);
            double turn = (entry[2] - finalPosition[2] + 360) % 360;
            check(Math.abs(turn - 90) < 0.001, direction + " did not turn left");
        }
    }

    // Project both car rectangles onto their headings and sides, with a visible gap.
    private static boolean separate(Vehicle a, Vehicle b, double gap) {
        double[] p = Simulation.place(a), q = Simulation.place(b);
        double ah = Math.toRadians(p[2]), bh = Math.toRadians(q[2]);
        double ax = Math.cos(ah), ay = Math.sin(ah), bx = Math.cos(bh), by = Math.sin(bh);
        double[][] axes = {{ax, ay}, {-ay, ax}, {bx, by}, {-by, bx}};
        for (double[] axis : axes) {
            double distance = Math.abs((q[0] - p[0]) * axis[0] + (q[1] - p[1]) * axis[1]);
            double ar = (a.length + gap) / 2 * Math.abs(ax * axis[0] + ay * axis[1])
                    + (a.width + gap) / 2 * Math.abs(-ay * axis[0] + ax * axis[1]);
            double br = (b.length + gap) / 2 * Math.abs(bx * axis[0] + by * axis[1])
                    + (b.width + gap) / 2 * Math.abs(-by * axis[0] + bx * axis[1]);
            if (distance >= ar + br - 0.001) return true;
        }
        return false;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
