package ui;

import devices.PedLightStatus;
import devices.TrafficPattern;
import objects.Phase;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

public final class SignalHeadTest {
    public static void main(String[] args) throws Exception {
        Simulation sim = new Simulation(null);
        check(IntersectionView.headRotation('N') == 0, "top head must span north/south lanes");
        check(IntersectionView.headRotation('S') == 180, "bottom arrow must align with its left lane");
        check(IntersectionView.headRotation('E') == 90, "east head must span east/west lanes");
        check(IntersectionView.headRotation('W') == -90, "west head must span east/west lanes");
        for (Phase phase : Phase.values()) {
            for (TrafficPattern pattern : new TrafficPattern[]{phase.green, phase.yellow,
                    TrafficPattern.ALL_RED}) {
                sim.applyLabState(pattern, PedLightStatus.STOP);
                for (char dir : Simulation.DIRS) {
                    boolean sameRoad = (dir == 'N' || dir == 'S') == (phase.road == objects.Road.NS);
                    String color = !sameRoad || pattern == TrafficPattern.ALL_RED ? "RED"
                            : pattern == phase.green ? "GREEN" : "AMBER";
                    boolean left = phase == phase.road.left();
                    check(sim.leftColourOf(dir).equals(left ? color : "RED"), "wrong arrow: " + pattern);
                    check(sim.throughColourOf(dir).equals(left ? "RED" : color), "wrong circles: " + pattern);
                }
            }
        }
        if (args.length > 0 && args[0].equals("--render")) render();
        System.out.println("SignalHeadTest: separate arrows and through lights passed for both roads");
    }

    private static void render() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                GridPane grid = new GridPane();
                grid.setPadding(new Insets(12));
                grid.setHgap(12);
                grid.setVgap(12);
                TrafficPattern[] patterns = {TrafficPattern.NS_LEFT_GREEN, TrafficPattern.EW_LEFT_GREEN,
                        TrafficPattern.NS_GREEN, TrafficPattern.EW_GREEN,
                        TrafficPattern.EW_YELLOW, TrafficPattern.ALL_RED};
                for (int i = 0; i < patterns.length; i++) {
                    Simulation sim = new Simulation(null);
                    sim.vehicles().clear();
                    sim.applyLabState(patterns[i], PedLightStatus.STOP);
                    IntersectionView view = new IntersectionView(sim, 360);
                    view.render();
                    grid.add(new VBox(6, new Label(patterns[i].name()), view), i % 2, i / 2);
                }
                var picture = new Scene(grid).snapshot(null);
                BufferedImage output = new BufferedImage((int) picture.getWidth(), (int) picture.getHeight(),
                        BufferedImage.TYPE_INT_ARGB);
                for (int y = 0; y < output.getHeight(); y++) {
                    for (int x = 0; x < output.getWidth(); x++) {
                        output.setRGB(x, y, picture.getPixelReader().getArgb(x, y));
                    }
                }
                ImageIO.write(output, "png", Path.of("out/signal-heads.png").toFile());
            } catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        });
        try {
            check(done.await(15, TimeUnit.SECONDS), "signal rendering timed out");
            if (failure.get() != null) throw new AssertionError("render failed", failure.get());
        } finally { Platform.exit(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
