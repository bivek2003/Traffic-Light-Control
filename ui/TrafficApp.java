package ui;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The traffic display.
 *
 * <p>Shows a four-way intersection with three lanes in each direction on both
 * roads, a double yellow centre line between opposing traffic, the four signal
 * heads, people crossing when a button is pressed, and the messages every
 * signal change and detector trip puts on the wire.
 *
 * <p>All styling goes through the JavaFX API in {@link Theme}. There is no
 * stylesheet, so the display depends on nothing but JavaFX.
 */
public class TrafficApp extends Application {

    /** Room the window needs around the canvas: header, panel bars, controls. */
    private static final double CHROME = 300;
    private static final double CANVAS_MIN = 430, CANVAS_MAX = 760;
    private static final int LOG_LIMIT = 90;

    private Simulation sim;
    private IntersectionView view;
    private Theme theme = Theme.light();

    /**
     * Everything that has to be repainted when the theme changes. Each styled
     * node adds one entry, which keeps the switch to a single loop rather than
     * a list of node references to keep in step.
     */
    private final List<Runnable> restylers = new ArrayList<>();

    private final ObservableList<String> logLines = FXCollections.observableArrayList();
    private ListView<String> logList;
    private CheckBox showDetectors;

    private final Map<Character, Circle[]> lamps = new LinkedHashMap<>();
    private final Map<Character, Label> headStates = new LinkedHashMap<>();
    private final Map<Character, Button> pedButtons = new LinkedHashMap<>();

    private Label phaseLabel;
    private Button playButton;
    private Slider speed;
    private VBox root;
    private boolean running = true;

    @Override
    public void start(Stage stage) {
        sim = new Simulation(this::onMessage);

        // Fill the screen the machine actually has, rather than a fixed guess.
        double usable = Screen.getPrimary().getVisualBounds().getHeight() - CHROME;
        double canvas = Math.max(CANVAS_MIN, Math.min(CANVAS_MAX, usable));
        view = new IntersectionView(sim, canvas);

        root = new VBox(22);
        root.setPadding(new Insets(24, 22, 28, 22));
        root.getChildren().addAll(header(), body());
        style(root, () -> root.setBackground(Theme.fill(theme.ground)));

        stage.setTitle("Traffic Light Control");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();

        refreshHeads();
        refreshPedestrians();
        view.render();
        startLoop();
    }

    /** Registers a painting step and runs it once, so nodes start styled. */
    private void style(Node node, Runnable paint) {
        restylers.add(paint);
        paint.run();
    }

    private void applyTheme() {
        for (Runnable r : restylers) {
            r.run();
        }
    }

    // ---- header ---------------------------------------------------------

    private VBox header() {
        Label eyebrow = new Label("CS 460 · TRAFFIC DISPLAY");
        eyebrow.setFont(Theme.MONO_BOLD);
        style(eyebrow, () -> eyebrow.setTextFill(theme.accent));

        Label title = new Label("Traffic Light Control");
        title.setFont(Theme.TITLE);
        style(title, () -> title.setTextFill(theme.ink));

        Label sub = new Label(
                "Three lanes each way on both roads, divided by a double yellow centre line. "
                + "Vehicles queue at red and discharge on green, and every signal change and "
                + "detector trip emits the pipe-delimited message the multiplexor will carry.");
        sub.setFont(Theme.BODY);
        sub.setWrapText(true);
        sub.setMaxWidth(820);
        style(sub, () -> sub.setTextFill(theme.inkSoft));

        return new VBox(4, eyebrow, title, sub);
    }

    // ---- body -----------------------------------------------------------

