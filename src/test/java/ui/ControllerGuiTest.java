package ui;

import controller.Controller;
import devices.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import objects.Phase;
import procedures.Mode;
import sim.SimulatedLabApi;
import util.Runtime;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;

// Runs real timed controller steps while JavaFX renders and receives button clicks.
public final class ControllerGuiTest {
    private static final SimulatedLabApi api = new SimulatedLabApi();
    private static final Controller controller = new Controller(api);
    private static final AtomicReference<Throwable> failure = new AtomicReference<>();
    private static final List<TrafficPattern> seen = new ArrayList<>();
    private static Scene scene;
    private static boolean requestDuringGreen;
    private static boolean clearEmergency;
    private static long greenStarted;
    private static TrafficPattern last = TrafficPattern.ALL_RED;

    public static void main(String[] args) throws Exception {
        Platform.setImplicitExit(false);
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            try {
                new ControllerView(api).show();
                scene = Stage.getWindows().get(0).getScene();
                click("automatic-traffic");
                new AnimationTimer() {
                    public void handle(long now) {
                        try { observe(); } catch (Throwable error) { failure.compareAndSet(null, error); }
                    }
                }.start();
            } catch (Throwable error) {
                failure.set(error);
            } finally { ready.countDown(); }
        });
        try {
            check(ready.await(10, TimeUnit.SECONDS), "GUI did not open");
            check(failure.get() == null, "GUI startup failed: " + failure.get());
            Runtime.delay(2_000);
            for (Phase phase : Phase.values()) {
                if (phase == Phase.NS_LEFT) onFx(() -> requestDuringGreen = true);
                run(Mode.NORMAL, phase.green, phase.name().contains("LEFT") ? 16_000 : 36_000);
                if (phase == Phase.NS_LEFT) {
                    check(api.pedRequest(), "button request lost during green");
                    check(controller.select() == Mode.PEDESTRIAN, "pedestrian priority");
                    run(Mode.PEDESTRIAN, null, 15_000);
                    check(!api.pedRequest(), "served request not cleared");
                    onFx(() -> click("pedestrian-request"));
                    check(controller.select() != Mode.PEDESTRIAN, "consecutive pedestrian steps");
                }
            }
            // The repeated request survives the traffic steps and is now served.
            check(controller.select() == Mode.PEDESTRIAN, "repeated request lost");
            run(Mode.PEDESTRIAN, null, 15_000);
            // Keep the through sensor active while both roads request left turns.
            onFx(() -> { click("car-E-C"); click("car-W-L"); click("car-S-L"); });
            run(Mode.NIGHT, TrafficPattern.EW_GREEN, 21_000);
            run(Mode.NIGHT, TrafficPattern.EW_LEFT_GREEN, 14_000);
            run(Mode.NIGHT, TrafficPattern.NS_LEFT_GREEN, 14_000);
            run(Mode.NIGHT, TrafficPattern.NS_GREEN, 26_000);
            onFx(() -> { click("car-E-C"); click("car-W-L"); click("car-S-L"); });
            onFx(() -> {
                click("pedestrian-request");
                click("emergency-N");
                click("emergency-E");
                clearEmergency = true;
            });
            check(controller.select() == Mode.EMERGENCY, "emergency must outrank pedestrian");
            run(Mode.EMERGENCY, TrafficPattern.NS_GREEN, 7_000);
            check(api.pedRequest(), "emergency lost pending pedestrian request");
            check(controller.select() == Mode.EMERGENCY, "second road request lost");
            run(Mode.EMERGENCY, TrafficPattern.EW_GREEN, 7_000);
            check(controller.select() == Mode.PEDESTRIAN, "pedestrian not selected after emergencies");
            run(Mode.PEDESTRIAN, null, 15_000);
            onFx(() -> { clearEmergency = false; click("emergency-S"); });
            long started = Runtime.millis();
            run(Mode.EMERGENCY, TrafficPattern.NS_GREEN, 66_000);
            check(Runtime.millis() - started < 69_000, "stuck detector exceeded hold cap");
            check(api.emergency(Direction.S), "cap must not erase a live detector");
            onFx(() -> click("emergency-S"));
            run(Mode.NORMAL, TrafficPattern.NS_LEFT_GREEN, 16_000);
            onFx(() -> {
                try { snapshot(); } catch (Exception error) { throw new RuntimeException(error); }
                ((Stage) scene.getWindow()).fireEvent(new javafx.stage.WindowEvent(
                        scene.getWindow(), javafx.stage.WindowEvent.WINDOW_CLOSE_REQUEST));
            });
            check(!api.isOpen(), "close request did not stop the controller loop");
            check(failure.get() == null, "GUI/safety check failed: " + failure.get());
            System.out.println("ControllerGuiTest: normal cycle, all night choices, pedestrian requests,");
            System.out.println("emergency priority/release/cap, GUI controls and safe close passed");
        } finally { Platform.exit(); }
    }

    private static void run(Mode mode, TrafficPattern expected, long minimum) throws Exception {
        onFx(seen::clear);
        api.setMode(mode.name());
        long started = Runtime.millis();
        controller.step(mode);
        long duration = Runtime.millis() - started;
        check(duration >= minimum, "step too short: " + mode + " " + duration);
        check(api.state().pattern() == TrafficPattern.ALL_RED, "step did not finish ALL_RED");
        check(api.state().pedestrian() == PedLightStatus.STOP, "step did not finish STOP");
        onFx(() -> {
            if (expected != null) {
                check(seen.size() == 3 && seen.get(0) == expected
                        && seen.get(2) == TrafficPattern.ALL_RED, "wrong phase sequence: " + seen);
            }
        });
        check(failure.get() == null, "GUI/safety failure: " + failure.get());
        System.out.println("PASS " + mode + " " + expected + " (" + duration + " ms)");
    }

    private static void observe() {
        SimulatedLabApi.State state = api.state();
        check(state.pedestrian() != PedLightStatus.WALK || state.pattern() == TrafficPattern.ALL_RED,
                "WALK overlapped traffic");
        if (last != state.pattern()) {
            boolean legal = false;
            for (Phase phase : Phase.values()) {
                legal |= last == TrafficPattern.ALL_RED && state.pattern() == phase.green;
                legal |= last == phase.green && state.pattern() == phase.yellow;
                legal |= last == phase.yellow && state.pattern() == TrafficPattern.ALL_RED;
            }
            check(legal, "unsafe output sequence");
            last = state.pattern();
            seen.add(last);
            if (last.name().endsWith("GREEN")) greenStarted = Runtime.millis();
        }
        if (last.name().endsWith("GREEN") && requestDuringGreen) {
            click("pedestrian-request");
            requestDuringGreen = false;
        }
        if (clearEmergency && state.mode().equals("EMERGENCY")
                && last.name().endsWith("GREEN") && Runtime.millis() - greenStarted >= 1_000) {
            String id = last == TrafficPattern.NS_GREEN ? "emergency-N" : "emergency-E";
            if (((CheckBox) scene.lookup("#" + id)).isSelected()) click(id);
        }
        check(!((Label) scene.lookup("#traffic-pattern")).getText().isBlank(), "GUI missing pattern");
    }

    private static void click(String id) {
        if (scene.lookup("#" + id) instanceof CheckBox box) box.fire();
        else ((Button) scene.lookup("#" + id)).fire();
    }

    private static void onFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { action.run(); } catch (Throwable error) { failure.compareAndSet(null, error); }
            finally { done.countDown(); }
        });
        check(done.await(5, TimeUnit.SECONDS), "JavaFX stopped responding");
        check(failure.get() == null, "JavaFX action failed: " + failure.get());
    }

    private static void snapshot() throws Exception {
        var picture = scene.snapshot(null);
        BufferedImage output = new BufferedImage((int) picture.getWidth(), (int) picture.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < output.getHeight(); y++) {
            for (int x = 0; x < output.getWidth(); x++) {
                output.setRGB(x, y, picture.getPixelReader().getArgb(x, y));
            }
        }
        ImageIO.write(output, "png", Path.of("out/controller-gui.png").toFile());
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
