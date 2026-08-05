package com.tomer.scoundrel.screens;

/**
 * Dying: a red flare over the killer, the board shaking, the screen going out
 * by ordered dither, and YOU DIED growing in.
 *
 * <p>The screen dies by <b>pattern, never alpha</b>. An alpha fade dims every
 * pixel toward black together, which reads as a dialog covering the game; an
 * ordered dither turns pixels off in a fixed order, so the board stays legible
 * through it and thins out until it is gone. That is the difference between the
 * game being obscured and the game failing.
 */
final class DeathCinematic {

    private static final float FRAME = 1f / 12f;

    /** The blow lands and flares over whatever killed you. */
    private static final int FLARE_FRAMES = 5;
    /** Then the board takes it. */
    private static final int SHAKE_FRAMES = 5;
    /** Then the screen goes out, a step of the pattern a frame. */
    private static final int DITHER_FRAMES = 10;
    /**
     * Four scale steps, each held three frames rather than one. At a frame a
     * step the title was up and finished almost before it registered; a quarter
     * of a second a step lets it arrive.
     */
    private static final int TITLE_STEPS = 4;
    private static final int TITLE_STEP_FRAMES = 3;
    private static final int HOLD_FRAMES = 8;

    /** A 4×4 ordered pattern has sixteen thresholds to cross. */
    static final int DITHER_LEVELS = 16;

    static final float DITHER_START = (FLARE_FRAMES + SHAKE_FRAMES) * FRAME;
    static final float TITLE_START = DITHER_START + DITHER_FRAMES * FRAME;
    /** ~3200ms, quantised onto the effect grid. */
    static final float TOTAL =
            TITLE_START + (TITLE_STEPS * TITLE_STEP_FRAMES + HOLD_FRAMES) * FRAME;

    /** Whole-pixel shake, one entry per frame of the shake phase. */
    private static final int[] SHAKE = {-8, 8, -4, 8, -4};

    private DeathCinematic() {
    }

    private static int frameOf(float elapsed) {
        return (int) Math.floor(elapsed / FRAME + 1e-4);
    }

    /** The red flare over the killer, before anything else happens. */
    static boolean flaring(float elapsed) {
        int frame = frameOf(elapsed);
        return frame >= 0 && frame < FLARE_FRAMES;
    }

    /** How far through the flare, so it can fade as it spreads. */
    static float flareStrength(float elapsed) {
        if (!flaring(elapsed)) {
            return 0f;
        }
        return 1f - frameOf(elapsed) / (float) FLARE_FRAMES;
    }

    static int shakeX(float elapsed) {
        int frame = frameOf(elapsed) - FLARE_FRAMES;
        if (frame < 0 || frame >= SHAKE.length) {
            return 0;
        }
        return SHAKE[frame];
    }

    /**
     * How many of the pattern's sixteen thresholds have gone dark. Climbs to
     * full and stays there — the screen never lightens again.
     */
    static int ditherLevel(float elapsed) {
        int frame = frameOf(elapsed) - (FLARE_FRAMES + SHAKE_FRAMES);
        if (frame < 0) {
            return 0;
        }
        int level = Math.round(DITHER_LEVELS * (frame + 1) / (float) DITHER_FRAMES);
        return Math.min(DITHER_LEVELS, level);
    }

    static boolean titleShowing(float elapsed) {
        return elapsed >= TITLE_START && elapsed < TOTAL;
    }

    /**
     * The title's size as a percentage of the display face, in four held steps.
     * It ends well over 100% — YOU DIED is the largest thing the game ever puts
     * on screen, and at the old range it read as a caption.
     */
    static int titleScale(float elapsed) {
        if (elapsed < TITLE_START) {
            return 0;
        }
        int frame = frameOf(elapsed) - frameOf(TITLE_START);
        int step = Math.min(TITLE_STEPS - 1, Math.max(0, frame / TITLE_STEP_FRAMES));
        return 90 + step * 70;
    }

    static boolean finished(float elapsed) {
        return elapsed >= TOTAL;
    }
}
