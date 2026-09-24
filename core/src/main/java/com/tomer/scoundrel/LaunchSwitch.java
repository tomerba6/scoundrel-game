package com.tomer.scoundrel;

/**
 * A developer switch read at launch, spelled two ways: a system property, or an
 * environment variable — the one that works through {@code gradlew lwjgl3:run},
 * which does not pass {@code -D} on to the game's JVM. Pure, and in the root
 * package because the launcher reads one and the screens the other.
 */
public record LaunchSwitch(String property, String environment) {

    /** The sound log: every sound, cue and level change, timestamped, on standard out. */
    public static final LaunchSwitch AUDIO_LOG = new LaunchSwitch("scoundrel.audio.log", "SCOUNDREL_AUDIO_LOG");
    /** No audio device, simulated: the launcher starts LibGDX with its audio disabled. */
    public static final LaunchSwitch NO_AUDIO = new LaunchSwitch("scoundrel.no.audio", "SCOUNDREL_NO_AUDIO");

    /** Whether this process was launched with the switch on. */
    public boolean isOn() {
        return on(System.getProperty(property), System.getenv(environment));
    }

    static boolean on(String property, String environment) {
        return "true".equalsIgnoreCase(property)
                || "1".equals(environment) || "true".equalsIgnoreCase(environment);
    }
}
