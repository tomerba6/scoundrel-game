package com.tomer.scoundrel.screens;

/**
 * The weapon kill's timeline: the creature holds its struck frame — brightened
 * up its ramp with the cream outline over it — and then the blade lands: the
 * card is picked up 10px, a bone slash bar crosses top-right to bottom-left,
 * and the two halves part and keep rising as they fade.
 *
 * <p>The struck frame is shared with the bare-handed exchange on purpose, so a
 * creature reads the same however it is being killed and only the outcome
 * differs. The art direction specifies an outline alone here, with the sprite
 * otherwise untouched; holding the full struck frame reads as one blow rather
 * than two unrelated effects.
 *
 * <p>The ordering is the effect. The flash has to finish before anything is
 * drawn over the card, and the trap is subtle: an element that exists early and
 * is merely transparent still occludes the creature. So this class answers
 * <em>whether a thing may be drawn at all</em> rather than handing out an alpha,
 * and callers draw nothing while it says no.
 *
 * <p>Everything steps on a 12fps grid and every offset is a whole pixel. There
 * is deliberately no rotation anywhere in this API — the halves slide apart
 * instead of turning, because a turned pixel is a blurred pixel.
 */
final class WeaponKill {

    /** One effect frame. Effects run at 12fps; idles run at 6. */
    static final float FRAME = 1f / 12f;

    /**
     * The flash holds this long before the blade lands. The art direction
     * quotes 0.36s, but 0.36 x 12 is 4.32 frames — not a frame boundary — so
     * holding it literally would step the phase change mid-frame and make the
     * effect slide. Quantised to the 4 frames it was chosen to land on, which
     * is 27ms shorter and indistinguishable.
     */
    static final float RIM_TIME = 4 * FRAME;

    private static final float SLASH_START = RIM_TIME + FRAME;
    private static final float SLASH_TIME = 2 * FRAME;
    private static final float HALVES_START = RIM_TIME + 2 * FRAME;
    private static final float HALVES_STEPS = 3;

    /** Lift, slash, then three frames of parting: 5 frames after the flash. */
    static final float TOTAL = RIM_TIME + 5 * FRAME;

    private static final int LIFT_PX = 10;
    // Where each half has drifted to by the end, from the reference mock.
    private static final int UPPER_END_X = -24;
    private static final int UPPER_END_Y = -34;
    private static final int LOWER_END_X = 24;
    private static final int LOWER_END_Y = -18;
    private static final int SLASH_TRAVEL = 28;

    private WeaponKill() {
    }

    /** Floors a time onto the effect grid, so every segment holds on a frame. */
    static float quantise(float elapsed) {
        return (float) Math.floor(elapsed / FRAME + 1e-4) * FRAME;
    }

    /** The creature is holding its struck frame, still whole. */
    static boolean rimShowing(float elapsed) {
        return elapsed < RIM_TIME;
    }

    /** Whether the blade has landed. Nothing may cover the card before this. */
    static boolean cardCut(float elapsed) {
        return elapsed >= RIM_TIME;
    }

    /** The blow picks the card up on its first frame, and it stays up. */
    static int cardLift(float elapsed) {
        return cardCut(elapsed) ? LIFT_PX : 0;
    }

    static boolean slashShowing(float elapsed) {
        return elapsed >= SLASH_START && elapsed < SLASH_START + SLASH_TIME;
    }

    /** The bar's travel along its own diagonal, in whole pixels. */
    static int slashOffset(float elapsed) {
        if (!slashShowing(elapsed)) {
            return 0;
        }
        int step = (int) ((quantise(elapsed) - SLASH_START) / FRAME);
        return Math.round(SLASH_TRAVEL * (step / (float) 2) * 2 - SLASH_TRAVEL);
    }

    static boolean halvesShowing(float elapsed) {
        return elapsed >= HALVES_START && elapsed < TOTAL;
    }

    static int upperDx(float elapsed) {
        return Math.round(UPPER_END_X * progress(elapsed));
    }

    static int upperDy(float elapsed) {
        return Math.round(UPPER_END_Y * progress(elapsed)) - cardLift(elapsed);
    }

    static int lowerDx(float elapsed) {
        return Math.round(LOWER_END_X * progress(elapsed));
    }

    static int lowerDy(float elapsed) {
        return Math.round(LOWER_END_Y * progress(elapsed)) - cardLift(elapsed);
    }

    /** Full at the first parting frame, gone by the end. */
    static float halfAlpha(float elapsed) {
        return 1f - progress(elapsed);
    }

    static boolean finished(float elapsed) {
        return elapsed >= TOTAL;
    }

    /**
     * How far through the parting we are, in three discrete steps. The last one
     * lands on 1 so the halves finish fully separated and fully faded rather
     * than snapping out while still visible.
     */
    private static float progress(float elapsed) {
        if (elapsed < HALVES_START) {
            return 0f;
        }
        int step = (int) ((quantise(elapsed) - HALVES_START) / FRAME) + 1;
        return Math.min(1f, step / HALVES_STEPS);
    }
}
