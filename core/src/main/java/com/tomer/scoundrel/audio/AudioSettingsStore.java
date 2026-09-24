package com.tomer.scoundrel.audio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The volume settings on disk: one versioned line of tab-separated
 * {@code key=value} tokens, the same shape as a run-log line. A sibling of
 * {@code RunLog} and {@code TutorialFlag} — the path is injected and a missing
 * file simply means the defaults. Pure Java.
 *
 * <p>Tolerant the way the other stores are, and a little more: a file that is
 * not version 1 is ignored whole, since a future format may mean something else
 * by the same keys, but within a version-1 file a bad value falls back for that
 * key alone. Losing one level to a typo is better than losing both.
 *
 * <p>Not part of the progress reset, and so it has no {@code clear()}.
 */
public final class AudioSettingsStore {

    static final int VERSION = 1;

    private final Path file;

    public AudioSettingsStore(Path file) {
        this.file = file;
    }

    /** The saved settings, or the defaults when there are none worth reading. */
    public AudioSettings load() {
        if (!Files.exists(file)) {
            return AudioSettings.DEFAULTS;
        }
        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("could not read audio settings from " + file, e);
        }
    }

    /** Replaces whatever was saved before. */
    public void save(AudioSettings settings) {
        String line = "v=" + VERSION
                + "\tmusic=" + settings.music()
                + "\tsound=" + settings.sound()
                + "\tmuted=" + settings.muted();
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("could not save audio settings to " + file, e);
        }
    }

    static AudioSettings parse(String text) {
        Map<String, String> kv = new HashMap<>();
        for (String token : text.strip().split("\\s+")) {
            int eq = token.indexOf('=');
            if (eq > 0) {
                kv.put(token.substring(0, eq), token.substring(eq + 1));
            }
        }
        if (!String.valueOf(VERSION).equals(kv.get("v"))) {
            return AudioSettings.DEFAULTS;
        }
        AudioSettings defaults = AudioSettings.DEFAULTS;
        return new AudioSettings(
                level(kv.get("music"), defaults.music()),
                level(kv.get("sound"), defaults.sound()),
                flag(kv.get("muted"), defaults.muted()));
    }

    private static int level(String text, int fallback) {
        try {
            int level = Integer.parseInt(text);
            return level >= 0 && level <= AudioSettings.MAX_LEVEL ? level : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean flag(String text, boolean fallback) {
        if (text == null) {
            return fallback;
        }
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> fallback;
        };
    }
}
