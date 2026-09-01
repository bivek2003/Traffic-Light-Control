package ui;

import javafx.scene.paint.Color;

/**
 * One vehicle travelling along one approach.
 *
 * <p>Position is held as a single number, {@code t}, being the distance
 * travelled from where the vehicle entered the view. Using one travel
 * coordinate instead of x and y means the four approaches share exactly the
 * same movement code, and the stop line sits at the same value of {@code t}
 * for all of them. {@link IntersectionView} turns {@code t} back into screen
 * coordinates when it draws.
 */
public class Vehicle {

    public enum Maneuver {
        LEFT,
        STRAIGHT,
        RIGHT
    }

    /** Direction of travel: N, S, E or W. */
    public final char dir;

    /** Lane 1 to 3, counted outward from the yellow centre line. */
    public final int lane;

    /** Movement through the intersection. */
    public final Maneuver maneuver;

    public final double length;
    public final double width = 15;
    public final double topSpeed;
    public final Color colour;

    /** Distance travelled along this approach. */
    public double t;

    /** Current speed, in world units per second. */
    public double speed;

    /** True once the lane detector has reported this vehicle. */
    public boolean counted;

    /** True once the detector has reported that this vehicle left. */
    public boolean detectorCleared;

    public Vehicle(char dir, int lane, double length, double topSpeed, Color colour) {
        this(dir, lane, length, topSpeed, colour, Maneuver.STRAIGHT);
    }

    public Vehicle(char dir, int lane, double length, double topSpeed, Color colour,
                   Maneuver maneuver) {
        this.dir = dir;
        this.lane = lane;
        this.length = length;
        this.topSpeed = topSpeed;
        this.colour = colour;
        this.maneuver = maneuver;
        this.t = -length;
        this.speed = 0;
    }

    /** Front bumper, in travel coordinates. */
    public double front() {
        return t + length / 2;
    }

    /** Rear bumper, in travel coordinates. */
    public double rear() {
        return t - length / 2;
    }

    /** Stopped or crawling, which is when the brake lights show. */
    public boolean braking() {
        return speed < 8;
    }
}
