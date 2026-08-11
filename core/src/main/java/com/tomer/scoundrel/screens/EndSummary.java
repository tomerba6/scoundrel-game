package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Status;

import java.util.List;

/**
 * What the run-end panel says, worked out before anything is drawn.
 *
 * <p>One layout covers both outcomes — HANDOFF §11 is explicit that the death
 * variant is the same panel with the gold accents swapped to dried blood — so
 * everything that differs between a win and a death is decided here and the
 * screen draws whichever it is handed.
 */
record EndSummary(String eyebrow, String headline, int accent, int headlineColour,
                  List<Cell> cells, boolean newBest) {

    /** One of the three figures in the shared frame. */
    record Cell(String label, String value, int colour) {
    }

    /**
     * The tutorial's ending, on the same panel with its own words. It never
     * claims a best or a trophy, because nothing about it is recorded.
     */
    static EndSummary tutorial(int score, long seconds) {
        return new EndSummary("THAT IS THE WHOLE GAME", "TUTORIAL DONE",
                ScreenArt.GOLD, ScreenArt.BODY,
                List.of(
                        new Cell("SCORE", String.valueOf(score), ScreenArt.BODY),
                        new Cell("YOU GOT OUT", "YES", ScreenArt.OUTCOME_WON),
                        new Cell("TIME", ClockText.format(seconds), ScreenArt.BODY)),
                false);
    }

    /**
     * @param score     the run's final score, which for a death is negative
     * @param health    health remaining; zero or below on a death
     * @param seconds   how long the run took
     * @param newBest   whether it beat the best for its mode
     * @param healthCap the ruleset's cap, so the middle cell can be labelled honestly
     */
    static EndSummary of(Status status, int score, int health, long seconds,
                         boolean newBest, int healthCap) {
        boolean won = status == Status.WON;
        int accent = won ? ScreenArt.GOLD : ScreenArt.OUTCOME_LOST;
        // A death has no health left to report — it has a debt. Showing what the
        // dungeon still had in it is the only thing on screen that explains why
        // the score is that far below zero.
        Cell middle = won
                ? new Cell("HEALTH LEFT", String.valueOf(health), ScreenArt.OUTCOME_WON)
                : new Cell("STILL BELOW", String.valueOf(score), ScreenArt.OUTCOME_LOST);
        return new EndSummary(
                won ? "THE DUNGEON RAN OUT" : "THE DUNGEON KEPT YOU",
                won ? "CLEARED" : "YOU DIED",
                accent,
                // Cream on a win, as the render has it — the hard shadow is what
                // gives the headline weight, not colour. A death sets it in
                // blood, since a cream YOU DIED reads as a second CLEARED.
                won ? ScreenArt.BODY : ScreenArt.OUTCOME_LOST,
                List.of(
                        new Cell("SCORE", String.valueOf(score), ScreenArt.BODY),
                        middle,
                        new Cell("TIME", ClockText.format(seconds), ScreenArt.BODY)),
                newBest);
    }
}
