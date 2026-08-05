package com.tomer.scoundrel.screens;

/**
 * What the health bar does when it changes.
 *
 * <p>Damage jolts the bar sideways for two frames and reddens the number for
 * three, so the number outlasts the movement and the loss registers after the
 * jolt has stopped. It also <em>drains</em>: the fill recedes a segment a
 * frame in dried blood, the mirror of a heal growing in green, so a big hit
 * reads as a big hit rather than the bar simply being shorter next frame.
 */
final class HpPulse {

    private static final float FRAME = 1f / 12f;

    /** The jolt, on the same 4px grid the bare-handed shake uses. */
    private static final int JUMP = 4;
    private static final int JOLT_FRAMES = 2;
    private static final int BLOOD_FRAMES = 3;

    /** How long the shake and the reddened number last, drain aside. */
    static final float JOLT_TOTAL = BLOOD_FRAMES * FRAME;

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

    /**
     * How wide the fill is partway through a drain: it recedes one segment per
     * frame and stops exactly on {@code toWidth}, so a partial last segment
     * lands on the right number rather than undershooting. Never negative — a
     * killing blow empties the bar and stops there.
     */
    static int damageWidth(int fromWidth, int toWidth, float elapsed) {
        if (toWidth >= fromWidth) {
            return toWidth;
        }
        int drained = fromWidth - Math.max(0, frameOf(elapsed)) * HudArt.SEGMENT_PITCH;
        return Math.max(Math.max(toWidth, 0), drained);
    }

    /** Whether the bar is still losing ground, and so painted in blood. */
    static boolean bleeding(int fromWidth, int toWidth, float elapsed) {
        return toWidth < fromWidth && damageWidth(fromWidth, toWidth, elapsed) > toWidth;
    }

    /** Over only when both the jolt and the drain have finished. */
    static boolean damageFinished(int fromWidth, int toWidth, float elapsed) {
        return elapsed >= JOLT_TOTAL && !bleeding(fromWidth, toWidth, elapsed);
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
