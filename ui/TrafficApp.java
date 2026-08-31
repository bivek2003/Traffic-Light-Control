package ui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The traffic display.
 *
 * <p>Shows a four-way intersection with three lanes in each direction on both
 * roads, a double yellow centre line between opposing traffic, the four signal
 * heads, and the messages every signal change and detector trip puts on the
 * wire.
 */
public class TrafficApp extends Application {

    /** Room the window needs around the canvas: header, panel bars, controls. */
    private static final double CHROME = 300;
    private static final double CANVAS_MIN = 430, CANVAS_MAX = 760;
    private static final int LOG_LIMIT = 90;

    private Simulation sim;
    private IntersectionView view;

    private final ObservableList<String> logLines = FXCollections.observableArrayList();
    private ListView<String> logList;
    private CheckBox showDetectors;

    private final Map<Character, Circle[]> lamps = new LinkedHashMap<>();
    private final Map<Character, Label> headStates = new LinkedHashMap<>();
    private final Map<Character, Button> pedButtons = new LinkedHashMap<>();

    private Label phaseLabel;
    private Button playButton;
    private Slider speed;
    private boolean running = true;

    @Override
    public void start(Stage stage) {
        sim = new Simulation(this::onMessage);
        // Fill the screen the machine actually has, rather than a fixed guess.
        double usable = Screen.getPrimary().getVisualBounds().getHeight() - CHROME;
        double canvas = Math.max(CANVAS_MIN, Math.min(CANVAS_MAX, usable));
        view = new IntersectionView(sim, canvas);

        VBox root = new VBox(22);
        root.getStyleClass().add("app");
        root.setPadding(new Insets(24, 22, 28, 22));
        root.getChildren().addAll(header(), body());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                TrafficApp.class.getResource("style.css").toExternalForm());

        stage.setTitle("Traffic Light Control");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();

