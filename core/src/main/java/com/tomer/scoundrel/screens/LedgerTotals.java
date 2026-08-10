package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Status;
import com.tomer.scoundrel.runs.RunRecord;
import com.tomer.scoundrel.runs.RunTotals;

import java.util.List;
import java.util.OptionalLong;

/**
 * The eight lifetime figures beside the table, as label/value pairs in the order
 * the panel draws them.
 *
 * <p>The reference render's own list is <b>not</b> reproduced verbatim. It asks
 * for WEAPONS BROKEN, and weapons never break in Scoundrel — they degrade and
 * stay equipped for weaker monsters, which is the rule the whole game turns on.
 * That row was drawn from placeholder data by someone not holding the rules, so
 * the panel keeps the render's shape and its eight-row rhythm and is filled with
 * figures the game actually keeps. Two of them, the best and worst score and the
 * fastest clear, are derived here rather than stored — {@link RunTotals} sums,
 * and these are extremes.
 */
final class LedgerTotals {

    /** A row of the panel: a dim label on the left, a bright value on the right. */
    record Stat(String label, String value) {
    }

    /** What a figure reads as when there is nothing to compute it from. */
    private static final String NONE = "—";

    private LedgerTotals() {
    }

    static List<Stat> of(List<RunRecord> records) {
        RunTotals totals = RunTotals.of(records);
        return List.of(
                new Stat("BEST SCORE", extreme(records, true)),
                new Stat("WORST", extreme(records, false)),
                // A count of wins says nothing without the count it came out of.
                new Stat("CLEARED", totals.wins() + " OF " + totals.runs()),
                new Stat("MONSTERS SLAIN", String.valueOf(totals.monstersDefeated())),
                new Stat("POTIONS DRUNK", String.valueOf(totals.potionsDrunk())),
                new Stat("WEAPONS EQUIPPED", String.valueOf(totals.weaponsEquipped())),
                new Stat("FASTEST CLEAR", fastestClear(records)),
                new Stat("TOTAL TIME", ClockText.format(totals.secondsPlayed())));
    }

    private static String extreme(List<RunRecord> records, boolean highest) {
        return records.stream()
                .mapToInt(RunRecord::score)
                .reduce((a, b) -> highest ? Math.max(a, b) : Math.min(a, b))
                .stream().mapToObj(String::valueOf).findFirst().orElse(NONE);
    }

    /** Only a cleared run has a clear time — a fast death is not a fast clear. */
    private static String fastestClear(List<RunRecord> records) {
        OptionalLong fastest = records.stream()
                .filter(run -> run.outcome() == Status.WON)
                .mapToLong(RunRecord::seconds)
                .min();
        return fastest.isPresent() ? ClockText.format(fastest.getAsLong()) : NONE;
    }
}
