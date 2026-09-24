package com.tomer.scoundrel.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioControlsTest {

    @TempDir
    Path dir;

    private final List<RuntimeException> failures = new ArrayList<>();

    private AudioSettingsStore store() {
        return new AudioSettingsStore(dir.resolve("audio.settings"));
    }

    private AudioControls controls() {
        return new AudioControls(store(), failures::add);
    }

    @Test
    void theFirstLaunchStartsAtTheDefaults() {
        assertEquals(AudioSettings.DEFAULTS, controls().settings());
        assertTrue(failures.isEmpty(), "no file yet is not a failure");
    }

    @Test
    void itStartsWhereTheLastLaunchLeftOff() {
        store().save(new AudioSettings(0, 1, true));
        assertEquals(new AudioSettings(0, 1, true), controls().settings());
    }

    @Test
    void aSettingsFileThatCannotBeReadMeansTheDefaultsAndIsReported() throws IOException {
        Files.createDirectory(dir.resolve("audio.settings")); // exists, but is no file to read

        assertEquals(AudioSettings.DEFAULTS, controls().settings());
        assertEquals(1, failures.size());
    }

    @Test
    void cyclingTheMusicSavesItAtOnce() {
        AudioControls controls = controls();

        AudioSettings changed = controls.cycleMusic();

        assertEquals(new AudioSettings(3, 3, false), changed);
        assertEquals(changed, controls.settings());
        assertEquals(changed, store().load());
    }

    @Test
    void cyclingTheSoundSavesItAtOnce() {
        AudioControls controls = controls();

        AudioSettings changed = controls.cycleSound();

        assertEquals(new AudioSettings(2, 0, false), changed);
        assertEquals(changed, controls.settings());
        assertEquals(changed, store().load());
    }

    @Test
    void mutingSavesItAtOnceAndUnmutingBringsTheLevelsBack() {
        AudioControls controls = controls();

        assertEquals(new AudioSettings(2, 3, true), controls.toggleMute());
        assertEquals(new AudioSettings(2, 3, true), store().load());
        assertEquals(AudioSettings.DEFAULTS, controls.toggleMute());
        assertEquals(AudioSettings.DEFAULTS, store().load());
    }

    @Test
    void turningALevelWhileMutedUnmutes() {
        AudioControls controls = controls();
        controls.toggleMute();

        assertEquals(new AudioSettings(3, 3, false), controls.cycleMusic());
    }

    @Test
    void aSaveThatFailsKeepsTheChangeForThisSessionAndIsReported() throws IOException {
        AudioControls controls = controls();
        Files.createDirectory(dir.resolve("audio.settings")); // now nothing can be written there

        AudioSettings changed = controls.cycleSound();

        assertEquals(new AudioSettings(2, 0, false), changed);
        assertEquals(changed, controls.settings());
        assertEquals(1, failures.size());
    }
}
