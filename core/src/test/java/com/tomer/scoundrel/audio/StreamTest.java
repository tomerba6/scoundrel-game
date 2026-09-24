package com.tomer.scoundrel.audio;

import com.tomer.scoundrel.audio.MusicDirector.Cue;
import com.tomer.scoundrel.audio.MusicDirector.Track;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StreamTest {

    /** The contract, by hand: docs/audio.md's five streamed files. */
    @Test
    void theStreamedFilesAreExactlyTheContract() {
        Map<String, String> paths = new TreeMap<>();
        for (Stream stream : Stream.values()) {
            paths.put(stream.name(), stream.path());
        }
        assertEquals(Map.of(
                "MENU", "audio/music/menu.ogg",
                "RUN", "audio/music/run.ogg",
                "WIN", "audio/music/win.ogg",
                "DEATH", "audio/music/death.ogg",
                "TORCH", "audio/ambience/torch.ogg"), paths);
    }

    @Test
    void theTracksAndTheTorchLoopAndTheCuesPlayOnce() {
        assertTrue(Stream.MENU.loops());
        assertTrue(Stream.RUN.loops());
        assertTrue(Stream.TORCH.loops());
        assertFalse(Stream.WIN.loops());
        assertFalse(Stream.DEATH.loops());
    }

    @Test
    void eachTrackHasItsStream() {
        assertEquals(Stream.MENU, Stream.of(Track.MENU));
        assertEquals(Stream.RUN, Stream.of(Track.RUN));
    }

    @Test
    void theEndOfRunCuesHaveStreamsButTheChimeIsASoundEffect() {
        assertEquals(Stream.WIN, Stream.of(Cue.WIN));
        assertEquals(Stream.DEATH, Stream.of(Cue.DEATH));
        assertThrows(IllegalArgumentException.class, () -> Stream.of(Cue.CHIME));
    }
}
