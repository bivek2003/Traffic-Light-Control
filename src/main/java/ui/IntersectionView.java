import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Draws the original dark, six-lane intersection design. */
public final class IntersectionView extends Canvas {
    private static final double WORLD = TrafficSimulation.WORLD;
    private static final double C = TrafficSimulation.CENTER;
    private static final double LANE = TrafficSimulation.LANE;
    private static final double EDGE_A = TrafficSimulation.EDGE_A;
    private static final double EDGE_B = TrafficSimulation.EDGE_B;
    private static final Color SURROUND = Color.web("#181c20");
    private static final Color KERB = Color.web("#3a4149");
    private static final Color ASPHALT = Color.web("#24282d");
    private static final Color JUNCTION = Color.web("#2c3138");
    private static final Color PAINT = Color.web("#e9ecea");
    private static final Color YELLOW = Color.web("#efc14a");

    private final TrafficSimulation simulation;
    private final Map<String, TrafficLight> lights;
    private final double scale;

    public IntersectionView(TrafficSimulation simulation,
                            Map<String, TrafficLight> lights, double size) {
        super(size, size);
        this.simulation = simulation;
        this.lights = lights;
        this.scale = size / WORLD;
        draw();
    }

    public void draw() {
        GraphicsContext graphics = getGraphicsContext2D();
        graphics.save();
        graphics.scale(scale, scale);
        drawRoad(graphics);
        drawMarkings(graphics);
        for (Vehicle vehicle : simulation.getVehicles()) {
            drawVehicle(graphics, vehicle);
        }
        drawSignal(graphics, 'N', C + LANE * 1.5, EDGE_A - 70, true);
        drawSignal(graphics, 'S', C - LANE * 1.5, EDGE_B + 70, true);
        drawSignal(graphics, 'E', EDGE_B + 70, C + LANE * 1.5, false);
        drawSignal(graphics, 'W', EDGE_A - 70, C - LANE * 1.5, false);
        graphics.restore();
    }

    private void drawRoad(GraphicsContext graphics) {
        graphics.setFill(SURROUND);
        graphics.fillRect(0, 0, WORLD, WORLD);
        graphics.setFill(KERB);
        graphics.fillRect(EDGE_A - 4, 0, EDGE_B - EDGE_A + 8, WORLD);
        graphics.fillRect(0, EDGE_A - 4, WORLD, EDGE_B - EDGE_A + 8);
        graphics.setFill(ASPHALT);
        graphics.fillRect(EDGE_A, 0, EDGE_B - EDGE_A, WORLD);
        graphics.fillRect(0, EDGE_A, WORLD, EDGE_B - EDGE_A);
        graphics.setFill(JUNCTION);
        graphics.fillRect(EDGE_A, EDGE_A, EDGE_B - EDGE_A, EDGE_B - EDGE_A);
    }

