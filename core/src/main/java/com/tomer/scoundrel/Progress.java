package com.tomer.scoundrel;

import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.runs.RunLog;
import com.tomer.scoundrel.tutorial.TutorialFlag;

/**
 * The progress-reset composition: erasing all recorded runs and earned
 * achievements together. Pure and headless (no LibGDX), so the "wipe both
 * stores" contract the reset feature promises can be unit-tested —
 * {@link ScoundrelGame#eraseAllProgress()} is a thin delegate to this. Lives in
 * the composition root because it spans both observer packages, which by the
 * layering never import each other.
 */
public final class Progress {

    private Progress() {
    }

    /**
     * Wipes the run history, the unlocked-achievement latch, and the
     * tutorial-seen marker (so a full reset makes the player new again). Each
     * {@code clear()} is a recoverable soft-delete — the file is moved aside to a
     * {@code .bak} sibling — so an accidental reset stays recoverable from disk.
     */
    public static void eraseAll(RunLog runLog, AchievementStore achievements, TutorialFlag tutorial) {
        runLog.clear();
        achievements.clear();
        tutorial.clear();
    }
}
