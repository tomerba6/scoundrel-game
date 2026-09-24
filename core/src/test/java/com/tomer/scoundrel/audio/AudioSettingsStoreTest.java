package com.tomer.scoundrel.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioSettingsStoreTest {

    @TempDir
    Path dir;

    private AudioSettingsStore store() {
        return new AudioSettingsStore(dir.resolve("audio.settings"));
    }

    private AudioSettings loadFrom(String content) throws IOException {
        Files.writeString(dir.resolve("audio.settings"), content, StandardCharsets.UTF_8);
        return store().load();
    }

    @Test
    void noFileYetMeansTheDefaults() {
        assertEquals(AudioSettings.DEFAULTS, store().load());
    }

    @Test
    void whatIsSavedIsWhatLoads() {
        AudioSettings settings = new AudioSettings(1, 0, true);
        store().save(settings);
        assertEquals(settings, store().load());
    }

    @Test
    void theFileIsOneVersionedLineOfKeysAndValues() throws IOException {
        store().save(new AudioSettings(1, 3, false));
        assertEquals("v=1\tmusic=1\tsound=3\tmuted=false",
                Files.readString(dir.resolve("audio.settings"), StandardCharsets.UTF_8).strip());
    }

    @Test
    void savingCreatesTheDirectory() {
        AudioSettingsStore nested = new AudioSettingsStore(dir.resolve("a").resolve("b").resolve("audio.settings"));
        nested.save(new AudioSettings(0, 1, false));
        assertEquals(new AudioSettings(0, 1, false), nested.load());
    }

    @Test
    void savingAgainReplacesTheOldSettings() {
        store().save(new AudioSettings(1, 1, true));
        store().save(new AudioSettings(3, 2, false));
        assertEquals(new AudioSettings(3, 2, false), store().load());
    }

    @Test
    void anUnreadableFileMeansTheDefaults() throws IOException {
        assertEquals(AudioSettings.DEFAULTS, loadFrom("this is not a settings file"));
        assertEquals(AudioSettings.DEFAULTS, loadFrom(""));
        assertEquals(AudioSettings.DEFAULTS, loadFrom("v=x\tmusic=1"));
    }

    @Test
    void aVersionItDoesNotKnowIsNotHalfRead() throws IOException {
        // A future format may mean something else by the same keys.
        assertEquals(AudioSettings.DEFAULTS, loadFrom("v=2\tmusic=0\tsound=0\tmuted=true"));
        assertEquals(AudioSettings.DEFAULTS, loadFrom("music=0\tsound=0\tmuted=true"));
    }

    @Test
    void aBadValueFallsBackForThatKeyAlone() throws IOException {
        assertEquals(new AudioSettings(2, 1, true), loadFrom("v=1\tmusic=9\tsound=1\tmuted=true"));
        assertEquals(new AudioSettings(2, 1, false), loadFrom("v=1\tmusic=-1\tsound=1"));
        assertEquals(new AudioSettings(0, 3, false), loadFrom("v=1\tmusic=0\tsound=loud\tmuted=perhaps"));
        assertEquals(new AudioSettings(1, 3, false), loadFrom("v=1\tmusic=1"));
    }

    @Test
    void unknownKeysAndStrayTokensAreIgnored() throws IOException {
        assertEquals(new AudioSettings(1, 2, true),
                loadFrom("v=1\tmusic=1\tsound=2\tmuted=TRUE\treverb=lots\tjunk\t=3"));
    }

    @Test
    void aFileThatCannotBeReadIsAnError() throws IOException {
        // The store reports it; the game decides to carry on with the defaults.
        Files.createDirectories(dir.resolve("audio.settings"));
        assertThrows(UncheckedIOException.class, () -> store().load());
    }

    @Test
    void aFileThatCannotBeWrittenIsAnError() throws IOException {
        Files.writeString(dir.resolve("blocker"), "a file where a directory should be");
        AudioSettingsStore blocked = new AudioSettingsStore(dir.resolve("blocker").resolve("audio.settings"));
        assertThrows(UncheckedIOException.class, () -> blocked.save(AudioSettings.DEFAULTS));
    }
}
