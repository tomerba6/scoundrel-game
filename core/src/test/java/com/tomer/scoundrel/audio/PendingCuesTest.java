package com.tomer.scoundrel.audio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingCuesTest {

    private static final Sfx BLADE = new Sfx(Sound.BLADE, "blade_light_1", 1f, 1f);
    private static final Sfx THUD = new Sfx(Sound.THUD, "thud_light_1", 1f, 1f);
    private static final Sfx DRINK = new Sfx(Sound.DRINK, "drink_light_1", 1f, 1f);
    private static final Sfx FLIP_1 = new Sfx(Sound.FLIP, "flip_1", 1f, 1f);
    private static final Sfx FLIP_2 = new Sfx(Sound.FLIP, "flip_2", 1f, 1f);
    private static final Sfx FLIP_3 = new Sfx(Sound.FLIP, "flip_3", 1f, 1f);

    @Test
    void nothingIsDueBeforeItsBeat() {
        PendingCues cues = new PendingCues();
        cues.schedule(BLADE, 0.167f);
        assertEquals(List.of(), cues.due(0.1f));
        assertFalse(cues.isEmpty());
    }

    @Test
    void aCueIsDueOnItsBeatAndOnlyOnce() {
        PendingCues cues = new PendingCues();
        cues.schedule(BLADE, 0.167f);
        assertEquals(List.of(BLADE), cues.due(0.2f));
        assertEquals(List.of(), cues.due(0.3f));
        assertTrue(cues.isEmpty());
    }

    @Test
    void aBeatAtZeroIsDueStraightAway() {
        PendingCues cues = new PendingCues();
        cues.schedule(BLADE, 0f);
        assertEquals(List.of(BLADE), cues.due(0f));
    }

    @Test
    void soundsSharingABeatComeOutInTheOrderTheyWereScheduled() {
        // A weapon kill's blade and thud are one moment; the blade leads.
        PendingCues cues = new PendingCues();
        cues.scheduleAll(List.of(BLADE, THUD), 0.167f);
        assertEquals(List.of(BLADE, THUD), cues.due(0.167f));
    }

    @Test
    void dueCuesComeOutInBeatOrderWhateverOrderTheyWereScheduled() {
        PendingCues cues = new PendingCues();
        cues.schedule(FLIP_2, 0.333f);
        cues.schedule(FLIP_1, 0.25f);
        assertEquals(List.of(FLIP_1, FLIP_2), cues.due(1f));
    }

    @Test
    void aFlushHandsOverEverythingStillPendingOnceAndEmptiesTheQueue() {
        PendingCues cues = new PendingCues();
        cues.schedule(DRINK, 0.417f);
        cues.schedule(BLADE, 0.167f);
        assertEquals(List.of(BLADE, DRINK), cues.flush());
        assertTrue(cues.isEmpty());
        assertEquals(List.of(), cues.flush());
        assertEquals(List.of(), cues.due(10f));
    }

    @Test
    void aFlushDoesNotReplayWhatAlreadyPlayed() {
        PendingCues cues = new PendingCues();
        cues.schedule(BLADE, 0.167f);
        cues.schedule(DRINK, 0.417f);
        assertEquals(List.of(BLADE), cues.due(0.2f));
        assertEquals(List.of(DRINK), cues.flush());
    }

    @Test
    void aSkippedDealIsOneFlipNotFour() {
        PendingCues cues = new PendingCues();
        cues.schedule(FLIP_1, 0.25f);
        cues.schedule(FLIP_2, 0.333f);
        cues.schedule(FLIP_3, 0.417f);
        cues.schedule(FLIP_1, 0.5f);
        assertEquals(List.of(FLIP_1), cues.flush());
    }

    @Test
    void collapsingTheFlipsKeepsEverythingElse() {
        PendingCues cues = new PendingCues();
        cues.schedule(BLADE, 0.167f);
        cues.schedule(FLIP_2, 0.25f);
        cues.schedule(THUD, 0.167f);
        cues.schedule(FLIP_3, 0.333f);
        assertEquals(List.of(BLADE, THUD, FLIP_2), cues.flush());
    }

    @Test
    void aFreshQueueIsEmpty() {
        PendingCues cues = new PendingCues();
        assertTrue(cues.isEmpty());
        assertEquals(List.of(), cues.due(1f));
        assertEquals(List.of(), cues.flush());
    }
}
