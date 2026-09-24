package com.tomer.scoundrel.audio;

/**
 * One sound effect, chosen and ready to play: which file, at what pitch, and
 * how loud relative to the file itself. Volume is at most 1 — a variation only
 * ever makes a play quieter, never louder than the file was mastered.
 */
public record Sfx(Sound sound, String file, float pitch, float volume) {

    /** Where the sound-effect files ship, relative to the asset root. */
    public static final String DIRECTORY = "audio/sfx/";
    public static final String EXTENSION = ".wav";

    /** The internal asset path of this sound's file. */
    public String path() {
        return DIRECTORY + file + EXTENSION;
    }
}
