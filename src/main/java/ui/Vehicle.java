import javafx.scene.paint.Color;

/** One car in the traffic simulation. */
public final class Vehicle {
    public enum Maneuver {
        STRAIGHT, LEFT, RIGHT
    }

    final char approach;
    final int lane;
    final Maneuver maneuver;
    double x;
    double y;
    final double speed;
    final double length;
    final Color color;
    double heading;
    double turnProgress = -1;
    double startX;
    double startY;
    double controlX;
    double controlY;
    double endX;
    double endY;
    double exitDx;
    double exitDy;

    public Vehicle(char approach, int lane, Maneuver maneuver,
                   double x, double y, double speed, double length,
                   double heading, Color color) {
        this.approach = approach;
        this.lane = lane;
        this.maneuver = maneuver;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.length = length;
        this.heading = heading;
        this.color = color;
    }
}
