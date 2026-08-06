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

    /**
     * A deal aims at the card's own slot, so unlike the sweep and the carry it
     * is built per card rather than being a constant. Every anchor in a flight
     * is a <b>centre</b> — the ticker and the rail
     * included, so a deal has to be built with the slot's centre and not its
     * left edge — aiming at the edge lands the card half a card too far left,
     * which on screen reads as the row being off rather than as a bug.
     */
    @Test
    void aDealLandsExactlyOnTheSlotCentreItWasBuiltFor() {
        int centre = CardArt.slotX(1) + CardArt.CARD_W / 2;
        int middle = CardArt.SLOT_Y + CardArt.CARD_H / 2;
        CardFlight.Flight deal = CardFlight.dealTo(centre, middle);
        float end = deal.total();
        assertEquals(centre, CardFlight.x(deal, CardFlight.TICKER_X, end));
        assertEquals(middle, CardFlight.y(deal, CardFlight.TICKER_Y, end));
        assertEquals(100, CardFlight.scale(deal, end), "a dealt card ends full size");
    }

    @Test
    void aDealStartsSmallAtTheDungeon() {
        CardFlight.Flight deal = CardFlight.dealTo(452, CardArt.SLOT_Y);
        assertEquals(CardFlight.TICKER_X, CardFlight.x(deal, CardFlight.TICKER_X, 0f));
        assertTrue(CardFlight.scale(deal, 0f) < 100, "it should grow as it comes");
    }

    /**
     * A card already on the board only moves; growing it would read as the
     * room being re-dealt rather than closing up.
     */
    @Test
    void aSlideNeverChangesSize() {
        CardFlight.Flight slide = CardFlight.slideTo(252, CardArt.SLOT_Y);
        for (float t = 0; t <= slide.total(); t += 1 / 60f) {
            assertEquals(100, CardFlight.scale(slide, t), "a slide resized at t=" + t);
        }
        assertEquals(252, CardFlight.x(slide, 452, slide.total()));
    }

    /** Both arrive on the same clock, so a mixed room lands together. */
    @Test
    void aDealAndASlideRunToTheSameLength() {
        assertEquals(CardFlight.dealTo(0, 0).total(), CardFlight.slideTo(0, 0).total(), 1e-5f);
        assertEquals(CardFlight.dealTo(0, 0).staggerTime(),
                CardFlight.slideTo(0, 0).staggerTime(), 1e-5f);
    }

    @Test
    void everyDealHopIsAWholeFrame() {
        float frames = CardFlight.dealTo(0, 0).hopTime() / (1f / 12f);
        assertEquals(Math.round(frames), frames, 1e-3f);
    }
}