    private HBox body() {
        HBox row = new HBox(22, canvasPanel(), rail());
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private VBox canvasPanel() {
        phaseLabel = new Label("-");
        phaseLabel.setFont(Theme.MONO_BOLD);
        style(phaseLabel, () -> phaseLabel.setTextFill(theme.inkSoft));

        VBox stage = new VBox(view);
        stage.setPadding(new Insets(14));
        style(stage, () -> stage.setBackground(Theme.fill(theme.surface)));

        VBox panel = new VBox(panelHead("INTERSECTION VIEW", phaseLabel), stage, controls());
        panel(panel);
        return panel;
    }

    /** The shared card treatment: a surface, a hairline, and rounded corners. */
    private void panel(Region region) {
        style(region, () -> {
            region.setBackground(Theme.fill(theme.surface, 10));
            region.setBorder(Theme.outline(theme.line, 10));
        });
    }

    private HBox controls() {
        playButton = button("Pause");
        makePrimary(playButton, true);
        playButton.setOnAction(e -> {
            running = !running;
            playButton.setText(running ? "Pause" : "Play");
            makePrimary(playButton, running);
        });

        Button step = button("Step");
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
        speedOut.setFont(Theme.MONO);
        style(speedOut, () -> speedOut.setTextFill(theme.inkSoft));
        speed.valueProperty().addListener((o, a, b) ->
                speedOut.setText(String.format("%.1fx", b.doubleValue())));

        Slider density = new Slider(0.2, 2.0, 1.0);
        density.setPrefWidth(96);
        density.valueProperty().addListener((o, a, b) -> sim.setDensity(b.doubleValue()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button themeButton = button("Theme");
        themeButton.setOnAction(e -> {
            theme = (theme.ground.equals(Theme.light().ground)) ? Theme.dark() : Theme.light();
            applyTheme();
            refreshHeads();
            refreshPedestrians();
            logList.refresh();
        });

        HBox bar = new HBox(10, playButton, step,
                fieldLabel("Speed"), speed, speedOut,
                fieldLabel("Traffic"), density,
                spacer, themeButton);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 14, 12, 14));
        style(bar, () -> {
            bar.setBackground(Theme.fill(theme.surfaceAlt));
            bar.setBorder(Theme.overline(theme.line));
        });
        return bar;
    }

    // ---- buttons --------------------------------------------------------

    private Button button(String text) {
        Button b = new Button(text);
        b.setFont(Theme.BUTTON);
        b.setPadding(new Insets(6, 13, 6, 13));
        style(b, () -> paintButton(b, false));
        b.setOnMouseEntered(e -> paintButton(b, true));
        b.setOnMouseExited(e -> paintButton(b, false));
        return b;
    }

    private void paintButton(Button b, boolean hover) {
        if (Boolean.TRUE.equals(b.getProperties().get("primary"))) {
            b.setBackground(Theme.fill(theme.accent, 7));
            b.setBorder(Theme.outline(theme.accent, 7));
            b.setTextFill(theme.surface);
            return;
        }
        Color edge = hover ? theme.accent : theme.line;
        b.setBackground(Theme.fill(theme.surface, 7));
        b.setBorder(Theme.outline(edge, 7));
        b.setTextFill(hover ? theme.accent : theme.ink);
    }

    private void makePrimary(Button b, boolean primary) {
        b.getProperties().put("primary", primary);
        paintButton(b, false);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setFont(Theme.SMALL);
        style(l, () -> l.setTextFill(theme.inkSoft));
        return l;
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
        grid.setHgap(1);
        grid.setVgap(1);
        style(grid, () -> grid.setBackground(Theme.fill(theme.line)));

        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);

        int i = 0;
        for (char d : Simulation.DIRS) {
            grid.add(headCard(d), i % 2, i / 2);
            i++;
        }

        VBox panel = new VBox(panelHead("SIGNAL HEADS", null), grid);
        panel(panel);
        return panel;
    }

