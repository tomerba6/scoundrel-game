package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cards leaving the room: swept to the depth ticker when a room is avoided, or
 * carried to the rail when a weapon is equipped. Both are the same motion — a
 * few whole-pixel hops with the card shrinking as it goes — so they share one
 * set of arithmetic and differ only in where they land and how many hops.
 */
class CardFlightTest {

    @Test
    void aFlightHopsRatherThanSliding() {
        // Each hop holds for its whole duration; there is no position between
        // one hop and the next, which is what makes it read as steps.
        Set<Integer> xs = new LinkedHashSet<>();
        for (float t = 0f; t < CardFlight.AVOID.total(); t += 0.005f) {
            xs.add(CardFlight.x(CardFlight.AVOID, 252, t));
        }
        assertEquals(CardFlight.AVOID.hops(), xs.size(),
                "expected one position per hop, got " + xs);
    }

    @Test
    void aFlightEndsOnItsAnchor() {
        float last = CardFlight.AVOID.total() - 0.001f;
        assertEquals(CardFlight.TICKER_X, CardFlight.x(CardFlight.AVOID, 252, last));
        assertEquals(CardFlight.TICKER_Y, CardFlight.y(CardFlight.AVOID, 220, last));
    }

    @Test
    void aFlightStartsWhereTheCardWas() {
        assertEquals(252, CardFlight.x(CardFlight.AVOID, 252, 0f));
        assertEquals(220, CardFlight.y(CardFlight.AVOID, 220, 0f));
    }

    @Test
    void everyPositionIsAWholePixel() {
        // Ints throughout: there is no way to express a half-pixel here, which
        // is the point.
        for (float t = 0f; t < CardFlight.EQUIP.total(); t += 0.01f) {
            int x = CardFlight.x(CardFlight.EQUIP, 452, t);
            int y = CardFlight.y(CardFlight.EQUIP, 220, t);
            assertTrue(x >= 0 && y >= 0, "position went negative at t=" + t);
        }
    }

    @Test
    void theEquipFlightShrinksThroughTheSpecifiedScales() {
        assertEquals(100, CardFlight.scale(CardFlight.EQUIP, 0f));
        assertEquals(55, CardFlight.scale(CardFlight.EQUIP, CardFlight.EQUIP.hopTime()));
        assertEquals(18, CardFlight.scale(CardFlight.EQUIP, 2 * CardFlight.EQUIP.hopTime()));
    }

    @Test
    void theAvoidSweepShrinksMonotonically() {
        int previous = 101;
        for (int hop = 0; hop < CardFlight.AVOID.hops(); hop++) {
            int scale = CardFlight.scale(CardFlight.AVOID, hop * CardFlight.AVOID.hopTime());
            assertTrue(scale < previous, "scale did not shrink at hop " + hop + ": " + scale);
            previous = scale;
        }
    }

    @Test
    void theTimingsMatchTheSpecifiedDurations() {
        assertEquals(4, CardFlight.AVOID.hops());
        assertEquals(3, CardFlight.EQUIP.hops());
        // The quoted hops -- 0.20s and 0.24s -- are 2.4 and 2.88 frames at
        // 12fps, so neither lands on the grid. Each is rounded to whole frames,
        // which is the constraint that actually matters: a hop that ended
        // mid-frame would slide instead of stepping.
        // Asserted as "the hop is a whole number of frames" rather than with a
        // modulo: float remainder returns the divisor rather than zero when the
        // division lands a hair under an integer.
        float frame = 1f / 12f;
        for (CardFlight.Flight flight : new CardFlight.Flight[] {CardFlight.AVOID, CardFlight.EQUIP}) {
            float frames = flight.hopTime() / frame;
            assertEquals(Math.round(frames), frames, 1e-3f,
                    "hop is " + frames + " frames, which would end mid-frame");
        }
        // And each stays within one frame of the duration it was tuned for.
        assertTrue(Math.abs(CardFlight.AVOID.hopTime() - 0.20f) < frame,
                "avoid hop drifted from 0.20s: " + CardFlight.AVOID.hopTime());
        assertTrue(Math.abs(CardFlight.EQUIP.hopTime() - 0.24f) < frame,
                "equip hop drifted from 0.24s: " + CardFlight.EQUIP.hopTime());
    }

    /**
     * The room empties left to right, not all at once: each card waits for the
     * one before it, so the sweep reads as a sweep.
     */
    @Test
    void theSweepLeavesOneCardAfterAnother() {
        assertTrue(CardFlight.started(CardFlight.AVOID, 0, 0f), "the leftmost card goes first");
        assertFalse(CardFlight.started(CardFlight.AVOID, 1, 0f), "its neighbour should still wait");
        assertFalse(CardFlight.started(CardFlight.AVOID, 3, 2 * CardFlight.AVOID.staggerTime()));
        assertTrue(CardFlight.started(CardFlight.AVOID, 3, 3 * CardFlight.AVOID.staggerTime()));
    }

    @Test
    void theStaggerIsAWholeFrameSoNoCardStartsMidFrame() {
        float frames = CardFlight.AVOID.staggerTime() / (1f / 12f);
        assertEquals(Math.round(frames), frames, 1e-3f);
    }

    @Test
    void theSweepIsOverOnlyWhenTheLastCardHasLanded() {
        float four = CardFlight.AVOID.totalFor(4);
        assertTrue(four > CardFlight.AVOID.total(), "four staggered cards take longer than one");
        assertEquals(CardFlight.AVOID.total() + 3 * CardFlight.AVOID.staggerTime(), four, 1e-5f);
    }

    @Test
    void aFlightFinishes() {
        assertFalse(CardFlight.AVOID.finished(CardFlight.AVOID.total() - 0.01f));
        assertTrue(CardFlight.AVOID.finished(CardFlight.AVOID.total()));
    }
}
