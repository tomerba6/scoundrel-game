package com.tomer.scoundrel.audio;

import java.util.Locale;

/**
 * How heavy a sound is. Every weighted sound is rendered once per weight, as a
 * separate file, so a heavy one can have a body a light one does not — more
 * low end, a longer tail — rather than being the same file pitched down.
 */
public enum Weight {
    LIGHT,
    MEDIUM,
    HEAVY;

    /** How the weight is spelled in a file name: {@code blade_heavy_1}. */
    public String fileName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