    private void drawMarkings(GraphicsContext graphics) {
        graphics.save();
        graphics.setStroke(PAINT);
        graphics.setGlobalAlpha(0.55);
        graphics.setLineWidth(2.5);
        graphics.setLineDashes(17, 15);
        for (int lane = 1; lane <= 2; lane++) {
            for (int sign : new int[]{-1, 1}) {
                double position = C + sign * LANE * lane;
                graphics.strokeLine(position, 0, position, EDGE_A);
                graphics.strokeLine(position, EDGE_B, position, WORLD);
                graphics.strokeLine(0, position, EDGE_A, position);
                graphics.strokeLine(EDGE_B, position, WORLD, position);
            }
        }
        graphics.restore();

        graphics.setStroke(YELLOW);
        graphics.setLineWidth(2.6);
        for (double offset : new double[]{-3.2, 3.2}) {
            graphics.strokeLine(C + offset, 0, C + offset, EDGE_A);
            graphics.strokeLine(C + offset, EDGE_B, C + offset, WORLD);
            graphics.strokeLine(0, C + offset, EDGE_A, C + offset);
            graphics.strokeLine(EDGE_B, C + offset, WORLD, C + offset);
        }

        graphics.setFill(PAINT);
        graphics.fillRect(C, EDGE_A - 31, LANE * 3, 5);
        graphics.fillRect(C - LANE * 3, EDGE_B + 26, LANE * 3, 5);
        graphics.fillRect(EDGE_B + 26, C - LANE * 3, 5, LANE * 3);
        graphics.fillRect(EDGE_A - 31, C, 5, LANE * 3);

        drawCrosswalk(graphics, EDGE_A - 16, true);
        drawCrosswalk(graphics, EDGE_B + 16, true);
        drawCrosswalk(graphics, EDGE_A - 16, false);
        drawCrosswalk(graphics, EDGE_B + 16, false);

        graphics.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
        graphics.setFill(Color.rgb(255, 255, 255, 0.30));
        graphics.setTextAlign(TextAlignment.CENTER);
        for (int lane = 1; lane <= 3; lane++) {
            graphics.fillText(String.valueOf(lane), C + LANE * (lane - 0.5), WORLD - 15);
            graphics.fillText(String.valueOf(lane), C - LANE * (lane - 0.5), 20);
        }
    }

    private void drawCrosswalk(GraphicsContext graphics, double band, boolean horizontal) {
        graphics.save();
        graphics.setGlobalAlpha(0.55);
        graphics.setFill(PAINT);
        for (int stripe = 0; stripe < 8; stripe++) {
            double position = EDGE_A + 5 + stripe * (EDGE_B - EDGE_A - 10) / 8.0;
            if (horizontal) {
                graphics.fillRect(position, band - 8, 12, 16);
            } else {
                graphics.fillRect(band - 8, position, 16, 12);
            }
        }
        graphics.restore();
    }

    private void drawVehicle(GraphicsContext graphics, Vehicle vehicle) {
        graphics.save();
        graphics.translate(vehicle.x, vehicle.y);
        graphics.rotate(vehicle.heading);
        graphics.setFill(Color.rgb(0, 0, 0, 0.35));
        graphics.fillRoundRect(-vehicle.length / 2 + 2, -9, vehicle.length, 18, 7, 7);
        graphics.setFill(vehicle.color);
        graphics.fillRoundRect(-vehicle.length / 2, -11, vehicle.length, 18, 7, 7);
        graphics.setFill(Color.rgb(24, 30, 36, 0.55));
        graphics.fillRoundRect(vehicle.length / 2 - 14, -8, 8, 12, 3, 3);
        graphics.restore();
    }

    private void drawSignal(GraphicsContext graphics, char direction,
                            double x, double y, boolean vertical) {
        String id = direction == 'N' ? "light-north"
                : direction == 'S' ? "light-south"
                : direction == 'E' ? "light-east" : "light-west";
        String state = lights.get(id).getState();
        String[] colors = {"RED", "YELLOW", "GREEN"};
        double width = vertical ? 18 : 45;
        double height = vertical ? 45 : 18;
        graphics.setFill(Color.web("#14181c"));
        graphics.fillRoundRect(x - width / 2, y - height / 2, width, height, 8, 8);
        for (int index = 0; index < 3; index++) {
            double offset = (index - 1) * 13;
            double lampX = vertical ? x : x + offset;
            double lampY = vertical ? y + offset : y;
            graphics.setFill(colors[index].equals(state)
                    ? signalColor(colors[index]) : Color.web("#2b3137"));
            graphics.fillOval(lampX - 5.5, lampY - 5.5, 11, 11);
        }
        graphics.setFont(Font.font("Menlo", FontWeight.BOLD, 10));
        graphics.setFill(Color.rgb(255, 255, 255, 0.42));
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.fillText(id, x, direction == 'S' ? y + 40 : y - 30);
    }

    private Color signalColor(String color) {
        if ("GREEN".equals(color)) return Color.web("#46b26a");
        if ("YELLOW".equals(color)) return Color.web("#f0a92e");
        return Color.web("#e5484d");
    }
}
