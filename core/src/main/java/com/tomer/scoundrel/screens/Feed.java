package com.tomer.scoundrel.screens;

import java.util.ArrayList;
import java.util.List;

/**
 * The event feed down the right margin: a few lines that hold, fade and go.
 *
 * <p>Pure state with no widgets, so it can be tested headlessly and so the fade
 * is something decided rather than something a toolkit happens to do. The fade
 * runs in whole steps: a continuous one puts the text on a slightly different
 * colour every frame, which at this resolution reads as the letters crawling.
 */
final class Feed {

    /** As many as the margin holds beside the room. */
    static final int MAX_LINES = 4;
    /** How long a line reads at full strength before it starts to go. */
    static final float HOLD = 4f;
    static final float FADE = 1.5f;
    /** The fade's whole steps — the only alphas a line is ever drawn at. */
    static final int FADE_STEPS = 5;

    private static final class Line {
        final String text;
        float age;

        Line(String text) {
            this.text = text;
        }
    }

    private final List<Line> lines = new ArrayList<>();

    void push(String text) {
        lines.add(new Line(text));
        while (lines.size() > MAX_LINES) {
            lines.remove(0);
        }
    }

    void update(float delta) {
        for (Line line : lines) {
            line.age += delta;
        }
        lines.removeIf(line -> line.age >= HOLD + FADE);
    }

    void clear() {
        lines.clear();
    }

    int size() {
        return lines.size();
    }

    String textAt(int index) {
        return lines.get(index).text;
    }

    /** How strongly a line is drawn: full while it holds, then down by steps. */
    float alphaAt(int index) {
        float age = lines.get(index).age;
        if (age <= HOLD) {
            return 1f;
        }
        float gone = (age - HOLD) / FADE;
        int step = Math.min(FADE_STEPS, (int) Math.ceil(gone * FADE_STEPS));
        return Math.max(0f, (FADE_STEPS - step) / (float) FADE_STEPS);
    }
}
