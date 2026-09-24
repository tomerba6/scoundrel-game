package com.tomer.scoundrel.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped sound-effect files against the contract in {@link Sound}.
 *
 * <p>The files are rendered by Python ({@code audio-source/}) and committed, not
 * built, so nothing else stops a missing, stray or malformed one reaching a
 * player. Everything under {@code assets/} ships — a stray file is a failure
 * too, not just a missing one.
 *
 * <p>Per file it checks what the game relies on: the format LibGDX is handed,
 * a peak under −1 dBFS, and an attack inside the first 5 ms, because every
 * sound is played on its animation's beat and a late start lands off it. The
 * rest of the measuring (loudness, tails, loop seams) is {@code check.py}'s,
 * which needs numpy.
 *
 * <p>JDK-only ({@code javax.sound.sampled}); no LibGDX. Tests run from
 * {@code core/}, so the assets are one level up.
 */
class AudioAssetsTest {

    private static final Path SFX = Path.of("..", "assets").resolve(Sfx.DIRECTORY);
    private static final float SAMPLE_RATE = 44100f;
    /** −1 dBFS, in 16-bit sample units. */
    private static final int PEAK_LIMIT = (int) Math.floor(32767 * Math.pow(10, -1 / 20.0));
    /** −40 dBFS: where a sound counts as having started. */
    private static final int ONSET = (int) Math.ceil(32768 * 0.01);
    private static final int LEAD_LIMIT_SAMPLES = Math.round(SAMPLE_RATE * 0.005f);

    static Stream<String> files() {
        return Sound.allFiles().stream();
    }

    @Test
    void theFolderHoldsExactlyTheContract() throws IOException {
        Set<String> expected = Sound.allFiles().stream()
                .map(name -> name + Sfx.EXTENSION)
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> present;
        try (Stream<Path> listing = Files.list(SFX)) {
            present = listing.map(p -> p.getFileName().toString()).collect(Collectors.toCollection(TreeSet::new));
        }
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(present);
        Set<String> stray = new TreeSet<>(present);
        stray.removeAll(expected);
        assertTrue(missing.isEmpty() && stray.isEmpty(),
                "assets/" + Sfx.DIRECTORY + " must hold exactly Sound.allFiles(). Missing: " + missing
                        + "; stray (these would ship): " + stray);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("files")
    void eachFileIsWhatTheGameExpects(String name) throws IOException, UnsupportedAudioFileException {
        Path file = SFX.resolve(name + Sfx.EXTENSION);
        assertTrue(Files.isRegularFile(file), file + " is missing");
        short[] pcm;
        AudioFormat format;
        try (AudioInputStream in = AudioSystem.getAudioInputStream(file.toFile())) {
            format = in.getFormat();
            pcm = samples(in.readAllBytes());
        }
        assertEquals(List.of(AudioFormat.Encoding.PCM_SIGNED, 16, 1, SAMPLE_RATE, false),
                List.of(format.getEncoding(), format.getSampleSizeInBits(), format.getChannels(),
                        format.getSampleRate(), format.isBigEndian()),
                name + ": 16-bit signed little-endian mono PCM at 44.1 kHz");
        assertTrue(pcm.length > 0, name + " is empty");

        int peak = 0;
        int onset = -1;
        for (int i = 0; i < pcm.length; i++) {
            int magnitude = Math.abs((int) pcm[i]);
            peak = Math.max(peak, magnitude);
            if (onset < 0 && magnitude >= ONSET) {
                onset = i;
            }
        }
        assertTrue(peak <= PEAK_LIMIT, name + ": peak " + peak + " is over -1 dBFS (" + PEAK_LIMIT + ")");
        assertTrue(onset >= 0 && onset <= LEAD_LIMIT_SAMPLES,
                name + ": starts at sample " + onset + ", later than 5 ms, so it would land off its beat");
    }

    private static short[] samples(byte[] bytes) {
        short[] out = new short[bytes.length / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (short) ((bytes[2 * i] & 0xff) | (bytes[2 * i + 1] << 8));
        }
        return out;
    }
}
