package ui;

import java.util.ArrayList;
import java.util.List;

public final class SimulationIntegrationTest {
    private int checks;

    public static void main(String[] args) {
        new SimulationIntegrationTest().run();
    }

    private void run() {
        List<String> messages = new ArrayList<>();
        List<String> sources = new ArrayList<>();
        Simulation simulation = new Simulation((type, source, destination,
                action, value, detector) -> {
                    messages.add(action);
                    sources.add(source);
                });
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

        simulation.setDetectorChecked(true);
        simulation.vehicles().clear();
        simulation.triggerDetector('E');
        simulation.triggerDetector('E');
        simulation.triggerDetector('E');
        simulation.triggerDetector('N');
        messages.clear();
        sources.clear();
        simulation.applyDeviceState("light-east", "TRAFFIC_LIGHT", "GREEN");
        simulation.advance(0.01);
        check(hasDetectorSource(sources, "detector-east-"),
                "busiest east approach was not detected");

        simulation.setDetectorChecked(false);
        messages.clear();
        sources.clear();
        simulation.advance(0.01);
        check(messages.contains("VEHICLE_DETECTED"),
                "unchecked mode did not create random demand");

        simulation.vehicles().clear();
        simulation.applyDeviceState("light-north", "TRAFFIC_LIGHT", "RED");
        Vehicle leftTurn = new Vehicle('N', 1, 32, 110, Palette.CAR_ACCENT,
                Vehicle.Maneuver.LEFT);
        leftTurn.t = Simulation.STOP_T - 10 - leftTurn.length / 2;
        simulation.vehicles().add(leftTurn);
        double beforeTurn = leftTurn.t;
        simulation.advance(0.2);
        check(leftTurn.t > beforeTurn + 10,
                "left-turn vehicle stopped for a red light");
        leftTurn.t = Simulation.STOP_T + 60;
        double[] curved = Simulation.place(leftTurn);
        double[] straight = Simulation.place(leftTurn.dir, leftTurn.t, leftTurn.lane);
        check(Math.abs(curved[0] - straight[0]) > 1 || Math.abs(curved[1] - straight[1]) > 1,
                "left-turn vehicle did not follow a curved path");

        Vehicle rightTurn = new Vehicle('W', 3, 32, 110, Palette.CAR_ACCENT,
                Vehicle.Maneuver.RIGHT);
        rightTurn.t = Simulation.STOP_T - 10 - rightTurn.length / 2;
        simulation.vehicles().add(rightTurn);
        double beforeRightTurn = rightTurn.t;
        simulation.advance(0.2);
        check(rightTurn.t > beforeRightTurn + 10,
                "right-turn vehicle stopped for a red light");
        System.out.println("SimulationIntegrationTest: " + checks + " checks passed");
    }

    private boolean hasDetectorSource(List<String> sources, String prefix) {
        for (String source : sources) {
            if (source.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
