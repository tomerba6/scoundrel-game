package com.tomer.scoundrel.audio;

import java.util.function.Consumer;

/**
 * The player's volume as it lives through a session: read once at launch,
 * saved the moment it changes, and never a crash.
 *
 * <p>{@link AudioSettingsStore} throws when the disk does, as every store does;
 * this is where that stops. A file that cannot be read means the defaults, and
 * a change that cannot be saved still holds until the game closes. Either is
 * handed to {@code onFailure} to be logged — sound is never worth a crash, and a
 * volume that will not stick is not worth a dialog.
 */
public final class AudioControls {

    private final AudioSettingsStore store;
    private final Consumer<RuntimeException> onFailure;
    private AudioSettings settings;
    private boolean minimised;

    public AudioControls(AudioSettingsStore store, Consumer<RuntimeException> onFailure) {
        this.store = store;
        this.onFailure = onFailure;
        this.settings = read();
    }

    /** The levels now. */
    public AudioSettings settings() {
        return settings;
    }

    /** The MUSIC plate: up a step, round to off, un-muting. Saved at once. */
    public AudioSettings cycleMusic() {
        return change(settings.cycleMusic());
    }

    /** The SOUND plate, likewise. */
    public AudioSettings cycleSound() {
        return change(settings.cycleSound());
    }

    /** M: silence everything, or bring it back at the levels it had. Saved at once. */
    public AudioSettings toggleMute() {
        return change(settings.toggleMute());
    }

    /**
     * The window went down or came back. Minimised, nothing is heard, but nothing
     * stops either: the audio keeps time with the picture, which goes on rendering,
     * so a cue due meanwhile plays out unheard rather than minutes late. Not a
     * setting, so nothing is saved.
     */
    public void windowMinimised(boolean minimised) {
        this.minimised = minimised;
    }

    /** The music's gain as it should sound now: the level, or silence while minimised. */
    public float musicGain() {
        return minimised ? 0f : settings.musicGain();
    }

    /** The sound effects' gain as they should sound now, likewise. */
    public float soundGain() {
        return minimised ? 0f : settings.soundGain();
    }

    private AudioSettings read() {
        try {
            return store.load();
        } catch (RuntimeException e) {
            onFailure.accept(e);
            return AudioSettings.DEFAULTS;
        }
    }

    private AudioSettings change(AudioSettings next) {
        settings = next;
        try {
            store.save(next);
        } catch (RuntimeException e) {
            onFailure.accept(e);
        }
        return next;
    }
}
