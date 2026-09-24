package com.tomer.scoundrel.audio;

/**
 * The player's volume: a music level and a sound level, each off or one of
 * three steps, and a mute that silences both without forgetting them.
 *
 * <p>Three steps and not a slider because the title's MUSIC and SOUND plates
 * cycle on a click, as every menu button acts; a slider is a widget this game
 * does not have. Each step is 6 dB — half the amplitude, clearly different —
 * so the quietest is still audible at −12 dB.
 *
 * <p>A setting, not progress: {@code Progress.eraseAll} never touches it.
 */
public record AudioSettings(int music, int sound, boolean muted) {

    public static final int MAX_LEVEL = 3;
    /** First launch: both on, the music a step below the effects. */
    public static final AudioSettings DEFAULTS = new AudioSettings(2, 3, false);
    private static final double DECIBELS_PER_STEP = 6.0;

    public AudioSettings {
        requireLevel("music", music);
        requireLevel("sound", sound);
    }

    /** The music plate: up a step, and from the top back round to off. Turning it up unmutes. */
    public AudioSettings cycleMusic() {
        return new AudioSettings(next(music), sound, false);
    }

    /** The sound plate, likewise. */
    public AudioSettings cycleSound() {
        return new AudioSettings(music, next(sound), false);
    }

    /** M: silence everything, or bring it back at the levels it had. */
    public AudioSettings toggleMute() {
        return new AudioSettings(music, sound, !muted);
    }

    /** The linear amplitude to scale music by. */
    public float musicGain() {
        return muted ? 0f : gain(music);
    }

    /** The linear amplitude to scale sound effects by. */
    public float soundGain() {
        return muted ? 0f : gain(sound);
    }

    /** 0 is silent; the top step is the file as mastered, and each step below it 6 dB quieter. */
    static float gain(int level) {
        if (level <= 0) {
            return 0f;
        }
        double decibels = -DECIBELS_PER_STEP * (MAX_LEVEL - level);
        return (float) Math.pow(10.0, decibels / 20.0);
    }

    private static int next(int level) {
        return (level + 1) % (MAX_LEVEL + 1);
    }

    private static void requireLevel(String name, int level) {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException(name + " level must be 0.." + MAX_LEVEL + ", got " + level);
        }
    }
}
