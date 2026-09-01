package ui;

/**
 * One person crossing one of the four crosswalks.
 *
 * <p>Like {@link Vehicle}, position is a single number. {@code progress} runs
 * from 0 at the kerb the person started from to 1 at the far kerb, so the same
 * movement code serves all four crossings and only the drawing code needs to
 * know which way the crosswalk lies.
 */
public class Pedestrian {

    /** The crossing runs between these two world coordinates. */
    public static final double START = 285, END = 515;
    public static final double SPAN = END - START;

    /** Which crosswalk: N, S, E or W. */
    public final char side;

    /** True walks from START towards END, false the other way. */
    public final boolean forward;

    /** World units per second. People move a good deal slower than cars. */
    public final double speed;

    /** Offset across the width of the crossing, so a group does not overlap. */
    public final double lateral;

    public double progress;

    /** Drives the walking animation. */
    public double gait;

    /** True once the person has actually stepped onto the carriageway. */
    public boolean entered;

    public Pedestrian(char side, boolean forward, double speed,
                      double lateral, double progress) {
        this.side = side;
        this.forward = forward;
        this.speed = speed;
        this.lateral = lateral;
        this.progress = progress;
        this.gait = Math.random() * Math.PI * 2;
    }

    /** Position along the crossing, in world coordinates. */
    public double along() {
        return forward ? START + progress * SPAN : END - progress * SPAN;
    }

    /** True once the person has reached the far pavement. */
    public boolean finished() {
        return progress > 1.12;
    }

    /**
     * True while the person is actually on the carriageway, rather than still
     * on the pavement either side. The signal is held only for this.
     */
    public boolean onRoad() {
        double a = along();
        return a > Simulation.EDGE_A - 7 && a < Simulation.EDGE_B + 7;
    }
}
