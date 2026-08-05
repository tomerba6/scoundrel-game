package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bare-handed exchange: two hits landing in quick succession while the
 * creature holds its struck frame throughout, the card shaking on a 4px grid.
 */
class BarehandedTest {

    @Test
    void theCreatureHoldsItsStruckFrameForTheWholeExchange() {
        // Not just during the hits -- it stays lit until the exchange ends, so
        // the blow reads as one event rather than two flickers.
        for (float t = 0f; t < Barehanded.TOTAL; t += 0.005f) {
            assertTrue(Barehanded.hurtShowing(t), "creature dropped its hurt frame at t=" + t);
        }
        assertFalse(Barehanded.hurtShowing(Barehanded.TOTAL));
    }

    @Test
    void thereAreExactlyTwoHitsAndTheSecondFollowsTheFirst() {
        Set<Integer> hitFrames = new LinkedHashSet<>();
        for (float t = 0f; t < Barehanded.TOTAL; t += 0.005f) {
            if (Barehanded.hitLanding(t)) {
                hitFrames.add((int) (Barehanded.quantise(t) / Barehanded.FRAME));
            }
        }
        assertEquals(2, hitFrames.size(), "expected two distinct hit frames, got " + hitFrames);
        assertEquals(Set.of(0, 3), hitFrames, "blows land on frames 0 and 3");
    }

    @Test
    void theSecondBlowLandsThreeFramesAfterTheFirst() {
        assertTrue(Barehanded.hitLanding(0f));
        assertFalse(Barehanded.hitLanding(Barehanded.FRAME));
        assertFalse(Barehanded.hitLanding(2 * Barehanded.FRAME));
        assertTrue(Barehanded.hitLanding(3 * Barehanded.FRAME), "second blow at 250ms");
    }

    @Test
    void eachStarSteppsThroughThreeDiscreteSizes() {
        Set<Integer> sizes = new LinkedHashSet<>();
        for (float t = 0f; t < Barehanded.TOTAL; t += 0.004f) {
            int size = Barehanded.starSize(0, t);
            if (size > 0) {
                sizes.add(size);
            }
        }
        // 80px box at 0.5, 1.2 and 1.9 -- never a tween between them.
        assertEquals(Set.of(40, 96, 152), sizes, "expected three discrete sizes, got " + sizes);
    }

    @Test
    void bothStarsFireAndTheSecondFollowsTheFirst() {
        assertTrue(Barehanded.starSize(0, 0f) > 0, "first star should be up at t=0");
        assertEquals(0, Barehanded.starSize(1, 0f), "second star must not exist yet");
        assertTrue(Barehanded.starSize(1, 3 * Barehanded.FRAME) > 0, "second star should fire");
    }

    @Test
    void aStarFadesAsItGrows() {
        float first = Barehanded.starAlpha(0, 0f);
        float last = Barehanded.starAlpha(0, 2 * Barehanded.FRAME);
        assertTrue(first > last, "star should fade: " + first + " -> " + last);
        assertEquals(0f, Barehanded.starAlpha(0, Barehanded.TOTAL), 1e-6f);
    }

    @Test
    void theBoneFlashRunsUnderEachHit() {
        assertTrue(Barehanded.flashShowing(0f), "no flash on the first hit");
        // Two frames each, and gone well before the exchange ends.
        assertFalse(Barehanded.flashShowing(Barehanded.TOTAL - Barehanded.FRAME));
    }

    @Test
    void theCardShakesOnAFourPixelGrid() {
        Set<Integer> xs = new LinkedHashSet<>();
        for (float t = 0f; t < Barehanded.TOTAL; t += 0.005f) {
            int x = Barehanded.shakeX(t);
            int y = Barehanded.shakeY(t);
            assertEquals(0, x % 4, "shake x off the 4px grid at t=" + t + ": " + x);
            assertEquals(0, y % 4, "shake y off the 4px grid at t=" + t + ": " + y);
            xs.add(x);
        }
        assertTrue(xs.size() > 1, "the card never actually moved");
    }

    @Test
    void theShakeSettlesBackToRest() {
        assertEquals(0, Barehanded.shakeX(Barehanded.TOTAL));
        assertEquals(0, Barehanded.shakeY(Barehanded.TOTAL));
    }

    @Test
    void everyValueHoldsForAWholeFrame() {
        for (int frame = 0; frame < 11; frame++) {
            Set<String> seen = new LinkedHashSet<>();
            for (float within = 0f; within < Barehanded.FRAME - 1e-4f; within += 0.004f) {
                float t = frame * Barehanded.FRAME + within;
                seen.add(Barehanded.shakeX(t) + "|" + Barehanded.shakeY(t) + "|"
                        + Barehanded.flashShowing(t) + "|" + Barehanded.hitLanding(t) + "|"
                        + Barehanded.starSize(0, t) + "|" + Barehanded.starAlpha(0, t) + "|"
                        + Barehanded.flashAlpha(t));
            }
            assertEquals(1, seen.size(), "values slid within frame " + frame + ": " + seen);
        }
    }

    @Test
    void theExchangeLastsAboutNineHundredMilliseconds() {
        assertTrue(Barehanded.TOTAL > 0.85f && Barehanded.TOTAL < 0.95f,
                "expected ~900ms, was " + Barehanded.TOTAL);
        assertTrue(Barehanded.finished(Barehanded.TOTAL));
        assertFalse(Barehanded.finished(Barehanded.TOTAL - 0.01f));
    }
}