        refreshHeads();
        refreshPedestrians();
        view.render();
        startLoop();
    }

    // ---- header ---------------------------------------------------------

    private VBox header() {
        Label eyebrow = new Label("CS 460 · TRAFFIC DISPLAY");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Traffic Light Control");
        title.getStyleClass().add("title");

        Label sub = new Label(
                "Three lanes each way on both roads, divided by a double yellow centre line. "
                + "Vehicles queue at red and discharge on green, and every signal change and "
                + "detector trip emits the pipe-delimited message the multiplexor will carry.");
        sub.getStyleClass().add("subtitle");
        sub.setWrapText(true);
        sub.setMaxWidth(820);

        VBox box = new VBox(4, eyebrow, title, sub);
        box.getStyleClass().add("header");
        return box;
    }

    // ---- body -----------------------------------------------------------

    private HBox body() {
        HBox row = new HBox(22, canvasPanel(), rail());
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private VBox canvasPanel() {
        phaseLabel = new Label("-");
        phaseLabel.getStyleClass().add("panel-title");

        VBox stage = new VBox(view);
        stage.getStyleClass().add("stage");

        VBox panel = new VBox(
                panelHead("INTERSECTION VIEW", phaseLabel),
                stage,
                controls());
        panel.getStyleClass().add("panel");
        return panel;
    }

    private HBox controls() {
        playButton = new Button("Pause");
        playButton.getStyleClass().add("primary");
        playButton.setOnAction(e -> {
            running = !running;
            playButton.setText(running ? "Pause" : "Play");
            playButton.getStyleClass().removeAll("primary");
            if (running) {
                playButton.getStyleClass().add("primary");
            }
        });

        Button step = new Button("Step");
        step.setOnAction(e -> {
            sim.advance(1.0 / 12);
            afterAdvance();
        });

        speed = new Slider(0.25, 2.5, 1.0);
        speed.setBlockIncrement(0.25);
        speed.setMajorTickUnit(0.25);
        speed.setSnapToTicks(true);
        speed.setPrefWidth(108);

        Label speedOut = new Label("1.0x");
        speedOut.getStyleClass().add("readout");
        speed.valueProperty().addListener((o, a, b) ->
                speedOut.setText(String.format("%.1fx", b.doubleValue())));

        Slider density = new Slider(0.2, 2.0, 1.0);
        density.setPrefWidth(96);
        density.valueProperty().addListener((o, a, b) -> sim.setDensity(b.doubleValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button theme = new Button("Theme");
        theme.setOnAction(e -> {
            VBox root = (VBox) view.getScene().getRoot();
            if (root.getStyleClass().contains("dark")) {
                root.getStyleClass().remove("dark");
            } else {
                root.getStyleClass().add("dark");
            }
        });

        HBox bar = new HBox(10, playButton, step,
                label("Speed"), speed, speedOut,
                label("Traffic"), density,
                spacer, theme);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("controls");
        return bar;
    }

    // ---- rail -----------------------------------------------------------

    private VBox rail() {
        VBox rail = new VBox(22, headsPanel(), pedestrianPanel(), logPanel());
        rail.setPrefWidth(330);
        rail.setMinWidth(310);
        return rail;
    }

    private VBox headsPanel() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("heads");
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);

        int i = 0;
        for (char d : Simulation.DIRS) {
            grid.add(headCard(d), i % 2, i / 2);
            i++;
        }

        VBox panel = new VBox(panelHead("SIGNAL HEADS", null), grid);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private HBox headCard(char dir) {
        Circle[] three = {new Circle(6), new Circle(6), new Circle(6)};
        for (Circle c : three) {
            c.getStyleClass().add("lamp");
        }
        lamps.put(dir, three);

        VBox housing = new VBox(3, three);
        housing.getStyleClass().add("lamps");
        housing.setAlignment(Pos.CENTER);

        Label id = new Label(Simulation.headId(dir));
        id.getStyleClass().add("head-id");

        Label state = new Label("RED");
        state.getStyleClass().add("head-state");
        headStates.put(dir, state);

        HBox card = new HBox(11, housing, new VBox(0, id, state));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("head");
        return card;
    }

    private VBox pedestrianPanel() {
        HBox row = new HBox(8);
        row.getStyleClass().add("peds");
        for (char d : Simulation.DIRS) {
            final char dir = d;
            Button b = new Button(String.valueOf(d));
            b.getStyleClass().add("ped");
            b.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setOnAction(e -> {
                sim.pressPedestrian(dir);
                refreshPedestrians();
            });
            pedButtons.put(d, b);
            row.getChildren().add(b);
        }
        VBox panel = new VBox(panelHead("PEDESTRIAN BUTTONS", null), row);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private VBox logPanel() {
        logList = new ListView<>(logLines);
        logList.getStyleClass().add("log");
        logList.setPrefHeight(302);
        logList.setFocusTraversable(false);
        logList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("t-COMMAND", "t-STATE", "t-EVENT", "t-ERROR");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                int bar = item.indexOf('|');
                if (bar > 0) {
                    getStyleClass().add("t-" + item.substring(0, bar));
                }
            }
        });

        showDetectors = new CheckBox("detectors");
        showDetectors.getStyleClass().add("toggle");

        VBox panel = new VBox(panelHead("MESSAGE LOG", showDetectors), logList);
        panel.getStyleClass().add("panel");
        return panel;
    }

    // ---- shared bits ----------------------------------------------------

    private HBox panelHead(String text, javafx.scene.Node trailing) {
        Label t = new Label(text);
        t.getStyleClass().add("panel-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox head = new HBox(10, t, spacer);
        if (trailing != null) {
            head.getChildren().add(trailing);
        }
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("panel-head");
        return head;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("field-label");
        return l;
    }

    // ---- state refresh --------------------------------------------------

    private void refreshHeads() {
        for (char d : Simulation.DIRS) {
            String c = sim.colourOf(d);
            Circle[] three = lamps.get(d);
            setLamp(three[0], "RED", c);
            setLamp(three[1], "AMBER", c);
            setLamp(three[2], "GREEN", c);
            headStates.get(d).setText(c);
        }
        phaseLabel.setText(sim.phaseLabel().toUpperCase());
    }

    private void setLamp(Circle lamp, String own, String current) {
        lamp.getStyleClass().removeAll("on-red", "on-amber", "on-green");
        if (own.equals(current)) {
            lamp.getStyleClass().add("on-" + own.toLowerCase());
        }
    }

    private void refreshPedestrians() {
        for (char d : Simulation.DIRS) {
            Button b = pedButtons.get(d);
            b.getStyleClass().removeAll("waiting", "walking");
            if (sim.pedestrianWalking(d)) {
                b.setText("WALK");
                b.getStyleClass().add("walking");
            } else if (sim.pedestrianWaiting(d)) {
                b.setText("WAIT");
                b.getStyleClass().add("waiting");
            } else {
                b.setText(String.valueOf(d));
            }
        }
    }

    // ---- messages -------------------------------------------------------

    private void onMessage(String type, String src, String dst,
                           String action, String value, boolean detector) {
        if (detector && (showDetectors == null || !showDetectors.isSelected())) {
            return;
        }
        logLines.add(type + "|" + src + "|" + dst + "|" + action + "|" + value);
        while (logLines.size() > LOG_LIMIT) {
            logLines.remove(0);
        }
        if (logList != null) {
            logList.scrollTo(logLines.size() - 1);
        }
    }

    // ---- loop -----------------------------------------------------------

    private void startLoop() {
        new AnimationTimer() {
            private long last = 0;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double dt = (now - last) / 1_000_000_000.0;
                last = now;
                if (dt > 0.1) {
                    dt = 0.1;
                }
                if (running) {
                    sim.advance(dt * speed.getValue());
                    afterAdvance();
                }
            }
        }.start();
    }

    private void afterAdvance() {
        refreshHeads();
        refreshPedestrians();
        view.render();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
