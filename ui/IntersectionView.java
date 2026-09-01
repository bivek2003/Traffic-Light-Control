package ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import static ui.Simulation.C;
import static ui.Simulation.EDGE_A;
import static ui.Simulation.EDGE_B;
import static ui.Simulation.HALF;
import static ui.Simulation.LANE;
import static ui.Simulation.WORLD;

/**
 * Draws the intersection. Everything is drawn in the 800 x 800 world that
 * {@link Simulation} works in, and a single scale factor maps that to
 * whatever size the canvas actually is.
 */
public class IntersectionView extends Canvas {

    private final Simulation sim;
    private final double scale;

    // Crossing geometry comes from Simulation so the paint sits exactly where
    // the vehicles think it does.
    private static final double WALK = Simulation.CROSS_WIDTH;
    static final double CROSS_NEAR = EDGE_A - Simulation.CROSS_MID;
    static final double CROSS_FAR  = EDGE_B + Simulation.CROSS_MID;

    private static final Font ID_FONT   = Font.font("Menlo", FontWeight.SEMI_BOLD, 11);
    private static final Font LANE_FONT = Font.font("Menlo", FontWeight.SEMI_BOLD, 10);

    public IntersectionView(Simulation sim, double size) {
        super(size, size);
        this.sim = sim;
        this.scale = size / WORLD;
    }

    public void render() {
        GraphicsContext g = getGraphicsContext2D();
        g.save();
        g.scale(scale, scale);
        drawRoad(g);
        drawMarkings(g);
        for (Vehicle v : sim.vehicles()) {
            drawVehicle(g, v);
        }
        drawPedestrians(g);
        for (char d : Simulation.DIRS) {
            drawHead(g, d);
        }
        drawLabels(g);
        g.restore();
    }

    // ---- road surface ---------------------------------------------------

    private void drawRoad(GraphicsContext g) {
        g.setFill(Palette.SURROUND);
        g.fillRect(0, 0, WORLD, WORLD);

        g.setFill(Palette.KERB);
        g.fillRect(EDGE_A - 3, 0, HALF * 2 + 6, WORLD);
        g.fillRect(0, EDGE_A - 3, WORLD, HALF * 2 + 6);

        g.setFill(Palette.ASPHALT);
        g.fillRect(EDGE_A, 0, HALF * 2, WORLD);
        g.fillRect(0, EDGE_A, WORLD, HALF * 2);

        // A slightly lighter box so the junction reads as its own surface.
        g.setFill(Palette.JUNCTION);
        g.fillRect(EDGE_A, EDGE_A, HALF * 2, HALF * 2);
    }

    // ---- markings -------------------------------------------------------

    private void dashed(GraphicsContext g, double x1, double y1, double x2, double y2) {
        g.save();
        g.setStroke(Palette.PAINT);
        g.setGlobalAlpha(0.55);
        g.setLineWidth(2.5);
        g.setLineDashes(17, 15);
        g.strokeLine(x1, y1, x2, y2);
        g.restore();
    }

    private void drawMarkings(GraphicsContext g) {
        // Broken white lane dividers, stopping short of the junction.
        for (int d = 1; d <= 2; d++) {
            for (int s = -1; s <= 1; s += 2) {
                double p = C + s * LANE * d;
                dashed(g, p, 0, p, EDGE_A);
                dashed(g, p, EDGE_B, p, WORLD);
                dashed(g, 0, p, EDGE_A, p);
                dashed(g, EDGE_B, p, WORLD, p);
            }
        }

        // Double solid yellow: the divider between opposing traffic.
        g.save();
        g.setStroke(Palette.YELLOW);
        g.setLineWidth(2.6);
        g.setLineDashes();
        for (double o : new double[]{-3.2, 3.2}) {
            g.strokeLine(C + o, 0, C + o, EDGE_A);
            g.strokeLine(C + o, EDGE_B, C + o, WORLD);
            g.strokeLine(0, C + o, EDGE_A, C + o);
            g.strokeLine(EDGE_B, C + o, WORLD, C + o);
        }
        g.restore();

        // Stop bars, set back beyond the crosswalk so waiting traffic never
        // stands on the crossing.
        final double bn = Simulation.BAR_NEAR, bf = Simulation.BAR_FAR;
        final double bw = Simulation.BAR_WIDTH;
        g.setFill(Palette.PAINT);
        g.fillRect(C, EDGE_B + bn, HALF, bw);                  // northbound
        g.fillRect(C - HALF, EDGE_A - bf, HALF, bw);           // southbound
        g.fillRect(EDGE_A - bf, C, bw, HALF);                  // eastbound
        g.fillRect(EDGE_B + bn, C - HALF, bw, HALF);           // westbound

        // Crosswalk ladders outside each stop bar. A crossing in use is
        // painted brighter, so it is obvious where people are walking.
        drawLadder(g, 'N', true);
        drawLadder(g, 'S', true);
        drawLadder(g, 'E', false);
        drawLadder(g, 'W', false);
    }

