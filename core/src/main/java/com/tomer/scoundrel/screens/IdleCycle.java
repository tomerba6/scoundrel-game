package com.tomer.scoundrel.screens;

import java.util.Random;

/**
 * The idle animation's clock (HANDOFF.md §7): five frames at 6 fps, looping,
 * with each card started at its own offset so four cards in a room do not
 * breathe in lockstep.
 *
 * <p>Time is <b>floored</b> to a frame rather than interpolated. Every segment
 * of this art holds on a frame — nothing tweens and nothing rotates — so the
 * quantising here is the rule, not an optimisation. It is also the idle half of
 * the single flooring point §11 step 11 asks for.
 */
final class IdleCycle {

    /** §7: six frames a second, 167ms each. Effects run at 12; idles do not. */
    static final int FPS = 6;
    static final float FRAME_TIME = 1f / FPS;
    /** One full five-frame loop, 833ms — also the range of the start stagger. */
    static final float CYCLE_TIME = 5f / FPS;

    private IdleCycle() {
    }

    /**
     * Which frame is showing at {@code elapsed} seconds for a card started at
     * {@code offset}. Wraps, and tolerates a negative elapsed time rather than
     * throwing, since a paused or rewound clock should still draw something.
     */
    static int frameIndex(float elapsed, float offset, int frameCount) {
        // Nudge before flooring. A frame boundary is not exactly representable
        // in binary — one whole cycle divided by one frame yields 4.9999998,
        // not 5 — so a bare floor sticks on the last frame for an extra tick
        // every time the cycle wraps. The epsilon is 1e-4 of a frame, about 17
        // microseconds, far below anything that can be seen or timed.
        double ticks = Math.floor((double) (elapsed + offset) / FRAME_TIME + 1e-4);
        int index = (int) (ticks % frameCount);
        return index < 0 ? index + frameCount : index;
    }

    /**
     * A start offset spanning exactly one cycle. Assigned once when a card is
     * dealt and kept — recomputing it per frame would make the card stutter
     * instead of breathe.
     */
    static float randomOffset(Random random) {
        return random.nextFloat() * CYCLE_TIME;
    }
}
