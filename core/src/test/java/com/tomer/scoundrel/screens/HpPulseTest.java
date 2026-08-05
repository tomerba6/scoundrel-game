package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the health bar does when it changes. Damage jolts it and reddens the
 * number; healing fills it back a segment at a time rather than jumping, so a
 * big potion reads as a big potion.
 */
class HpPulseTest {

    private static final float FRAME = 1f / 12f;

    @Test
    void damageJoltsTheBarAndSettles() {
        assertEquals(-4, HpPulse.barOffset(0f));
        assertEquals(4, HpPulse.barOffset(FRAME));
        assertEquals(0, HpPulse.barOffset(2 * FRAME), "the jolt is two frames and no more");
    }

    @Test
    void theJoltIsAlwaysAWholePixel() {
        for (float t = 0f; t < 1f; t += 0.004f) {
            assertEquals(0, Math.abs(HpPulse.barOffset(t)) % 4,
                    "offset off the 4px grid at t=" + t);
        }
    }

    /** The number stays red a frame longer than the bar moves, so it registers. */
    @Test
    void theNumberRedensForThreeFrames() {
        assertTrue(HpPulse.numberBloodied(0f));
        assertTrue(HpPulse.numberBloodied(2 * FRAME));
        assertFalse(HpPulse.numberBloodied(3 * FRAME));
        assertTrue(HpPulse.numberBloodied(2 * FRAME) && HpPulse.barOffset(2 * FRAME) == 0,
                "the number should outlast the jolt");
    }

    @Test
    void aHealGrowsOneSegmentPerFrame() {
        // From 100px to 140px is four segments, so four frames.
        assertEquals(100, HpPulse.healWidth(100, 140, 0f));
        assertEquals(110, HpPulse.healWidth(100, 140, FRAME));
        assertEquals(120, HpPulse.healWidth(100, 140, 2 * FRAME));
        assertEquals(140, HpPulse.healWidth(100, 140, 4 * FRAME));
    }

    @Test
    void aHealNeverOvershootsItsTarget() {
        assertEquals(140, HpPulse.healWidth(100, 140, 99f));
        // A partial last segment still lands exactly on the target.
        assertEquals(135, HpPulse.healWidth(100, 135, 99f));
        assertEquals(135, HpPulse.healWidth(100, 135, 4 * FRAME));
    }

    @Test
    void aHealHoldsEachStepForAWholeFrame() {
        Set<Integer> widths = new LinkedHashSet<>();
        for (float t = 0f; t < 5 * FRAME; t += 0.004f) {
            widths.add(HpPulse.healWidth(100, 140, t));
        }
        assertEquals(5, widths.size(), "expected one width per frame, got " + widths);
    }

    @Test
    void aHealThatChangesNothingIsAlreadyDone() {
        assertEquals(120, HpPulse.healWidth(120, 120, 0f));
        assertTrue(HpPulse.healFinished(120, 120, 0f));
    }

    @Test
    void bothPulsesEnd() {
        assertTrue(HpPulse.damageFinished(HpPulse.DAMAGE_TOTAL));
        assertFalse(HpPulse.damageFinished(HpPulse.DAMAGE_TOTAL - 0.01f));
        assertTrue(HpPulse.healFinished(100, 140, 4 * FRAME));
        assertFalse(HpPulse.healFinished(100, 140, 3 * FRAME));
    }
}
