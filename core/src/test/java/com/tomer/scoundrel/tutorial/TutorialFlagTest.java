package com.tomer.scoundrel.tutorial;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorialFlagTest {

    @TempDir
    Path dir;

    @Test
    void aFreshFlagHasNotBeenSeen() {
        assertFalse(new TutorialFlag(dir.resolve("tutorial.seen")).isSeen());
    }

    @Test
    void markingSeenPersistsAcrossHandlesAndCreatesMissingDirectories() {
        Path file = dir.resolve("nested").resolve("tutorial.seen");
        TutorialFlag flag = new TutorialFlag(file);
        flag.markSeen();
        assertTrue(flag.isSeen());
        assertTrue(Files.exists(file));
        assertTrue(new TutorialFlag(file).isSeen(), "a fresh handle to the same path reads seen");
    }

    @Test
    void markingSeenTwiceIsHarmless() {
        TutorialFlag flag = new TutorialFlag(dir.resolve("tutorial.seen"));
        flag.markSeen();
        flag.markSeen();
        assertTrue(flag.isSeen());
    }

    @Test
    void clearForgetsItButKeepsARecoverableBackup() {
        Path file = dir.resolve("tutorial.seen");
        TutorialFlag flag = new TutorialFlag(file);
        flag.markSeen();
        flag.clear();
        assertFalse(flag.isSeen(), "the game should now treat the player as new");
        assertTrue(Files.exists(dir.resolve("tutorial.seen.bak")), "a backup must be kept");
    }

    @Test
    void clearWithoutHavingSeenItIsANoOp() {
        TutorialFlag flag = new TutorialFlag(dir.resolve("tutorial.seen"));
        flag.clear();
        assertFalse(flag.isSeen());
    }
}
