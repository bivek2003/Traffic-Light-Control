package ui;

public final class RightTurnPathTest {
    public static void main(String[] args) {
        for (char direction : Simulation.DIRS) {
            checkPath(direction);
        }
        System.out.println("RightTurnPathTest: all four right turns stay forward, smooth and in lane");
    }

    private static void checkPath(char direction) {
        Vehicle car = new Vehicle(direction, 3, 32, 110, Palette.CAR_ACCENT,
                Vehicle.Maneuver.RIGHT);
        double[] entry = Simulation.place(direction, Simulation.STOP_T, 3);
        double heading = Math.toRadians(entry[2]);
        double forwardX = Math.cos(heading), forwardY = Math.sin(heading);
        double rightX = -forwardY, rightY = forwardX;
        car.t = Simulation.STOP_T - 1;
        double[] previous = Simulation.place(car);
        double previousTurn = 0;
        // Small steps catch reversing, jumping and sudden heading changes.
        for (double t = car.t + 0.25; t <= Simulation.EXIT_T; t += 0.25) {
            car.t = t;
            double[] point = Simulation.place(car);
            double dx = point[0] - previous[0], dy = point[1] - previous[1];
            check(dx * forwardX + dy * forwardY >= -0.000001, direction + " reversed");
            check(dx * rightX + dy * rightY >= -0.000001, direction + " turned left");
            double distance = Math.hypot(dx, dy);
            check(distance > 0.249 && distance <= 0.250001, direction + " jumped or slowed");
            double turn = (point[2] - entry[2] + 360) % 360;
            check(turn >= previousTurn - 0.000001 && turn <= 90.000001,
                    direction + " heading went beyond its right turn");
            check(turn - previousTurn < 1, direction + " heading snapped");
            previous = point;
            previousTurn = turn;
        }
        check(Math.abs(previousTurn - 90) < 0.000001, direction + " did not finish turning");
        char exit = switch (direction) {
            case 'N' -> 'E';
            case 'S' -> 'W';
            case 'E' -> 'S';
            default -> 'N';
        };
        double[] lane = Simulation.place(exit, Simulation.EXIT_T, 3);
        int sideways = exit == 'N' || exit == 'S' ? 0 : 1;
        check(Math.abs(previous[sideways] - lane[sideways]) < 0.000001,
                direction + " missed the outgoing right lane");
        // The join must also be smooth at the exact end of the quarter circle.
        double radius = Simulation.HALF + Simulation.BAR_FAR - 2.5 * Simulation.LANE;
        double end = Simulation.STOP_T + Math.PI * radius / 2;
        for (double join : new double[]{Simulation.STOP_T, end}) {
            car.t = join - 0.0001;
            double[] before = Simulation.place(car);
            car.t = join + 0.0001;
            double[] after = Simulation.place(car);
            check(Math.hypot(after[0] - before[0], after[1] - before[1]) < 0.000201,
                    direction + " discontinuous join");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
