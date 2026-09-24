package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioLogTest {

    private final AtomicLong nanos = new AtomicLong(5_000_000_000L);
    private final List<String> lines = new ArrayList<>();

    @Test
    void offItWritesNothing() {
        AudioLog log = new AudioLog(false, nanos::get, lines::add);
        log.log("play click_1");
        assertEquals(List.of(), lines);
        assertFalse(log.enabled());
    }

    @Test
    void onEachLineCarriesTheSecondsSinceItStarted() {
        AudioLog log = new AudioLog(true, nanos::get, lines::add);
        nanos.addAndGet(1_234_567_000L);
        log.log("play blade_light_1");
        nanos.addAndGet(250_000_000L);
        log.log("effect SLICE");
        assertEquals(List.of("audio     1.235 play blade_light_1", "audio     1.485 effect SLICE"), lines);
        assertTrue(log.enabled());
    }

    @Test
    void eitherTheSystemPropertyOrTheEnvironmentTurnsItOn() {
        // gradlew lwjgl3:run does not forward -D to the game's JVM, but the
        // environment reaches it; a packaged jar takes the property.
        assertTrue(AudioLog.enabledBy("true", null));
        assertTrue(AudioLog.enabledBy(null, "1"));
        assertTrue(AudioLog.enabledBy(null, "true"));
        assertTrue(AudioLog.enabledBy("TRUE", "0"));
        assertFalse(AudioLog.enabledBy(null, null));
        assertFalse(AudioLog.enabledBy("false", "0"));
        assertFalse(AudioLog.enabledBy("yes please", ""));
    }
}
