package me.amasiero.visualgorithm.sorting;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import me.amasiero.visualgorithm.core.Stepper;
import me.amasiero.visualgorithm.ui.MainLayout;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@PageTitle("Sorting")
@Route(value = "", layout = MainLayout.class)
@CssImport("./themes/visualgorithm/styles.css")
public class SortingView extends VerticalLayout {
    private final Random rnd;
    private int[] data;
    private Div[] bars;

    private final Set<Integer> finalized;
    private final Stepper<SortingStep> stepper;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> playingTask;
    private double stepsPerSecond;

    private final FlexLayout barsArea;

    public SortingView() {
        rnd = new Random();
        finalized = new HashSet<>();
        stepsPerSecond = 20;
        barsArea = new FlexLayout();

        data = randomArray(20, 5, 25);
        bars = createBars(data);

        stepper = new Stepper<>(this::applyStep, this::resetBars);

        var algo = new Select<>();
        algo.setItems("Bubble", "Insertion");
        algo.setValue("Bubble");

        var play = new Button("Play");
        var pause = new Button("Pause");
        var step = new Button("Step");
        var reset = new Button("Reset");
        var shuffle = new Button("Shuffle");

        var speedLabel = new Span("Speed: ");
        var speedRange = new RangeInput();
        speedRange.setMin(5);
        speedRange.setMax(60);
        speedRange.setStep(1.);
        speedRange.setValue(stepsPerSecond);

        var speedNumber = new NumberField();
        speedNumber.setMin(5.0);
        speedNumber.setMax(60.0);
        speedNumber.setStep(1.0);
        speedNumber.setValue(stepsPerSecond);

        speedRange.getElement().addEventListener("change", e -> {
            var v = Double.parseDouble(speedRange.getElement().getProperty("value"));
            stepsPerSecond = v;
            speedNumber.setValue(v);
            schedulePlay();
        });

        speedNumber.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                double v = Math.max(5.0, Math.min(60.0, e.getValue()));
                stepsPerSecond = v;
                speedRange.getElement().setProperty("value", String.valueOf((int) v));
                schedulePlay();
            }
        });

        play.addClickListener(e -> { stepper.play(); schedulePlay(); });
        pause.addClickListener(e -> stepper.pause());
        step.addClickListener(e -> stepper.step());
        reset.addClickListener(e -> {
            stepper.pause();
            resetBars();
            stepper.load(buildStepsFor(algo.getValue().toString(), data.clone()));
        });
        shuffle.addClickListener(e -> {
            stepper.pause();
            resetBars();
            data = randomArray(20, 5, 25);
            bars = createBars(data);
            renderBars();
            stepper.load(buildStepsFor(algo.getValue().toString(), data.clone()));
        });
        algo.addValueChangeListener(e -> {
            stepper.pause();
            resetBars();
            stepper.load(buildStepsFor(e.getValue().toString(), data.clone()));
        });

        var algoLabel = new Span("Algorithm: ");
        var controls1 = new HorizontalLayout(play, pause, step, reset, shuffle, algoLabel, algo);
        controls1.setAlignItems(Alignment.CENTER);
        var controls2 = new HorizontalLayout(speedLabel, speedNumber, speedRange); // number + slider
        controls2.setAlignItems(Alignment.CENTER);

        add(controls1, controls2, barsArea);

        renderBars();
        stepper.load(buildStepsFor(algo.getValue().toString(), data.clone()));
    }

    private void schedulePlay() {
        if (scheduler == null) scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "sorting-play-thread");
            t.setDaemon(true);
            return t;
        });

        if (playingTask != null && !playingTask.isDone()) playingTask.cancel(false);

        var periodMs = (long) Math.max(1, 1000.0 / stepsPerSecond);
        final var ui = UI.getCurrent();
        if (ui == null) return;

        playingTask = scheduler.scheduleAtFixedRate(() -> {
            if (stepper.isPlaying()) {
                ui.access(stepper::step); // safely update UI
            }
        }, periodMs, periodMs, TimeUnit.MILLISECONDS);
    }

    // ----- Steps builders -----
    private List<SortingStep> buildStepsFor(String name, int[] arr) {
        return switch (name) {
            case "Insertion" -> buildStepsInsertion(arr);
            default -> buildStepsBubble(arr);
        };
    }

    private List<SortingStep> buildStepsBubble(int[] a) {
        var steps = new ArrayList<SortingStep>();
        int n = a.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            for (int j = 0; j < n - i - 1; j++) {
                steps.add(SortingStep.compare(j, j + 1));
                if (a[j] > a[j + 1]) {
                    swap(a, j, j + 1);
                    steps.add(SortingStep.swap(j, j + 1));
                    swapped = true;
                }
            }
            steps.add(SortingStep.markFinal(n - i - 1));
            if (!swapped) {
                for (int k = n - i - 2; k >= 0; k--) steps.add(SortingStep.markFinal(k));
                break;
            }
        }
        steps.add(SortingStep.markFinal(0));
        return steps;
    }

    private List<SortingStep> buildStepsInsertion(int[] a) {
        var steps = new ArrayList<SortingStep>();
        for (int i = 1; i < a.length; i++) {
            int j = i;
            while (j > 0) {
                steps.add(SortingStep.compare(j - 1, j));
                if (a[j - 1] > a[j]) {
                    swap(a, j - 1, j);
                    steps.add(SortingStep.swap(j - 1, j));
                    j--;
                } else break;
            }
        }
        for (int k = 0; k < a.length; k++) steps.add(SortingStep.markFinal(k));
        return steps;
    }

    // ----- Apply step (DOM updates) -----
    private void applyStep(SortingStep s) {
        switch (s.type()) {
            case COMPARE -> {
                for (int idx = 0; idx < bars.length; idx++) {
                    if (!finalized.contains(idx)) bars[idx].getStyle().set("background", "var(--col-base)");
                }
                if (s.i() >= 0) bars[s.i()].getStyle().set("background", "var(--col-comp)");
                if (s.j() >= 0) bars[s.j()].getStyle().set("background", "var(--col-comp)");
            }
            case SWAP -> {
                int i = s.i(), j = s.j();
                Div tmp = bars[i];
                bars[i] = bars[j];
                bars[j] = tmp;
                renderBars(); // re-append in new order
            }
            case MARK_FINAL -> {
                int k = s.i();
                finalized.add(k);
                bars[k].getStyle().set("background", "var(--col-final)");
            }
        }
    }

    private void resetBars() {
        finalized.clear();
        for (Div bar : bars) bar.getStyle().set("background", "var(--col-base)");
    }

    // ----- UI helpers -----
    private Div[] createBars(int[] values) {
        var arr = new Div[values.length];
        for (int i = 0; i < values.length; i++) {
            var bar = new Div();
            bar.addClassName("bar");
            bar.setHeight(values[i] * 8 + "px");
            arr[i] = bar;
        }
        return arr;
    }

    private void renderBars() {
        barsArea.removeAll();
        barsArea.addClassName("bars-area");
        for (Div b : bars) barsArea.add(b);
    }

    private int[] randomArray(int size, int min, int max) {
        var a = new int[size];
        for (int i = 0; i < size; i++) a[i] = rnd.nextInt(max - min + 1) + min;
        return a;
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (playingTask != null) {
            playingTask.cancel(true);
            playingTask = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
