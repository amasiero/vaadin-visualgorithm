package me.amasiero.visualgorithm.graph;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import me.amasiero.visualgorithm.core.Stepper;
import me.amasiero.visualgorithm.ui.MainLayout;
import org.w3c.dom.Document;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@PageTitle( "Graph")
@Route(value = "graph", layout = MainLayout.class)
@CssImport("./themes/visualgorithm/styles.css")
public class GraphView extends VerticalLayout {
    // Container
    private final Div canvas;

    // Graph
    private Map<Integer, List<Integer>> adj;
    private final Map<Integer, Div> nodeViews;
    private Document svgDoc;
    private Span svgEl;

    // Transversal
    private final Stepper<GraphStep> stepper;
    private final Set<Integer> visited;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> playingTask;
    private double stepsPerSecond;
    private boolean useDFS;
    private int startNode;

    private final Random rnd;

    public GraphView() {
        canvas = new Div();
        adj = new HashMap<>();
        nodeViews = new HashMap<>();
        visited = new HashSet<>();
        stepsPerSecond = 20;
        useDFS = false;
        startNode = 0;
        rnd = new Random();

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        var play = new Button("Play");
        var pause = new Button("Pause");
        var step = new Button("Step");
        var reset = new Button("Reset");
        var generate = new Button("Generate");

        var mode = new Button("Mode: BFS");
        mode.addClickListener(e -> {
            useDFS = !useDFS;
            mode.setText(useDFS ? "Mode: DFS" : "Mode: BFS");
            reload();
        });

        var start = new Select<Integer>();
        start.setWidth("100px");

        var nodeLabel = new Span("Nodes: ");
        var nodesRange = new RangeInput();
        nodesRange.setMin(4);
        nodesRange.setMax(30);
        nodesRange.setStep(1.);
        nodesRange.setValue(10.);

        var densityLabel = new Span("Density: ");
        var densityRange = new RangeInput();
        densityRange.setMin(0.05);
        densityRange.setMax(0.85);
        densityRange.setStep(0.05);
        densityRange.setValue(0.3);

        var speedLabel = new Span("Speed: ");
        var speedRange = new RangeInput();
        speedRange.setMin(5);
        speedRange.setMax(60);
        speedRange.setStep(1.);
        speedRange.setValue(stepsPerSecond);

        speedRange.getElement().addEventListener("change", e -> {
            var v = speedRange.getValue();
            stepsPerSecond = v;
            schedulePlay();
        });

        var controls1 = new HorizontalLayout(play, pause, step, reset, generate, mode, new Span("Start: "), start);
        controls1.setAlignItems(Alignment.CENTER);
        var controls2 = new HorizontalLayout(nodeLabel, nodesRange, densityLabel, densityRange, speedLabel, speedRange);
        controls2.setAlignItems(Alignment.CENTER);
        add(controls1, controls2);

        canvas.addClassName("graph-canvas");
        canvas.getStyle().set("position", "relative");
        canvas.setWidth("100%");
        canvas.setHeight("640px");
        add(canvas);

        buildRandomGraph((int) Math.round(nodesRange.getValue()), densityRange.getValue());
        drawGraph();
        refreshStartChoices(start);

        stepper = new Stepper<>(this::applyStep, this::resetColors);
        reload();

        play.addClickListener(e -> {
            stepper.play();
            schedulePlay();
        });
        pause.addClickListener(e -> stepper.pause());
        step.addClickListener(e -> stepper.step());
        reset.addClickListener(e -> {
            stepper.pause();
            visited.clear();
            resetColors();
            reload();
        });
        generate.addClickListener(e -> {
           stepper.pause();
           visited.clear();
           resetColors();
           buildRandomGraph((int) Math.round(nodesRange.getValue()), densityRange.getValue());
           drawGraph();
           refreshStartChoices(start);
           start.setValue(0);
           startNode = 0;
           reload();
        });

        start.addValueChangeListener(e -> {
           if (e.getValue() != null) {
               startNode = e.getValue();
               stepper.pause();
               visited.clear();
               resetColors();
               reload();
           }
        });
    }

