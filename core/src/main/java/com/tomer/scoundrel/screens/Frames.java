package com.tomer.scoundrel.screens;

/**
 * The frame grid: the one place time is floored onto it.
 *
 * <p>Nothing in this art tweens. Every segment of every effect holds on a frame
 * and then jumps, because the alternative — asking a continuous clock how far
 * through we are — draws something a little different on every render tick, and
 * a hand-placed pixel that lands on 387 one tick and 388 the next crawls.
 * Effects hold at {@value #EFFECT_FPS} fps, idles at the calmer {@value #IDLE_FPS}.
 *
 * <p><b>Why the nudge before flooring.</b> A frame boundary is not exactly
 * representable in binary: one whole five-frame idle cycle divided by one frame
 * yields 4.9999998, not 5. A bare floor therefore sticks on the frame before the
 * boundary for one extra tick — sometimes, depending on how the elapsed time was
 * accumulated — which shows up as two effects tuned against each other drifting a
 * frame apart, and reads as a bug in whichever one you happen to be looking at.
 * {@link #EPSILON} is a ten-thousandth of a frame, about 8 microseconds at 12 fps:
 * far below anything that can be seen, timed, or reached by an accumulating clock.
 *
 * <p>The constant used to be copied into nine places, with the comment explaining
 * it beside only one of them.
 */
final class Frames {

    /** Effects: 12 fps, 83ms a frame. Chosen in HANDOFF §10 so the durations divide. */
    static final int EFFECT_FPS = 12;
    /** Idles: 6 fps, 167ms a frame. Slower on purpose — a breath, not an action. */
    static final int IDLE_FPS = 6;

    /** A ten-thousandth of a frame; see the class note for what it is for. */
    private static final double EPSILON = 1e-4;

    private Frames() {
    }

    /**
     * Which frame {@code elapsed} seconds falls in at {@code fps}.
     *
     * <p>Multiplies by the rate rather than dividing by a stored 1/12, which
     * matters over a long session: a float 1/12 is a hair above the true value,
     * and by an hour in that error has outgrown the epsilon and the boundary
     * starts landing a frame late. An integer rate has no such error.
     *
     * <p>Tolerates a negative time rather than throwing — a clock that has not
     * started, or one that has been rewound, should still draw something.
     */
    static int at(float elapsed, int fps) {
        return (int) Math.floor((double) elapsed * fps + EPSILON);
    }

    /**
     * As {@link #at}, for a hold expressed as a period rather than a rate —
     * a card flight holds each hop for a time it carries with it.
     */
    static int atPeriod(float elapsed, float period) {
        return (int) Math.floor((double) elapsed / period + EPSILON);
    }

    /**
     * The start time of the frame {@code elapsed} falls in: the same flooring,
     * for the effects whose timelines are written in seconds rather than frame
     * counts. Snapping an already-snapped time returns it unchanged.
     */
    static float snap(float elapsed, int fps) {
        return at(elapsed, fps) / (float) fps;
    }
}
