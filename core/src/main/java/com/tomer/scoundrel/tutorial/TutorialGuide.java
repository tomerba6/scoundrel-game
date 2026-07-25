package com.tomer.scoundrel.tutorial;

import com.tomer.scoundrel.rules.Move;

import java.util.List;

/**
 * Walks a player through a {@link TutorialScript}'s steps. It exposes the
 * current beat, gates input — only the current action step's expected move is
 * accepted — and advances on that move, or on a Next for explanation beats.
 * Pure: the screen renders it and asks it what is allowed; it never touches the
 * engine.
 */
public final class TutorialGuide {

    private final List<TutorialStep> steps;
    private int index;

    public TutorialGuide(List<TutorialStep> steps) {
        this.steps = List.copyOf(steps);
    }

    public boolean isComplete() {
        return index >= steps.size();
    }

    /** The beat now showing. Only valid while {@link #isComplete()} is false. */
    public TutorialStep current() {
        if (isComplete()) {
            throw new IllegalStateException("the tutorial has no more steps");
        }
        return steps.get(index);
    }

    /** True only when the current beat is an action step expecting exactly this move. */
    public boolean accepts(Move move) {
        return !isComplete() && current().isAction() && current().expectedMove().equals(move);
    }

    /** Advance past the current action beat once its expected move has been made. */
    public void onMoveApplied(Move move) {
        if (accepts(move)) {
            index++;
        }
    }

    /** Advance an explanation beat (the Next affordance); a no-op on action beats. */
    public void next() {
        if (!isComplete() && !current().isAction()) {
            index++;
        }
    }
}
