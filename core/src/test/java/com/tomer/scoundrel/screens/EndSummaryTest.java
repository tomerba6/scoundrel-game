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
        EndSummary won = EndSummary.of(Status.WON, 17, 17, 252, true, 20);
        assertEquals("THE DUNGEON RAN OUT", won.eyebrow());
        assertEquals("CLEARED", won.headline());
        assertEquals(ScreenArt.GOLD, won.accent());
    }

    @Test
    void dyingSwapsEveryGoldAccentForDriedBlood() {
        EndSummary lost = EndSummary.of(Status.LOST, -63, 0, 158, false, 20);
        assertEquals("YOU DIED", lost.headline());
        assertEquals(ScreenArt.OUTCOME_LOST, lost.accent());
        assertNotEquals(ScreenArt.GOLD, lost.accent());
    }

    /** Three cells, always, so the shared frame never has a hole in it. */
    @Test
    void thereAreAlwaysExactlyThreeStatCells() {
        for (EndSummary summary : new EndSummary[] {
                EndSummary.of(Status.WON, 17, 17, 252, true, 20),
                EndSummary.of(Status.LOST, -63, 0, 158, false, 20)}) {
            assertEquals(3, summary.cells().size());
            for (EndSummary.Cell cell : summary.cells()) {
                assertFalse(cell.label().isBlank());
                assertFalse(cell.value().isBlank());
            }
        }
    }

    @Test
    void aWonRunShowsTheHealthItGotOutWith() {
        EndSummary won = EndSummary.of(Status.WON, 17, 17, 252, false, 20);
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
     * A death has no health left to report — it has a debt. The middle cell
     * shows what the dungeon still had in it, which is the whole reason the
     * score is that far below zero and is otherwise unexplained on screen.
     */
    @Test
    void aLostRunPutsTheDebtWhereTheHealthWould() {
        EndSummary lost = EndSummary.of(Status.LOST, -63, 0, 158, false, 20);
        assertEquals("-63", lost.cells().get(0).value());
        assertEquals("STILL BELOW", lost.cells().get(1).label());
        assertEquals("-63", lost.cells().get(1).value());
        assertEquals(ScreenArt.OUTCOME_LOST, lost.cells().get(1).colour());
        assertEquals("2:38", lost.cells().get(2).value());
    }

    /**
     * The headline is cream on a win, as the render has it — the hard shadow is
     * what gives it weight, not colour. A death sets it in blood, because a
     * cream YOU DIED reads as a second CLEARED at a glance.
     */
    @Test
    void theHeadlineIsCreamOnAWinAndBloodOnADeath() {
        assertEquals(ScreenArt.BODY, EndSummary.of(Status.WON, 17, 17, 252, true, 20).headlineColour());
        assertEquals(ScreenArt.OUTCOME_LOST,
                EndSummary.of(Status.LOST, -63, 0, 158, false, 20).headlineColour());
    }

    /**
     * The tutorial ends on the same panel with its own words. It never claims a
     * best or a trophy — nothing about it is recorded.
     */
    @Test
    void theTutorialEndsOnTheSamePanelWithoutClaimingAnything() {
        EndSummary done = EndSummary.tutorial(8, 95);
        assertEquals("THAT IS THE WHOLE GAME", done.eyebrow());
        assertEquals("TUTORIAL DONE", done.headline());
        assertEquals(ScreenArt.GOLD, done.accent());
        assertFalse(done.newBest(), "the tutorial is never a record");
        assertEquals(3, done.cells().size());
        assertEquals("8", done.cells().get(0).value());
        assertEquals("1:35", done.cells().get(2).value());
    }

    @Test
    void newBestOnlyShowsWhenItIsOne() {
        assertTrue(EndSummary.of(Status.WON, 17, 17, 252, true, 20).newBest());
        assertFalse(EndSummary.of(Status.WON, 17, 17, 252, false, 20).newBest());
        assertTrue(EndSummary.of(Status.LOST, -3, 0, 100, true, 20).newBest(),
                "a least-bad death is still a record");
    }

    /**
     * The one scoring edge case the game has: clearing on full health with a
     * potion last takes the score above the cap. The panel must not present that
     * as a bug by showing a score higher than the health it reports.
     */
    @Test
    void aScoreAboveTheHealthCapIsShownAsItStands() {
        EndSummary won = EndSummary.of(Status.WON, 27, 20, 300, true, 20);
        assertEquals("27", won.cells().get(0).value());
        assertEquals("20", won.cells().get(1).value());
    }
}
