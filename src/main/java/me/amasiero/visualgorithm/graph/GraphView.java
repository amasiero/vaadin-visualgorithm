package me.amasiero.visualgorithm.graph;

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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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
    private final Stepper<GraphStep> stepper = null;
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

        var controls1 = new HorizontalLayout(play, pause, step, reset, generate, mode, new Span("Start: "), start);
        controls1.setAlignItems(Alignment.CENTER);
        var controls2 = new HorizontalLayout(nodeLabel, nodesRange, densityLabel, densityRange, speedLabel, speedRange);
        controls2.setAlignItems(Alignment.CENTER);

        add(controls1, controls2);
    }

    private void reload() {

    }
}
