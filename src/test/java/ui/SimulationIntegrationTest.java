package ui;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public final class SimulationIntegrationTest {
    private int checks;

    public static void main(String[] args) {
        new SimulationIntegrationTest().run();
    }

    private void run() {
        List<String> messages = new ArrayList<>();
        Simulation simulation = new Simulation((type, source, destination,
                action, value, detector) -> messages.add(action));
        simulation.useDeviceControl();
        simulation.applyDeviceState("light-north", "TRAFFIC_LIGHT", "YELLOW");
        check("AMBER".equals(simulation.colourOf('N')),
                "YELLOW was not shown as AMBER");
        simulation.advance(20);
        check("AMBER".equals(simulation.colourOf('N')),
                "local cycle changed a controller state");
        simulation.applyDeviceState("light-north", "TRAFFIC_LIGHT", "GREEN");
        simulation.pressPedestrian('N');
        check(messages.contains("PEDESTRIAN_REQUEST"),
                "pedestrian request was not emitted");
        simulation.applyDeviceState("button-north", "PEDESTRIAN_BUTTON", "REQUESTED");
        simulation.applyDeviceState("button-north", "PEDESTRIAN_BUTTON", "IDLE");
        check(!simulation.pedestrianWalking('N'),
                "pedestrian crossed while the light was green");
        simulation.applyDeviceState("light-north", "TRAFFIC_LIGHT", "RED");
        check(simulation.pedestrianWalking('N'),
                "served pedestrian request did not start on red");
        simulation.vehicles().clear();
        Vehicle vehicle = new Vehicle('E', 2, 30, 100, Color.BLUE);
        vehicle.t = Simulation.STOP_T - 50 - vehicle.length / 2;
        simulation.vehicles().add(vehicle);
        simulation.applyDeviceState("light-east", "TRAFFIC_LIGHT", "GREEN");
        simulation.advance(0.01);
        check(messages.contains("VEHICLE_DETECTED"),
                "vehicle detection was not emitted");
        vehicle.t = Simulation.STOP_T + 21 - vehicle.length / 2;
        simulation.advance(0.01);
        check(messages.contains("VEHICLE_CLEARED"),
                "vehicle clear was not emitted");
        System.out.println("SimulationIntegrationTest: " + checks + " checks passed");
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
