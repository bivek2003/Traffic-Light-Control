package ui;

import devices.Direction;
import devices.Lane;
import devices.PedLightStatus;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Screen;
import sim.SimulatedLabApi;

// JavaFX draws the shared API state. It does not change traffic-light outputs.
public final class ControllerView {
    private final SimulatedLabApi api;
    private final Simulation simulation = new Simulation(null);
    private final Label mode = new Label();
    private final Label pattern = new Label();
    private final Label pedestrian = new Label();
    private final Label request = new Label();
    private final TextArea log = new TextArea();
    private final IntersectionView intersection;
    private final Stage stage = new Stage();
    private final Label[][] sensors = new Label[4][3];
    private SimulatedLabApi.State previous;

    public ControllerView(SimulatedLabApi api) {
        this.api = api;
        simulation.applyLabState(api.state().pattern(), api.state().pedestrian());
        intersection = new IntersectionView(simulation, 600);
    }

    public void show() {
        Label title = new Label("Traffic Light Controller · Team 5");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        Label subtitle = new Label("Controller-driven signals · requests are handled between complete phases");
        subtitle.setStyle("-fx-text-fill: #526170;");
        mode.setId("controller-mode");
        pattern.setId("traffic-pattern");
        pedestrian.setId("pedestrian-status");
        pattern.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");
        VBox road = new VBox(10, pattern, intersection);
        VBox controls = new VBox(14, statusPanel(), emergencyPanel(), carPanel(), pedestrianPanel());
        controls.setPrefWidth(310);
        log.setEditable(false);
        log.setPrefRowCount(5);
        log.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        log.setId("controller-log");
        VBox root = new VBox(14, title, subtitle, new HBox(20, road, controls), log);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #edf2f6; -fx-font-family: sans-serif;");
        stage.setTitle("Team 5 · Controller and Simulation");
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        stage.setScene(new Scene(scroll, 990,
                Math.min(900, Screen.getPrimary().getVisualBounds().getHeight() - 70)));
        stage.setOnCloseRequest(event -> {
            event.consume();
            api.close();
            root.setDisable(true);
            subtitle.setText("Stopping safely after the current phase and clearance finish...");
        });
        stage.show();
        stage.centerOnScreen();
        startAnimation();
    }

    private VBox statusPanel() {
        Label explanation = new Label("Normal: fixed phases\nNight: 22:00–06:00, rotating demand\nEmergency: at most 60 s green");
        explanation.setWrapText(true);
        return panel("CONTROLLER", mode, explanation);
    }

    private VBox emergencyPanel() {
        HBox row = new HBox(16);
        for (Direction direction : Direction.values()) {
            CheckBox box = new CheckBox(direction.name());
            box.setId("emergency-" + direction);
            box.setOnAction(event -> api.setEmergency(direction, box.isSelected()));
            row.getChildren().add(box);
        }
        Label hint = new Label("Check while approaching; clear after passing.\nA request waits for the current phase to end.");
        hint.setWrapText(true);
        return panel("EMERGENCY DETECTORS", row, hint);
    }

    private VBox carPanel() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(8);
        for (Lane lane : Lane.values()) grid.add(new Label(lane.name()), lane.ordinal() + 1, 0);
        for (Direction direction : Direction.values()) {
            grid.add(new Label(direction.name()), 0, direction.ordinal() + 1);
            for (Lane lane : Lane.values()) {
                CheckBox box = new CheckBox();
                box.setId("car-" + direction + "-" + lane);
                box.setOnAction(event -> api.setCar(direction, lane, box.isSelected()));
                Label active = new Label("○");
                sensors[direction.ordinal()][lane.ordinal()] = active;
                grid.add(new HBox(3, box, active), lane.ordinal() + 1, direction.ordinal() + 1);
            }
        }
        CheckBox automatic = new CheckBox("Generate moving traffic");
        automatic.setId("automatic-traffic");
        automatic.setSelected(true);
        automatic.setOnAction(event -> simulation.setDensity(automatic.isSelected() ? 1 : 0));
        Label hint = new Label("Boxes add sensor demand; dots show actual presence.\nL needs LEFT GREEN; C/R use through green.\nNight mode rotates through waiting movements.");
        hint.setWrapText(true);
        return panel("VEHICLE SENSORS", grid, automatic, hint);
    }

    private VBox pedestrianPanel() {
        Button button = new Button("Request crossing");
        button.setId("pedestrian-request");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> api.pressPedestrian());
        return panel("PEDESTRIANS", pedestrian, request, button);
    }

    private VBox panel(String title, javafx.scene.Node... contents) {
        Label heading = new Label(title);
        heading.setStyle("-fx-font-weight: bold; -fx-text-fill: #245875;");
        VBox box = new VBox(9, heading);
        box.getChildren().addAll(contents);
        box.setPadding(new Insets(12));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        return box;
    }

    private void startAnimation() {
        new AnimationTimer() {
            private long last;

            public void handle(long now) {
                SimulatedLabApi.State state = api.state();
                simulation.applyLabState(state.pattern(), state.pedestrian());
                if (last != 0) simulation.advance(Math.min(0.05, (now - last) / 1_000_000_000.0));
                last = now;
                api.setMovingCars(simulation.vehiclePresence());
                intersection.render();
                mode.setText("Mode: " + state.mode());
                pattern.setText(state.pattern().name().replace('_', ' '));
                pedestrian.setText(state.pedestrian() == PedLightStatus.WALK ? "WALK" : "STOP");
                pedestrian.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                        + (state.pedestrian() == PedLightStatus.WALK ? "#16723c;" : "#b22c2c;"));
                request.setText(state.requested() ? "Crossing request pending" : "No pending request");
                for (Direction d : Direction.values()) {
                    for (Lane lane : Lane.values()) {
                        sensors[d.ordinal()][lane.ordinal()].setText(api.carDetection(d, lane) ? "●" : "○");
                    }
                }
                if (previous == null || !previous.events().equals(state.events())) {
                    log.setText(String.join("\n", state.events()));
                    log.positionCaret(log.getLength());
                }
                previous = state;
            }
        }.start();
    }
}
