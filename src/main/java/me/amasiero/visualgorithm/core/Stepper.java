package me.amasiero.visualgorithm.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Stepper<T> {
    private final List<T> steps;
    private int index;
    private boolean playing;

    private final Consumer<T> applyStep;
    private final Runnable onReset;

    public Stepper(Consumer<T> applyStep, Runnable onReset) {
        this.steps = new ArrayList<>();
        this.index = 0;
        this.playing = false;

        this.applyStep = applyStep;
        this.onReset = onReset;
    }

    public void load(List<T> newSteps) {
        this.steps.clear();
        this.steps.addAll(newSteps);
        reset();
    }

    public void step() {
        if (index < steps.size()) {
            applyStep.accept(steps.get(index++));
        }
    }

    public void play() { playing = true; }
    public void pause() { playing = false; }
    public boolean isPlaying() { return playing; }

    public void reset() {
        pause();
        index = 0;
        onReset.run();
    }

}