    private HBox headCard(char dir) {
        Circle[] three = {new Circle(6), new Circle(6), new Circle(6)};
        lamps.put(dir, three);

        VBox housing = new VBox(3, three);
        housing.setAlignment(Pos.CENTER);
        housing.setPadding(new Insets(4));
        housing.setBackground(Theme.fill(Color.web("#191d21"), 5));

        Label id = new Label(Simulation.headId(dir));
        id.setFont(Theme.MONO_ID);
        style(id, () -> id.setTextFill(theme.ink));

        Label state = new Label("RED");
        state.setFont(Theme.MONO_BOLD);
        style(state, () -> state.setTextFill(theme.inkFaint));
        headStates.put(dir, state);

        HBox card = new HBox(11, housing, new VBox(0, id, state));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(11, 12, 11, 12));
        style(card, () -> card.setBackground(Theme.fill(theme.surface)));
        return card;
    }

    private VBox pedestrianPanel() {
        HBox row = new HBox(8);
        row.setPadding(new Insets(12, 13, 12, 13));
        for (char d : Simulation.DIRS) {
            final char dir = d;
            Button b = button(String.valueOf(d));
            b.setFont(Theme.MONO_BOLD);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPadding(new Insets(8, 4, 8, 4));
            HBox.setHgrow(b, Priority.ALWAYS);
            b.setOnAction(e -> {
                sim.pressPedestrian(dir);
                refreshPedestrians();
            });
            pedButtons.put(d, b);
            row.getChildren().add(b);
        }
        VBox panel = new VBox(panelHead("PEDESTRIAN BUTTONS", null), row);
        panel(panel);
        return panel;
    }

    private VBox logPanel() {
        logList = new ListView<>(logLines);
        logList.setPrefHeight(302);
        logList.setFocusTraversable(false);
        style(logList, () -> logList.setBackground(Theme.fill(theme.surface)));
        logList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setBackground(Background.EMPTY);
                setFont(Theme.MONO);
                setPadding(new Insets(1, 12, 1, 12));
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                setTextFill(colourFor(item));
            }
        });

        showDetectors = new CheckBox("detectors");
        showDetectors.setFont(Theme.SMALL);
        style(showDetectors, () -> showDetectors.setTextFill(theme.inkSoft));

        VBox panel = new VBox(panelHead("MESSAGE LOG", showDetectors), logList);
        panel(panel);
        return panel;
    }

    /** Message types are told apart by colour, the same as on the canvas. */
    private Color colourFor(String line) {
        int bar = line.indexOf('|');
        String type = bar > 0 ? line.substring(0, bar) : "";
        return switch (type) {
            case "COMMAND" -> theme.accent;
            case "STATE" -> theme.ink;
            case "EVENT" -> Theme.AMBER;
            case "ERROR" -> Theme.RED;
            default -> theme.inkSoft;
        };
    }

    // ---- shared bits ----------------------------------------------------

    private HBox panelHead(String text, Node trailing) {
        Label t = new Label(text);
        t.setFont(Theme.MONO_BOLD);
        style(t, () -> t.setTextFill(theme.inkSoft));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox head = new HBox(10, t, spacer);
        if (trailing != null) {
            head.getChildren().add(trailing);
        }
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(10, 14, 10, 14));
        style(head, () -> {
            head.setBackground(Theme.fill(theme.surfaceAlt));
            head.setBorder(Theme.underline(theme.line));
        });
        return head;
    }

    // ---- state refresh --------------------------------------------------

    private void refreshHeads() {
        for (char d : Simulation.DIRS) {
            String c = sim.colourOf(d);
            Circle[] three = lamps.get(d);
            setLamp(three[0], "RED", c, Theme.RED);
            setLamp(three[1], "AMBER", c, Theme.AMBER);
            setLamp(three[2], "GREEN", c, Theme.GREEN);
            headStates.get(d).setText(c);
        }
        phaseLabel.setText(sim.phaseLabel().toUpperCase());
    }

    private void setLamp(Circle lamp, String own, String current, Color lit) {
        if (own.equals(current)) {
            lamp.setFill(lit);
            lamp.setEffect(new DropShadow(9, lit));
        } else {
            lamp.setFill(Color.web("#32383e"));
            lamp.setEffect(null);
        }
    }

    private void refreshPedestrians() {
        for (char d : Simulation.DIRS) {
            Button b = pedButtons.get(d);
            if (sim.pedestrianWalking(d)) {
                b.setText("WALK");
                b.setBorder(Theme.outline(Theme.GREEN, 7));
                b.setTextFill(Theme.GREEN);
            } else if (sim.pedestrianWaiting(d)) {
                b.setText("WAIT");
                b.setBorder(Theme.outline(Theme.AMBER, 7));
                b.setTextFill(Theme.AMBER);
            } else {
                b.setText(String.valueOf(d));
                paintButton(b, false);
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
