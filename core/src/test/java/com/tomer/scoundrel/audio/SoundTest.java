package com.tomer.scoundrel.audio;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundTest {

    /**
     * The asset contract, spelled out by hand rather than derived, so that a
     * change to the enum which silently renames or drops a file fails here.
     * Mirrors docs/audio.md's table: 29 files.
     */
    private static final Set<String> CONTRACT = Set.of(
            "click_1",
            "flip_1", "flip_2", "flip_3",
            "sweep_1",
            "equip_light_1", "equip_medium_1", "equip_heavy_1",
            "blade_light_1", "blade_light_2", "blade_medium_1", "blade_medium_2",
            "blade_heavy_1", "blade_heavy_2",
            "thud_light_1", "thud_light_2", "thud_heavy_1", "thud_heavy_2",
            "fist_light_1", "fist_light_2", "fist_medium_1", "fist_medium_2",
            "fist_heavy_1", "fist_heavy_2",
            "drink_light_1", "drink_medium_1", "drink_heavy_1",
            "spill_1",
            "chime_1");

    @Test
    void theFilesAreExactlyTheContract() {
        List<String> files = Sound.allFiles();
        assertEquals(29, files.size());
        assertEquals(CONTRACT, Set.copyOf(files));
    }

    @Test
    void thudHasNoMediumBecauseDamageHasNoMediumBand() {
        assertEquals(List.of(Weight.LIGHT, Weight.HEAVY), Sound.THUD.weights());
        assertEquals(List.of(Weight.LIGHT, Weight.MEDIUM, Weight.HEAVY), Sound.BLADE.weights());
        assertEquals(List.of(), Sound.CLICK.weights());
    }

    @Test
    void stemsAreTheSoundThenItsWeight() {
        assertEquals("click", Sound.CLICK.stem());
        assertEquals("blade_light", Sound.BLADE.stem(Weight.LIGHT));
        assertEquals("fist_heavy", Sound.FIST.stem(Weight.HEAVY));
    }

    @Test
    void aStemMustMatchWhetherTheSoundIsWeighted() {
        assertThrows(IllegalArgumentException.class, () -> Sound.BLADE.stem());
        assertThrows(IllegalArgumentException.class, () -> Sound.CLICK.stem(Weight.LIGHT));
        assertThrows(IllegalArgumentException.class, () -> Sound.THUD.stem(Weight.MEDIUM));
    }

    @Test
    void onlySoundsWithSeveralVersionsVary() {
        for (Sound sound : Sound.values()) {
            assertEquals(sound.versions() > 1, sound.varies(), sound.name());
        }
        assertTrue(Sound.FLIP.varies());
        assertFalse(Sound.CHIME.varies());
    }

    @Test
    void onlyTheFlipCollapsesOnASkip() {
        for (Sound sound : Sound.values()) {
            assertEquals(sound == Sound.FLIP, sound.collapsesOnSkip(), sound.name());
        }
    }

    @Test
    void theFlipCanSoundOnceForEveryCardOfADeal() {
        // A deal's flips overlap by design (the riffle): four cards, four voices.
        assertEquals(4, Sound.FLIP.voices());
    }

    @Test
    void aSoundHeardOftenCanOverlapItselfOnceAndTheChimeNever() {
        // Fast play lands a second kill while the first still rings; cutting it off
        // would click. A second chime on top of the first would only be louder.
        for (Sound sound : Sound.values()) {
            if (sound != Sound.FLIP && sound != Sound.CHIME) {
                assertEquals(2, sound.voices(), sound.name());
            }
        }
        assertEquals(1, Sound.CHIME.voices());
    }

    @Test
    void aSfxKnowsWhereItsFileShips() {
        Sfx sfx = new Sfx(Sound.BLADE, "blade_light_2", 1f, 1f);
        assertEquals("audio/sfx/blade_light_2.wav", sfx.path());
    }
}
