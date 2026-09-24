package com.tomer.scoundrel.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioSettingsTest {

    private static final float EPSILON = 1e-4f;

    @Test
    void firstLaunchHasMusicBelowTheEffectsAndNothingMuted() {
        assertEquals(new AudioSettings(2, 3, false), AudioSettings.DEFAULTS);
        assertTrue(AudioSettings.DEFAULTS.musicGain() < AudioSettings.DEFAULTS.soundGain());
    }

    @Test
    void eachLevelIsSixDecibelsBelowTheOneAbove() {
        assertEquals(1f, AudioSettings.gain(3), EPSILON);
        assertEquals((float) Math.pow(10, -6 / 20.0), AudioSettings.gain(2), EPSILON);
        assertEquals((float) Math.pow(10, -12 / 20.0), AudioSettings.gain(1), EPSILON);
        assertEquals(0f, AudioSettings.gain(0), 0f);
    }

    @Test
    void theGainsFollowTheirOwnLevels() {
        AudioSettings settings = new AudioSettings(1, 3, false);
        assertEquals(AudioSettings.gain(1), settings.musicGain(), EPSILON);
        assertEquals(AudioSettings.gain(3), settings.soundGain(), EPSILON);
    }

    @Test
    void mutingSilencesBothButRemembersTheLevels() {
        assertTrue(AudioSettings.DEFAULTS.toggleMute().muted());
        AudioSettings muted = new AudioSettings(2, 3, true);
        assertEquals(0f, muted.musicGain(), 0f);
        assertEquals(0f, muted.soundGain(), 0f);
        AudioSettings back = muted.toggleMute();
        assertEquals(new AudioSettings(2, 3, false), back);
    }

    @Test
    void aPlateCyclesUpThroughTheLevelsAndWrapsToOff() {
        AudioSettings settings = AudioSettings.DEFAULTS; // music 2
        settings = settings.cycleMusic();
        assertEquals(3, settings.music());
        settings = settings.cycleMusic();
        assertEquals(0, settings.music());
        settings = settings.cycleMusic();
        assertEquals(1, settings.music());
        assertEquals(3, settings.sound(), "cycling music leaves the sound alone");
        assertEquals(0, AudioSettings.DEFAULTS.cycleSound().sound());
        assertEquals(2, AudioSettings.DEFAULTS.cycleSound().music(), "and the other way round");
    }

    @Test
    void changingALevelUnmutes() {
        // Turning a volume is a request to hear it; staying silent would read as broken.
        AudioSettings muted = new AudioSettings(2, 3, true);
        assertFalse(muted.cycleMusic().muted());
        assertFalse(muted.cycleSound().muted());
    }

    @Test
    void levelsOutsideOffToThreeAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new AudioSettings(-1, 3, false));
        assertThrows(IllegalArgumentException.class, () -> new AudioSettings(2, 4, false));
    }
}
