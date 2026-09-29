package controller;

import config.Config;
import devices.LabApi;
import devices.LabApiViews;
import javafx.application.Platform;
import objects.*;
import procedures.*;
import sim.SimulatedLabApi;
import ui.ControllerView;
import util.Runtime;
import java.util.concurrent.CountDownLatch;

public final class Controller {
    private final PedInterface ped;
    private final EmergencyDetector em;
    private final CarDetector cars;
    private final TrafficLights lights;
    private final ModeSelection selection = new ModeSelection();
    private final NormalTrafficControl normal = new NormalTrafficControl();
    private final NightTime night = new NightTime();
    private final Pedestrian pedestrian = new Pedestrian();
    private final Emergency emergency = new Emergency();

    public Controller(LabApi api) {
        lights = new TrafficLights(LabApiViews.lights(api));
        ped = new PedInterface(LabApiViews.ped(api));
        em = new EmergencyDetector(LabApiViews.emergency(api));
        cars = new CarDetector(LabApiViews.cars(api));
    }

    public Mode select() { return selection.select(ped, em); }

    public void step(Mode mode) {
        if (!lights.isAllRed()) throw new IllegalStateException("mode must start ALL_RED");
        switch (mode) {
            case NORMAL -> normal.step(lights);
            case NIGHT -> night.step(cars, lights);
            case PEDESTRIAN -> pedestrian.step(ped);
            case EMERGENCY -> emergency.step(em, lights);
        }
        if (!lights.isAllRed()) throw new IllegalStateException("mode must finish ALL_RED");
    }

    public static void main(String[] args) throws InterruptedException {
        if (!Config.USE_SIMULATOR) {
            throw new IllegalStateException("the real lab adapter has not been supplied");
        }
        SimulatedLabApi api = new SimulatedLabApi();
        Controller controller = new Controller(api);
        CountDownLatch ready = new CountDownLatch(1);
        Platform.setImplicitExit(false);
        Platform.startup(() -> {
            try {
                new ControllerView(api).show();
            } catch (RuntimeException error) {
                api.close();
                error.printStackTrace();
            } finally {
                ready.countDown();
            }
        });
        try {
            ready.await();
            Runtime.delay(Config.ALL_RED);
            while (api.isOpen() && !Thread.currentThread().isInterrupted()) {
                Mode mode = controller.select();
                api.setMode(mode.name());
                controller.step(mode);
            }
        } finally {
            Platform.exit();
        }
    }
}
