package com.tomer.scoundrel.screens;

/**
 * The bare-handed exchange: two hits landing in quick succession, the creature
 * holding its struck frame throughout, the card shaking on a 4px grid and a
 * short bone flash under each blow.
 *
 * <p>The creature stays lit for the whole exchange rather than flashing once
 * per hit, so the two blows read as a single event. The shake is on a 4px grid
 * for the same reason everything else is on whole pixels — a 1px jitter at this
 * scale reads as noise rather than force.
 */
final class Barehanded {

    /** One effect frame. Effects run at 12fps. */
    static final float FRAME = 1f / 12f;

    /** ~900ms, rounded to the effect grid. */
    static final float TOTAL = 11 * FRAME;

    /** Each blow lands for two frames; the second follows one frame behind. */
    private static final int HIT_LENGTH = 2;
    private static final int[] HIT_FRAMES = {0, 1};

    /** Whole-pixel shake, one entry per frame, settling back to rest. */
    private static final int[] SHAKE_X = {-8, 8, -4, 4, -4, 0, 0, 0, 0, 0, 0};
    private static final int[] SHAKE_Y = {4, -4, 4, 0, 0, 0, 0, 0, 0, 0, 0};

    private Barehanded() {
    }

    static float quantise(float elapsed) {
        return (float) Math.floor(elapsed / FRAME + 1e-4) * FRAME;
    }

    private static int frameOf(float elapsed) {
        return (int) (quantise(elapsed) / FRAME);
    }

    /** The creature is lit for the whole exchange, not just on impact. */
    static boolean hurtShowing(float elapsed) {
        return elapsed >= 0f && elapsed < TOTAL;
    }

    /** True on the frame each blow lands, for the impact star. */
    static boolean hitLanding(float elapsed) {
        int frame = frameOf(elapsed);
        for (int start : HIT_FRAMES) {
            if (frame == start) {
                return true;
            }
        }
        return false;
    }

    /** The bone flash under a blow, two frames long. */
    static boolean flashShowing(float elapsed) {
        int frame = frameOf(elapsed);
        for (int start : HIT_FRAMES) {
            if (frame >= start && frame < start + HIT_LENGTH) {
                return true;
            }
        }
        return false;
    }

    static int shakeX(float elapsed) {
        return shake(SHAKE_X, elapsed);
    }

    static int shakeY(float elapsed) {
        return shake(SHAKE_Y, elapsed);
    }

    private static int shake(int[] table, float elapsed) {
        int frame = frameOf(elapsed);
        return frame >= 0 && frame < table.length ? table[frame] : 0;
    }

    static boolean finished(float elapsed) {
        return elapsed >= TOTAL;
    }
}