    private void drawLadder(GraphicsContext g, char side, boolean horizontal) {
        double band = (side == 'N' || side == 'W') ? CROSS_NEAR : CROSS_FAR;
        g.save();
        g.setGlobalAlpha(sim.pedestrianWalking(side) ? 0.92 : 0.5);
        g.setFill(Palette.PAINT);
        for (int i = 0; i < 6; i++) {
            double q = EDGE_A + 4 + i * (HALF * 2 - 8) / 6.0;
            if (horizontal) {
                g.fillRect(q, band - WALK / 2, 10, WALK);
            } else {
                g.fillRect(band - WALK / 2, q, WALK, 10);
            }
        }
        g.restore();
    }

    // ---- people ---------------------------------------------------------

    private void drawPedestrians(GraphicsContext g) {
        for (Pedestrian p : sim.pedestrians()) {
            double a = p.along();
            double x, y, dx, dy;
            switch (p.side) {
                case 'N' -> { x = a; y = CROSS_NEAR + p.lateral; dx = 1; dy = 0; }
                case 'S' -> { x = a; y = CROSS_FAR + p.lateral;  dx = 1; dy = 0; }
                case 'E' -> { x = CROSS_FAR + p.lateral;  y = a; dx = 0; dy = 1; }
                default  -> { x = CROSS_NEAR + p.lateral; y = a; dx = 0; dy = 1; }
            }
            drawWalker(g, x, y, dx, dy, p.gait);
        }
    }

    /**
     * A person seen from above: shoulders, head, and two limbs that swing
     * along the direction of travel while the body sways against them.
     */
    private void drawWalker(GraphicsContext g, double x, double y,
                            double dx, double dy, double gait) {
        double px = -dy, py = dx;                 // across the walking line
        double sway  = Math.sin(gait) * 1.35;
        double swing = Math.sin(gait) * 2.8;

        double cx = x + px * sway;
        double cy = y + py * sway;

        g.setFill(Color.rgb(0, 0, 0, 0.40));
        g.fillOval(cx - 4.9 + 0.9, cy - 4.9 + 1.1, 9.8, 9.8);

        g.setFill(Palette.PED_LIMB);
        g.fillOval(cx + dx * swing + px * 3.3 - 1.6,
                   cy + dy * swing + py * 3.3 - 1.6, 3.2, 3.2);
        g.fillOval(cx - dx * swing - px * 3.3 - 1.6,
                   cy - dy * swing - py * 3.3 - 1.6, 3.2, 3.2);

        g.setFill(Palette.PED_BODY);
        g.fillOval(cx - 4.75, cy - 4.75, 9.5, 9.5);

        g.setFill(Palette.PED_HEAD);
        g.fillOval(cx - 3.0, cy - 3.0, 6.0, 6.0);
    }

    // ---- vehicles -------------------------------------------------------

    /** @see Simulation#place */
    public static double[] place(char dir, double t, int lane) {
        return Simulation.place(dir, t, lane);
    }

