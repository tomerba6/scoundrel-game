package com.tomer.scoundrel.screens;

/**
 * What the health bar does when it changes.
 *
 * <p>Damage <em>drains</em> the bar: the fill recedes a segment a frame in
 * dried blood, the mirror of a heal growing in green, so a big hit reads as a
 * big hit rather than the bar simply being shorter next frame.
 *
 * <p>The bar shakes for as long as it is losing ground. A fixed-length jolt
 * would settle while a large hit was still bleeding, leaving the bar calmly
 * draining — so the shake lasts the whole drain, with a two-frame floor so
 * even a hit that takes nothing off still registers.
 */
final class HpPulse {

    private static final float FRAME = 1f / 12f;

    /** The jolt, on the same 4px grid the bare-handed shake uses. */
    private static final int JUMP = 4;
    private static final int JOLT_FRAMES = 2;
    private static final int BLOOD_FRAMES = 3;

    /**
     * How much the fill moves in a frame, filling and draining. A heal climbs
     * three times as fast as a hit drains: the two used to share one rate, and
     * a large drink then took the best part of a second to register — long
     * after the bottle had finished pouring, so the cause and the effect came
     * apart. The drain keeps the slower rate because it is the beat the whole
     * hit is built around; the bar shakes for exactly as long as it lasts.
     */
    private static final int HEAL_STEP = 3 * HudArt.SEGMENT_PITCH;
    private static final int DRAIN_STEP = HudArt.SEGMENT_PITCH;

    /** The shortest a hit can register for, when nothing is actually lost. */
    static final float JOLT_TOTAL = BLOOD_FRAMES * FRAME;

    private HpPulse() {
    }

    private static int frameOf(float elapsed) {
        return (int) Math.floor(elapsed / FRAME + 1e-4);
    }

    /**
     * Sideways displacement of the whole bar, alternating each frame so it
     * reads as a hit rather than a lean. It runs for as long as the bar is
     * draining, so a big hit shakes throughout instead of settling early.
     */
    static int barOffset(int fromWidth, int toWidth, float elapsed) {
        int frame = frameOf(elapsed);
        if (frame < 0 || frame >= shakeFrames(fromWidth, toWidth)) {
            return 0;
        }
        return frame % 2 == 0 ? -JUMP : JUMP;
    }

    /** However long the drain takes, but never fewer than the two-frame floor. */
    private static int shakeFrames(int fromWidth, int toWidth) {
        int lost = Math.max(0, fromWidth - Math.max(toWidth, 0));
        int drainFrames = (lost + DRAIN_STEP - 1) / DRAIN_STEP;
        return Math.max(JOLT_FRAMES, drainFrames);
    }

    /**
     * Whether the health number is showing in dried blood rather than bone. It
     * holds for the whole drain, so a big hit does not read as settled while
     * the bar is still bleeding; the three-frame floor covers a hit that takes
     * nothing off.
     */
    static boolean numberBloodied(int fromWidth, int toWidth, float elapsed) {
        int frame = frameOf(elapsed);
        if (frame < 0) {
            return false;
        }
        return frame < BLOOD_FRAMES || bleeding(fromWidth, toWidth, elapsed);
    }

    /** And green for as long as a drink is still filling the bar. */
    static boolean numberHealed(int fromWidth, int toWidth, float elapsed) {
        return toWidth > fromWidth && healWidth(fromWidth, toWidth, elapsed) < toWidth;
    }

    /**
     * How wide the fill is partway through a drain: it recedes a segment per
     * frame and stops exactly on {@code toWidth}, so a partial last segment
     * lands on the right number rather than undershooting. Never negative — a
     * killing blow empties the bar and stops there.
     */
    static int damageWidth(int fromWidth, int toWidth, float elapsed) {
        if (toWidth >= fromWidth) {
            return toWidth;
        }
        int drained = fromWidth - Math.max(0, frameOf(elapsed)) * DRAIN_STEP;
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
     * How wide the fill is partway through a heal: it grows {@link #HEAL_STEP}
     * a frame from {@code fromWidth} and stops exactly on {@code toWidth}, so a
     * partial last step still lands on the right number rather than
     * overshooting and snapping back.
     */
    static int healWidth(int fromWidth, int toWidth, float elapsed) {
        if (toWidth <= fromWidth) {
            return toWidth;
        }
        int grown = fromWidth + Math.max(0, frameOf(elapsed)) * HEAL_STEP;
        return Math.min(toWidth, grown);
    }

    static boolean healFinished(int fromWidth, int toWidth, float elapsed) {
        return healWidth(fromWidth, toWidth, elapsed) >= toWidth;
    }
}
