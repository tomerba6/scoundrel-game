package com.tomer.scoundrel.audio;

/**
 * The streamed audio: the two music tracks, the two end-of-run cues and the
 * torch's crackle — everything too long to hold in memory as a sound effect, so
 * played as a stream. The file contract for them, as {@link Sound} is for the
 * effects: OGG Vorbis under {@code assets/audio/}, rendered by
 * {@code audio-source/} and held to this list by {@code AudioAssetsTest}.
 *
 * <p>The chime is not here: it is short, and plays as a sound effect.
 */
public enum StreamFile {
    /** Title, mode select, ledger, trophies: the run's theme, stripped back. Loops. */
    MENU("music/menu", true),
    /** Under a run, slow and brooding. Loops. */
    RUN("music/run", true),
    /** A run won: the theme resolving onto a major chord. Once. */
    WIN("music/win", false),
    /** A run lost: sinking, then going out. Once. */
    DEATH("music/death", false),
    /** The torch, on every screen, quietly under the music. Loops. */
    TORCH("ambience/torch", true);

    public static final String ROOT = "audio/";
    public static final String EXTENSION = ".ogg";

    private final String name;
    private final boolean loops;

    StreamFile(String name, boolean loops) {
        this.name = name;
        this.loops = loops;
    }

    /** The internal asset path. */
    public String path() {
        return ROOT + name + EXTENSION;
    }

    /** Whether it plays round and round, or once. */
    public boolean loops() {
        return loops;
    }

    public static StreamFile of(MusicDirector.Track track) {
        return switch (track) {
            case MENU -> MENU;
            case RUN -> RUN;
        };
    }

    /** The stream a cue plays. The chime has none: it is a sound effect. */
    public static StreamFile of(MusicDirector.Cue cue) {
        return switch (cue) {
            case WIN -> WIN;
            case DEATH -> DEATH;
            case CHIME -> throw new IllegalArgumentException("the chime is a sound effect, not a stream");
        };
    }
}