    private void drawVehicle(GraphicsContext g, Vehicle v) {
        double[] p = Simulation.place(v);
        double len = v.length, wid = v.width;

        g.save();
        g.translate(p[0], p[1]);
        g.rotate(p[2]);

        g.setFill(Color.rgb(0, 0, 0, 0.34));
        g.fillRoundRect(-len / 2 + 2, -wid / 2 + 2.5, len, wid, 7, 7);

        g.setFill(v.colour);
        g.fillRoundRect(-len / 2, -wid / 2, len, wid, 7, 7);

        // Windscreen band, so the direction of travel reads at a glance.
        g.setFill(Color.rgb(24, 30, 36, 0.5));
        g.fillRoundRect(len / 2 - len * 0.42, -wid / 2 + 2.5, len * 0.24, wid - 5, 4, 4);

        if (v.braking()) {
            g.setFill(Color.rgb(229, 72, 77, 0.92));
            g.fillRect(-len / 2, -wid / 2 + 1.5, 2.4, 3.4);
            g.fillRect(-len / 2, wid / 2 - 4.9, 2.4, 3.4);
        }
        g.restore();
    }

    // ---- signal heads ---------------------------------------------------

    /** Each head sits over the far side of the approach it governs. */
    /** Clear of the crossing, whose outer edge sits 40 units from the junction. */
    private static final double HEAD_OUT = 68;

    private static double[] headPos(char dir) {
        return switch (dir) {
            case 'N' -> new double[]{C + LANE * 1.5, EDGE_A - HEAD_OUT, 1};
            case 'S' -> new double[]{C - LANE * 1.5, EDGE_B + HEAD_OUT, 1};
            case 'E' -> new double[]{EDGE_B + HEAD_OUT, C + LANE * 1.5, 0};
            default  -> new double[]{EDGE_A - HEAD_OUT, C - LANE * 1.5, 0};
        };
    }

    private void drawHead(GraphicsContext g, char dir) {
        double[] p = headPos(dir);
        boolean vertical = p[2] == 1;
        String state = sim.colourOf(dir);

        double lengthways = 42, across = 17, r = 5.4;
        double w = vertical ? across : lengthways;
        double h = vertical ? lengthways : across;

        g.save();
        g.translate(p[0], p[1]);

        g.setFill(Color.rgb(0, 0, 0, 0.4));
        g.fillRoundRect(-w / 2 + 1.5, -h / 2 + 2, w, h, 8, 8);

        g.setFill(Palette.HEAD_BODY);
        g.fillRoundRect(-w / 2, -h / 2, w, h, 8, 8);
        g.setStroke(Color.rgb(255, 255, 255, 0.10));
        g.setLineWidth(1);
        g.strokeRoundRect(-w / 2, -h / 2, w, h, 8, 8);

        String[] order = {"RED", "AMBER", "GREEN"};
        for (int i = 0; i < 3; i++) {
            double o = (i - 1) * 12.6;
            double cx = vertical ? 0 : o;
            double cy = vertical ? o : 0;
            boolean lit = order[i].equals(state);
            Color tint = Palette.forState(order[i]);

            if (lit) {
                // Canvas has no blur, so the glow is a soft halo instead.
                g.setFill(Color.color(tint.getRed(), tint.getGreen(), tint.getBlue(), 0.28));
                g.fillOval(cx - r * 2.1, cy - r * 2.1, r * 4.2, r * 4.2);
            }
            g.setFill(lit ? tint : Palette.LAMP_OFF);
            g.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        g.restore();
    }

    // ---- labels ---------------------------------------------------------

    private void drawLabels(GraphicsContext g) {
        g.save();
        g.setFont(ID_FONT);
        g.setFill(Color.rgb(255, 255, 255, 0.42));

        g.setTextAlign(TextAlignment.CENTER);
        for (char d : Simulation.DIRS) {
            double[] hp = headPos(d);
            boolean vertical = hp[2] == 1;
            // Clear of the housing: half its length plus room for the text.
            double lift = vertical ? 34 : 22;
            double ty = (d == 'S') ? hp[1] + lift + 8 : hp[1] - lift;
            g.fillText(Simulation.headId(d), hp[0], ty);
        }

        // Lane numbers, counted outward from the yellow.
        g.setFont(LANE_FONT);
        g.setFill(Color.rgb(255, 255, 255, 0.30));
        g.setTextAlign(TextAlignment.CENTER);
        for (int l = 1; l <= 3; l++) {
            g.fillText(String.valueOf(l), C + LANE * (l - 0.5), WORLD - 16);
            g.fillText(String.valueOf(l), C - LANE * (l - 0.5), 20);
        }
        g.restore();
    }
}
