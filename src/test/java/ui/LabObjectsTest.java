package ui;

import config.Config;
import devices.*;
import objects.*;
import procedures.*;
import sim.SimulatedLabApi;
import java.time.LocalTime;

public final class LabObjectsTest {
    public static void main(String[] args) throws Exception {
        SimulatedLabApi api = new SimulatedLabApi();
        TrafficLights lights = new TrafficLights(LabApiViews.lights(api));
        PedInterface ped = new PedInterface(LabApiViews.ped(api));
        EmergencyDetector em = new EmergencyDetector(LabApiViews.emergency(api));
        CarDetector cars = new CarDetector(LabApiViews.cars(api));
        check(lights.isAllRed() && api.state().pedestrian() == PedLightStatus.STOP, "startup");
        for (Phase phase : Phase.values()) {
            lights.green(phase);
            check(api.state().pattern() == phase.green, "green mapping");
            rejected(lights::allRed);
            rejected(() -> lights.green(phase));
            rejected(() -> ped.setPedLight(PedLightStatus.WALK));
            lights.yellow();
            check(api.state().pattern() == phase.yellow, "yellow mapping");
            lights.allRed();
            check(lights.lastPhase() == phase, "last phase");
        }
        rejected(lights::yellow);
        ped.setPedLight(PedLightStatus.WALK);
        rejected(() -> lights.green(Phase.NS_LEFT));
        check(lights.isAllRed(), "rejected output changed object state");
        ped.setPedLight(PedLightStatus.STOP);
        api.pressPedestrian();
        check(ped.pedRequest() && ped.pedRequest(), "request must latch");
        ped.pedClearRequest();
        check(!ped.pedRequest(), "request clear");
        for (Direction direction : Direction.values()) {
            Road road = Road.from(direction);
            check(road == (direction == Direction.N || direction == Direction.S ? Road.NS : Road.EW),
                    "direction-to-road mapping");
            api.setEmergency(direction, true);
            check(em.approach() == direction && em.emergency(road), "emergency mapping");
            api.setEmergency(direction, false);
            for (Lane lane : Lane.values()) {
                api.setCar(direction, lane, true);
                check(cars.waiting(lane == Lane.L ? road.left() : road.through()), "lane mapping");
                check(!cars.waiting(lane == Lane.L ? road.through() : road.left()), "lane isolation");
                api.setCar(direction, lane, false);
            }
        }
        ModeSelection selection = new ModeSelection();
        api.pressPedestrian();
        api.setEmergency(Direction.W, true);
        check(selection.select(ped, em) == Mode.EMERGENCY, "emergency priority");
        api.setEmergency(Direction.W, false);
        check(selection.select(ped, em) == Mode.PEDESTRIAN, "pedestrian priority");
        LocalTime now = LocalTime.now();
        Mode fallback = !now.isBefore(Config.NIGHT_START) || now.isBefore(Config.NIGHT_END)
                ? Mode.NIGHT : Mode.NORMAL;
        check(selection.select(ped, em) == fallback, "no consecutive pedestrian mode");
        check(selection.select(ped, em) == Mode.PEDESTRIAN, "request remains pending");
        access(ModeSelection.class, "select", PedInterface.class, EmergencyDetector.class);
        access(NormalTrafficControl.class, "step", TrafficLights.class);
        access(NightTime.class, "step", CarDetector.class, TrafficLights.class);
        access(procedures.Pedestrian.class, "step", PedInterface.class);
        access(Emergency.class, "step", EmergencyDetector.class, TrafficLights.class);
        check(LabApi.class.getDeclaredMethods().length == 6, "lab API must have six operations");
        check(PedInterface.class.getDeclaredFields()[0].getType() == LabApiViews.Ped.class, "ped view");
        check(EmergencyDetector.class.getDeclaredFields()[0].getType() == LabApiViews.Emergency.class,
                "emergency view");
        check(CarDetector.class.getDeclaredFields()[0].getType() == LabApiViews.Cars.class, "car view");
        simulation();
        System.out.println("LabObjectsTest: guards, mappings, requests, priority, access and vehicles passed");
    }

    private static void simulation() {
        Simulation sim = new Simulation(null);
        sim.vehicles().clear();
        sim.setDensity(0);
        sim.applyLabState(TrafficPattern.ALL_RED, PedLightStatus.STOP);
        for (Direction dir : Direction.values()) {
            for (Lane lane : Lane.values()) {
                Vehicle.Maneuver turn = lane == Lane.L ? Vehicle.Maneuver.LEFT
                        : lane == Lane.C ? Vehicle.Maneuver.STRAIGHT : Vehicle.Maneuver.RIGHT;
                Vehicle car = new Vehicle(dir.name().charAt(0), lane.ordinal() + 1, 32, 110,
                        Palette.CAR_ACCENT, turn);
                car.t = Simulation.STOP_T - 50 - car.length / 2;
                sim.vehicles().add(car);
                check(sim.vehiclePresence()[dir.ordinal()][lane.ordinal()], "animated sensor mapping");
            }
        }
        for (int frame = 0; frame < 300; frame++) sim.advance(0.02);
        check(sim.vehicles().size() == 12, "disabled generation spawned a car");
        for (Vehicle car : sim.vehicles()) check(car.front() < Simulation.STOP_T, "car ran red");
        sim.applyLabState(TrafficPattern.NS_LEFT_GREEN, PedLightStatus.STOP);
        for (int frame = 0; frame < 100; frame++) sim.advance(0.02);
        for (Vehicle car : sim.vehicles()) {
            boolean permitted = (car.dir == 'N' || car.dir == 'S') && car.lane == 1;
            check((car.front() > Simulation.STOP_T) == permitted, "protected left movement");
        }
        sim.setDensity(1);
        sim.advance(0.02);
        check(sim.vehicles().size() > 12, "generation did not restart");
    }

    private static void access(Class<?> type, String name, Class<?>... inputs) throws Exception {
        check(type.getDeclaredMethod(name, inputs).getReturnType()
                == (name.equals("select") ? Mode.class : void.class), "procedure signature");
        for (var field : type.getDeclaredFields()) {
            check(field.getType() == int.class || field.getType() == Mode.class,
                    "procedure holds an extra device or API");
        }
    }

    private static void rejected(Runnable action) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError("unsafe operation accepted");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
