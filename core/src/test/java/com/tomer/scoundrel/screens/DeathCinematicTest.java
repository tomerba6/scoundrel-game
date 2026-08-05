package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dying: a red flare over the killer, the board shaking, the screen going out
 * by ordered dither, and YOU DIED growing in.
 *
 * <p>The dither is a <b>pattern, never an alpha fade</b> — the board stays
 * legible through it as it thins out, which is what makes the death read as the
 * screen failing rather than a dialog dimming the game behind it.
 */
class DeathCinematicTest {

    private static final float FRAME = 1f / 12f;

    @Test
    void thePhasesRunInOrder() {
        assertTrue(DeathCinematic.flaring(0f), "the blow flares first");
        assertEquals(0, DeathCinematic.ditherLevel(0f), "nothing has gone dark yet");
        assertFalse(DeathCinematic.titleShowing(0f), "and the title is not up");

        assertTrue(DeathCinematic.ditherLevel(DeathCinematic.DITHER_START) > 0);
        assertTrue(DeathCinematic.titleShowing(DeathCinematic.TITLE_START));
        assertFalse(DeathCinematic.titleShowing(DeathCinematic.TITLE_START - FRAME));
    }

    @Test
    void theBoardShakesOnAWholePixelGridThenStops() {
        boolean moved = false;
        for (float t = 0f; t < DeathCinematic.TOTAL; t += 0.004f) {
            int x = DeathCinematic.shakeX(t);
            assertEquals(0, x % 4, "shake off the 4px grid at t=" + t);
            moved |= x != 0;
        }
        assertTrue(moved, "the board never shook");
        assertEquals(0, DeathCinematic.shakeX(DeathCinematic.TOTAL), "and it settles");
    }

    /**
     * The dither climbs one step at a time to full, and never overshoots the
     * pattern it is drawn from.
     */
    @Test
    void theDitherClimbsToFullAndStops() {
        assertEquals(0, DeathCinematic.ditherLevel(0f));
        int previous = -1;
        for (float t = 0f; t <= DeathCinematic.TOTAL; t += 0.004f) {
            int level = DeathCinematic.ditherLevel(t);
            assertTrue(level >= previous, "the screen got lighter again at t=" + t);
            assertTrue(level <= DeathCinematic.DITHER_LEVELS,
                    "level " + level + " is past the pattern");
            previous = level;
        }
        assertEquals(DeathCinematic.DITHER_LEVELS, DeathCinematic.ditherLevel(DeathCinematic.TOTAL));
    }

    @Test
    void theBoardIsStillVisibleWhileTheDitherIsClimbing() {
        // Partway through, some of the pattern is on and some is off -- that is
        // what a dither is. A fade would have one value covering everything.
        float midway = DeathCinematic.DITHER_START
                + (DeathCinematic.TITLE_START - DeathCinematic.DITHER_START) / 2f;
        int level = DeathCinematic.ditherLevel(midway);
        assertTrue(level > 0 && level < DeathCinematic.DITHER_LEVELS,
                "expected a partial pattern midway, got " + level);
    }

    @Test
    void theTitleGrowsInFourSteps() {
        Set<Integer> scales = new LinkedHashSet<>();
        for (float t = DeathCinematic.TITLE_START; t < DeathCinematic.TOTAL; t += 0.004f) {
            scales.add(DeathCinematic.titleScale(t));
        }
        assertEquals(4, scales.size(), "expected four scale steps, got " + scales);
        assertTrue(DeathCinematic.titleScale(DeathCinematic.TITLE_START)
                < DeathCinematic.titleScale(DeathCinematic.TOTAL - FRAME), "it should grow");
    }

    @Test
    void everyValueHoldsForAWholeFrame() {
        for (int frame = 0; frame < 38; frame++) {
            Set<String> seen = new LinkedHashSet<>();
            for (float within = 0f; within < FRAME - 1e-4f; within += 0.004f) {
                float t = frame * FRAME + within;
                seen.add(DeathCinematic.shakeX(t) + "|" + DeathCinematic.ditherLevel(t)
                        + "|" + DeathCinematic.titleScale(t) + "|" + DeathCinematic.flaring(t));
            }
            assertEquals(1, seen.size(), "values slid within frame " + frame + ": " + seen);
        }
    }

    @Test
    void itLastsAboutThreeSeconds() {
        assertTrue(DeathCinematic.TOTAL > 2.9f && DeathCinematic.TOTAL < 3.4f,
                "expected ~3200ms, was " + DeathCinematic.TOTAL);
        assertTrue(DeathCinematic.finished(DeathCinematic.TOTAL));
        assertFalse(DeathCinematic.finished(DeathCinematic.TOTAL - 0.01f));
    }
}
