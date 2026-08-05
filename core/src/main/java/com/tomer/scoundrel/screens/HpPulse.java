package com.tomer.scoundrel.screens;

/**
 * What the health bar does when it changes.
 *
 * <p>Damage jolts the bar sideways for two frames and reddens the number for
 * three, so the number outlasts the movement and the loss registers after the
 * jolt has stopped. Healing grows the fill a segment at a time rather than
 * jumping to its new length, which is what makes a big potion look like one.
 */
final class HpPulse {

    private static final float FRAME = 1f / 12f;

    /** The jolt, on the same 4px grid the bare-handed shake uses. */
    private static final int JUMP = 4;
    private static final int JOLT_FRAMES = 2;
    private static final int BLOOD_FRAMES = 3;

    static final float DAMAGE_TOTAL = BLOOD_FRAMES * FRAME;

    private HpPulse() {
    }

    private static int frameOf(float elapsed) {
        return (int) Math.floor(elapsed / FRAME + 1e-4);
    }

    /** Sideways displacement of the whole bar, alternating so it reads as a hit. */
    static int barOffset(float elapsed) {
        int frame = frameOf(elapsed);
        if (frame < 0 || frame >= JOLT_FRAMES) {
            return 0;
        }
        return frame % 2 == 0 ? -JUMP : JUMP;
    }

    /** Whether the health number is showing in dried blood rather than bone. */
    static boolean numberBloodied(float elapsed) {
        int frame = frameOf(elapsed);
        return frame >= 0 && frame < BLOOD_FRAMES;
    }

    static boolean damageFinished(float elapsed) {
        return elapsed >= DAMAGE_TOTAL;
    }

    /**
     * How wide the fill is partway through a heal: it grows one segment per
     * frame from {@code fromWidth} and stops exactly on {@code toWidth}, so a
     * partial last segment still lands on the right number rather than
     * overshooting and snapping back.
     */
    static int healWidth(int fromWidth, int toWidth, float elapsed) {
        if (toWidth <= fromWidth) {
            return toWidth;
        }
        int grown = fromWidth + Math.max(0, frameOf(elapsed)) * HudArt.SEGMENT_PITCH;
        return Math.min(toWidth, grown);
    }

    static boolean healFinished(int fromWidth, int toWidth, float elapsed) {
        return healWidth(fromWidth, toWidth, elapsed) >= toWidth;
    }
}