    private void schedulePlay() {
        if (scheduler == null) scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "graph-play-thread");
            t.setDaemon(true);
            return t;
        });

        if (playingTask != null && !playingTask.isDone()) playingTask.cancel(false);

        var periodMs = (long) Math.max(1, 1000.0 / stepsPerSecond);
        final var ui = UI.getCurrent();
        if (ui == null) return;

        playingTask = scheduler.scheduleAtFixedRate(() -> {
            if (stepper.isPlaying()) {
                ui.access(stepper::step);
            }
        }, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    private void refreshStartChoices(Select<Integer> start) {
        start.setItems(IntStream.range(0, adj.size()).boxed().toList());
        start.setValue(0);
        startNode = 0;
    }

    private void buildRandomGraph(int n, double p) {
        adj.clear();
        IntStream.range(0, n).forEach(i -> adj.put(i, new ArrayList<>()));
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (rnd.nextDouble() < p) connect(i, j);
            }
        }
    }

    private void connect(int i, int j) {
        adj.get(i).add(j);
        adj.get(j).add(i);
    }

    private void dshMark(int u, Set<Integer> seen) {
        if (!seen.add(u)) return;
        for (int v : adj.get(u)) dshMark(v, seen);
    }

    private void drawGraph() {
        canvas.removeAll();
        nodeViews.clear();

        int n = adj.size();
        double cx = 480, cy = 300, R = Math.max(120, Math.min(260, 40 + n * 8));

        var svg = new com.vaadin.flow.component.html.Span();
        svg.getElement().setProperty("innerHTML",
                "<svg width='100%' height='100%' style='position:absolute;left:0;top:0'>" +
                        edgesSvg(n, cx, cy, R) + "</svg>");
        svg.getStyle().set("position", "absolute");
        svg.getStyle().set("left", "0"); svg.getStyle().set("top", "0");
        svg.getStyle().set("width", "100%"); svg.getStyle().set("height", "100%");
        canvas.add(svg);

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            double x = cx + R * Math.cos(angle);
            double y = cy + R * Math.sin(angle);

            var node = new Div();
            node.addClassName("node");
            node.getStyle().set("left", (x - 18) + "px");
            node.getStyle().set("top", (y - 18) + "px");
            node.setText(String.valueOf(i));
            canvas.add(node);
            nodeViews.put(i, node);
        }
    }

    private String edgesSvg(int n, double cx, double cy, double R) {
        var pos = new double[n][2];
        for (int i = 0; i < n; i++) {
            double ang = 2 * Math.PI * i / n;
            pos[i][0] = cx + R * Math.cos(ang);
            pos[i][1] = cy + R * Math.sin(ang);
        }
        var sb = new StringBuilder();
        sb.append("<g stroke='#d9d9d9' stroke-width='2'>");
        for (var e : edgesOf()) {
            double[] p1 = pos[e[0]], p2 = pos[e[1]];
            sb.append("<line x1='").append(p1[0]).append("' y1='").append(p1[1])
                    .append("' x2='").append(p2[0]).append("' y2='").append(p2[1]).append("'/>\n");
        }
        sb.append("</g>");
        return sb.toString();
    }

    private List<int[]> edgesOf() {
        var list = new ArrayList<int[]>();
        for (var en : adj.entrySet()) {
            int u = en.getKey();
            for (int v : en.getValue()) if (u < v) list.add(new int[]{u, v});
        }
        return list;
    }

    private void reload() {
        var steps = useDFS ? buildDfsSteps(startNode) : buildBfsSteps(startNode);
        stepper.load(steps);
    }

    private List<GraphStep> buildBfsSteps(int start) {
        var steps = new ArrayList<GraphStep>();
        var vis = new boolean[adj.size()];
        var q = new ArrayDeque<Integer>();
        vis[start] = true; q.add(start);
        steps.add(GraphStep.frontier(List.of(start)));

        while (!q.isEmpty()) {
            int u = q.remove();
            steps.add(GraphStep.visit(u));
            for (int v : adj.get(u)) {
                if (!vis[v]) { vis[v] = true; q.add(v); steps.add(GraphStep.frontier(new ArrayList<>(q))); }
            }
        }
        return steps;
    }

    private List<GraphStep> buildDfsSteps(int start) {
        var steps = new ArrayList<GraphStep>();
        var vis = new boolean[adj.size()];
        var stack = new ArrayDeque<Integer>();
        stack.push(start);
        steps.add(GraphStep.frontier(List.of(start)));
        while (!stack.isEmpty()) {
            int u = stack.pop();
            if (!vis[u]) {
                vis[u] = true; steps.add(GraphStep.visit(u));
                var neigh = new ArrayList<>(adj.get(u));
                Collections.reverse(neigh);
                for (int v : neigh) if (!vis[v]) stack.push(v);
                if (!stack.isEmpty()) steps.add(GraphStep.frontier(new ArrayList<>(stack)));
            }
        }
        return steps;
    }

    private void applyStep(GraphStep s) {
        switch (s.type()) {
            case VISIT -> {
                visited.add(s.node());
                nodeViews.get(s.node()).getStyle().set("background", "var(--col-visited)");
            }
            case FRONTIER -> {
                nodeViews.forEach((i, n) -> {
                    if (!visited.contains(i)) n.getStyle().set("background", "var(--col-base)");
                });
                for (int v : s.frontier()) {
                    if (!visited.contains(v)) nodeViews.get(v).getStyle().set("background", "var(--col-frontier)");
                }
            }
        }
    }
    private void resetColors() {
        visited.clear();
        nodeViews.values().forEach(n -> n.getStyle().set("background", "var(--col-base)"));
    }
}
