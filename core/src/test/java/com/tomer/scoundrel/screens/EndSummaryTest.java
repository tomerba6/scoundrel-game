package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the run-end panel says, decided before anything is drawn. One layout
 * covers both outcomes — HANDOFF §11 is explicit that the death variant is the
 * same panel with the gold accents swapped to dried blood — so the only thing
 * that can go wrong here is the wording and the colours, which is exactly what a
 * test can hold and a screenshot cannot.
 */
class EndSummaryTest {

    @Test
    void clearingTheDungeonReadsAsAnAchievementNotAnEnding() {
        EndSummary won = EndSummary.of(Status.WON, 17, 17, 252, true, 20, 12);
        assertEquals("THE DUNGEON RAN OUT", won.eyebrow());
        assertEquals("CLEARED", won.headline());
        assertEquals(ScreenArt.GOLD, won.accent());
    }

    @Test
    void dyingSwapsEveryGoldAccentForDriedBlood() {
        EndSummary lost = EndSummary.of(Status.LOST, -63, 0, 158, false, 20, 12);
        assertEquals("YOU DIED", lost.headline());
        assertEquals(ScreenArt.OUTCOME_LOST, lost.accent());
        assertNotEquals(ScreenArt.GOLD, lost.accent());
    }

    /** Three cells, always, so the shared frame never has a hole in it. */
    @Test
    void thereAreAlwaysExactlyThreeStatCells() {
        for (EndSummary summary : new EndSummary[] {
                EndSummary.of(Status.WON, 17, 17, 252, true, 20, 12),
                EndSummary.of(Status.LOST, -63, 0, 158, false, 20, 12)}) {
            assertEquals(3, summary.cells().size());
            for (EndSummary.Cell cell : summary.cells()) {
                assertFalse(cell.label().isBlank());
                assertFalse(cell.value().isBlank());
            }
        }
    }

    @Test
    void aWonRunShowsTheHealthItGotOutWith() {
        EndSummary won = EndSummary.of(Status.WON, 17, 17, 252, false, 20, 12);
        assertEquals("SCORE", won.cells().get(0).label());
        assertEquals("17", won.cells().get(0).value());
        assertEquals("HEALTH LEFT", won.cells().get(1).label());
        assertEquals("17", won.cells().get(1).value());
        assertEquals(ScreenArt.OUTCOME_WON, won.cells().get(1).colour(),
                "health left is the green the render uses");
        assertEquals("TIME", won.cells().get(2).label());
        assertEquals("4:12", won.cells().get(2).value());
    }

    /**
     * A death has no health left to report, so the middle cell says what the
     * dungeon still had in it — the count of monsters still face-down, which is
     * exactly what the loss score was charged for.
     *
     * <p>It used to repeat the score there, which said nothing twice. The count
     * is the one figure that explains why the number is as far below zero as it
     * is: falling early with the deck still full is the worst score there is.
     */
    @Test
    void aLostRunCountsWhatWasStillWaitingRatherThanRepeatingTheScore() {
        EndSummary lost = EndSummary.of(Status.LOST, -63, 0, 158, false, 20, 21);
        assertEquals("-63", lost.cells().get(0).value());
        assertEquals("STILL DOWN THERE", lost.cells().get(1).label());
        assertEquals("21", lost.cells().get(1).value());
        assertNotEquals(lost.cells().get(0).value(), lost.cells().get(1).value(),
                "the panel is saying the same number twice again");
        assertEquals(ScreenArt.OUTCOME_LOST, lost.cells().get(1).colour());
        assertEquals("2:38", lost.cells().get(2).value());
    }

    /** Dying on the very last card is a different story, and reads as one. */
    @Test
    void aDeathWithAnEmptyDungeonSaysNoneWereLeft() {
        EndSummary lost = EndSummary.of(Status.LOST, -2, 0, 300, false, 20, 0);
        assertEquals("0", lost.cells().get(1).value());
    }

    /** A win still reports health, and never the monster count. */
    @Test
    void aWonRunIgnoresTheCountEntirely() {
        EndSummary won = EndSummary.of(Status.WON, 17, 17, 252, false, 20, 0);
        assertEquals("HEALTH LEFT", won.cells().get(1).label());
        assertEquals("17", won.cells().get(1).value());
    }

    /**
     * The headline is cream on a win, as the render has it — the hard shadow is
     * what gives it weight, not colour. A death sets it in blood, because a
     * cream YOU DIED reads as a second CLEARED at a glance.
     */
    @Test
    void theHeadlineIsCreamOnAWinAndBloodOnADeath() {
        assertEquals(ScreenArt.BODY, EndSummary.of(Status.WON, 17, 17, 252, true, 20, 12).headlineColour());
        assertEquals(ScreenArt.OUTCOME_LOST,
                EndSummary.of(Status.LOST, -63, 0, 158, false, 20, 12).headlineColour());
    }

    /**
     * The tutorial ends on the same panel with its own words. It never claims a
     * best or a trophy — nothing about it is recorded.
     */
    @Test
    void theTutorialEndsOnTheSamePanelWithoutClaimingAnything() {
        EndSummary done = EndSummary.tutorial(8, 8);
        assertEquals("THAT IS THE WHOLE GAME", done.eyebrow());
        assertEquals("TUTORIAL DONE", done.headline());
        assertEquals(ScreenArt.GOLD, done.accent());
        assertFalse(done.newBest(), "the tutorial is never a record");
        assertEquals(3, done.cells().size());
    }

    /**
     * It reports no time. The board hides the clock for the whole tutorial —
     * it is not a timed run — and the panel showed a TIME cell anyway, which
     * could only ever read 0:00 because nothing was recording.
     *
     * <p>What it shows instead is the worked example the last beat just taught:
     * score and health left, side by side and equal.
     */
    @Test
    void theTutorialShowsTheScoringLessonRatherThanATimeItNeverKept() {
        EndSummary done = EndSummary.tutorial(10, 10);
        for (EndSummary.Cell cell : done.cells()) {
            assertNotEquals("TIME", cell.label());
            assertNotEquals("0:00", cell.value());
        }
        assertEquals("SCORE", done.cells().get(0).label());
        assertEquals("10", done.cells().get(0).value());
        assertEquals("HEALTH LEFT", done.cells().get(1).label());
        assertEquals("10", done.cells().get(1).value(),
                "clearing scores the health you kept — the two must read the same");
        assertEquals(ScreenArt.OUTCOME_WON, done.cells().get(1).colour());
        assertEquals("CLEARED", done.cells().get(2).label());
        assertEquals("YES", done.cells().get(2).value());
    }

    @Test
    void newBestOnlyShowsWhenItIsOne() {
        assertTrue(EndSummary.of(Status.WON, 17, 17, 252, true, 20, 12).newBest());
        assertFalse(EndSummary.of(Status.WON, 17, 17, 252, false, 20, 12).newBest());
        assertTrue(EndSummary.of(Status.LOST, -3, 0, 100, true, 20, 12).newBest(),
                "a least-bad death is still a record");
    }

    /**
     * The one scoring edge case the game has: clearing on full health with a
     * potion last takes the score above the cap. The panel must not present that
     * as a bug by showing a score higher than the health it reports.
     */
    @Test
    void aScoreAboveTheHealthCapIsShownAsItStands() {
        EndSummary won = EndSummary.of(Status.WON, 27, 20, 300, true, 20, 12);
        assertEquals("27", won.cells().get(0).value());
        assertEquals("20", won.cells().get(1).value());
    }
}
