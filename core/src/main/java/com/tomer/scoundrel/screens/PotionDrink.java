package com.tomer.scoundrel.screens;

/**
 * Drinking a potion: the card collapses into its bottle, the bottle hops to the
 * health bar, tips, and pours while the bar fills and three drops fall.
 *
 * <p>The phases must not overlap wrongly. If the bar started filling while the
 * bottle was still in the air, the drink would read as two unrelated things
 * happening at once rather than one action with a cause — so nothing reaches
 * the bar until the bottle has arrived and turned.
 *
 * <p>The bottle <b>rotates</b>, in a few discrete steps held a frame each. That
 * is safe here where it would not be in a browser: the texture is nearest
 * filtered, so a rotated draw point-samples one texel per pixel and cannot
 * blend two ramp steps into a colour that is not in the palette. The edges
 * step rather than smooth, which is the correct look.
 */
final class PotionDrink {

    private static final float FRAME = 1f / 12f;

    /** The card folding down into the bottle it contained. */
    static final int COLLAPSE_FRAMES = 2;
    /** The bottle's hops to the bar. */
    private static final int FLIGHT_FRAMES = 2;
    /**
     * Turning over, one step a frame. A single step: nothing in this art tweens,
     * so the bottle holds upright and then holds poured, and one held angle
     * reads exactly as deliberate as two while costing a frame less.
     */
    static final int TIP_STEPS = 1;
    private static final int TIP_FRAMES = TIP_STEPS;
    /** How far the bottle ends up turned, matching the reference. */
    private static final float POURED_DEGREES = -62f;
    /**
     * And pouring. The quoted 1600ms drink spent ten frames here, most of them
     * over a bar that had already finished filling — the bottle sat tipped with
     * nothing left to do. Three is enough to read as a pour; the bar's own fill
     * runs on its own clock and finishes in its own time either way.
     */
    private static final int POUR_FRAMES = 3;

    /** When the bottle is tipped far enough for anything to come out. */
    static final float POUR_START = (COLLAPSE_FRAMES + FLIGHT_FRAMES + TIP_FRAMES) * FRAME;

    /** ~667ms, quantised onto the effect grid. */
    static final float TOTAL =
            (COLLAPSE_FRAMES + FLIGHT_FRAMES + TIP_FRAMES + POUR_FRAMES) * FRAME;

    private static final int DROPS = 3;

    private PotionDrink() {
    }

    private static int frameOf(float elapsed) {
        return (int) Math.floor(elapsed / FRAME + 1e-4);
    }

    static boolean collapsing(float elapsed) {
        return elapsed >= 0f && frameOf(elapsed) < COLLAPSE_FRAMES;
    }

    static boolean flying(float elapsed) {
        int frame = frameOf(elapsed);
        return frame >= COLLAPSE_FRAMES && frame < COLLAPSE_FRAMES + FLIGHT_FRAMES;
    }

    /** How far through its hops the bottle is, 0 to 1. */
    static float flightProgress(float elapsed) {
        int frame = frameOf(elapsed) - COLLAPSE_FRAMES;
        if (frame < 0) {
            return 0f;
        }
        return Math.min(1f, frame / (float) FLIGHT_FRAMES);
    }

    /** The card's size as it folds away, as a percentage. */
    static int cardScale(float elapsed) {
        int frame = frameOf(elapsed);
        if (frame <= 0) {
            return 100;
        }
        if (frame >= COLLAPSE_FRAMES) {
            return 0;
        }
        return Math.round(100f * (COLLAPSE_FRAMES - frame) / COLLAPSE_FRAMES);
    }

    /**
     * Which turn step the bottle is on: upright until it has arrived, then one
     * step a frame. Discrete, so it holds each angle rather than sweeping
     * through them — nothing in this art tweens.
     */
    static int tiltStage(float elapsed) {
        int frame = frameOf(elapsed) - (COLLAPSE_FRAMES + FLIGHT_FRAMES);
        if (frame < 0) {
            return 0;
        }
        return Math.min(TIP_STEPS, frame + 1);
    }

    /** The bottle's angle in degrees, one of a handful of held values. */
    static float tiltDegrees(float elapsed) {
        return POURED_DEGREES * tiltStage(elapsed) / TIP_STEPS;
    }

    static boolean pouring(float elapsed) {
        return elapsed >= POUR_START && elapsed < TOTAL;
    }

    /** How many of the three drops have left the bottle. */
    static int dropsFallen(float elapsed) {
        if (elapsed < POUR_START) {
            return 0;
        }
        int frame = frameOf(elapsed) - frameOf(POUR_START);
        // Spread across the pour rather than in the first instant.
        return Math.min(DROPS, frame / Math.max(1, POUR_FRAMES / DROPS) + 1);
    }

    static boolean finished(float elapsed) {
        return elapsed >= TOTAL;
    }
}
