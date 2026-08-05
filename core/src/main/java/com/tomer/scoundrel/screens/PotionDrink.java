package com.tomer.scoundrel.screens;

/**
 * Drinking a potion: the card collapses into its bottle, the bottle hops to the
 * health bar, tips, and pours while the bar fills and three drops fall.
 *
 * <p>The phases must not overlap wrongly. If the bar started filling while the
 * bottle was still in the air, the drink would read as two unrelated things
 * happening at once rather than one action with a cause — so nothing reaches
 * the bar until the bottle has arrived and tipped.
 */
final class PotionDrink {

    private static final float FRAME = 1f / 12f;

    /** The card folding down into the bottle it contained. */
    static final int COLLAPSE_FRAMES = 2;
    /** The bottle's hops to the bar. */
    private static final int FLIGHT_FRAMES = 4;
    /** Tipping over, one lean stage a frame. */
    private static final int TIP_FRAMES = TiltMask.STAGES;
    /** And pouring, long enough for the bar to fill under it. */
    private static final int POUR_FRAMES = 10;

    /** When the bottle is tipped far enough for anything to come out. */
    static final float POUR_START = (COLLAPSE_FRAMES + FLIGHT_FRAMES + TIP_FRAMES) * FRAME;

    /** ~1600ms, quantised onto the effect grid. */
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
     * The bottle's lean, upright until it has arrived and then one stage a
     * frame. Discrete: it holds each stage rather than turning through them.
     */
    static int tiltStage(float elapsed) {
        int frame = frameOf(elapsed) - (COLLAPSE_FRAMES + FLIGHT_FRAMES);
        if (frame < 0) {
            return 0;
        }
        return Math.min(TiltMask.STAGES, frame + 1);
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
