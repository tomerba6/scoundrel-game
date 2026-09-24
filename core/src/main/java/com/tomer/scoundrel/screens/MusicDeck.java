package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import com.tomer.scoundrel.audio.AudioSettings;
import com.tomer.scoundrel.audio.MusicDirector;
import com.tomer.scoundrel.audio.MusicDirector.Command;
import com.tomer.scoundrel.audio.MusicDirector.Cue;
import com.tomer.scoundrel.audio.MusicDirector.Play;
import com.tomer.scoundrel.audio.MusicDirector.Restart;
import com.tomer.scoundrel.audio.MusicDirector.Track;
import com.tomer.scoundrel.audio.StreamFile;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The music, the end-of-run cues and the torch, streamed. Every decision about
 * them is {@link MusicDirector}'s, which is pure and tested; this only carries
 * them out, once a frame: starting what it is told to start, and setting each
 * stream's volume to what the director says times the player's setting.
 *
 * <p>The torch is not the director's. It burns on every screen, at the player's
 * <em>sound</em> level — it is a sound of the room, not music, so turning the
 * music off leaves it burning — times how brightly the torch is drawn, which is
 * what makes it gutter out with the light when you die.
 *
 * <p>Never takes the game down: a stream that fails to load is logged and silent,
 * and a cue that cannot play reports itself finished at once, or the trophy
 * chime that waits for it would never come.
 */
public final class MusicDeck implements Disposable {

    /** The one-shot cues that are streams; the chime is a sound effect. */
    private static final Cue[] CUES = {Cue.WIN, Cue.DEATH};

    private final SoundBank sounds;
    private final MusicDirector director;
    private final Map<StreamFile, Music> loaded = new EnumMap<>(StreamFile.class);
    private float musicGain = AudioSettings.DEFAULTS.musicGain();
    private float soundGain = AudioSettings.DEFAULTS.soundGain();

    /** For the log: each track's gain last frame, and the torch's light. */
    private final Map<Track, Float> lastGain = new EnumMap<>(Track.class);
    private final Map<Cue, Float> lastCueGain = new EnumMap<>(Cue.class);
    private float lastLight = 1f;

    /** Minimised: nothing plays, and what was playing waits in {@link #held}. */
    private boolean paused;
    private final List<Music> held = new ArrayList<>();
    private float pausedSeconds;
    private int pausedFrames;
    private boolean reported;

    public MusicDeck(SoundBank sounds, MusicDirector director) {
        this.sounds = sounds;
        this.director = director;
        for (StreamFile stream : StreamFile.values()) {
            try {
                Music music = Gdx.audio.newMusic(Gdx.files.internal(stream.path()));
                music.setLooping(stream.loops());
                music.setVolume(0f);
                loaded.put(stream, music);
            } catch (RuntimeException e) {
                Gdx.app.error("audio", "could not load " + stream.path() + "; it will be silent", e);
            }
        }
        for (Cue cue : CUES) {
            Music music = loaded.get(StreamFile.of(cue));
            if (music != null) {
                music.setOnCompletionListener(m -> {
                    sounds.log("cue ended " + cue);
                    director.cueEnded(cue);
                });
            }
        }
        for (Track track : Track.values()) {
            lastGain.put(track, 0f);
        }
        for (Cue cue : CUES) {
            lastCueGain.put(cue, 0f);
        }
        sounds.log("loaded " + loaded.size() + " of " + StreamFile.values().length + " streams");
    }

    /** The player's levels, 0..1 each: {@code AudioSettings.musicGain()} and {@code soundGain()}. */
    public void setGains(float music, float sound) {
        this.musicGain = music;
        this.soundGain = sound;
        sounds.log(String.format(Locale.ROOT, "levels music=%.3f sound=%.3f", music, sound));
    }

    /**
     * One frame: moves the director on, carries out what it asks for, and sets every
     * stream's volume.
     *
     * @param boardIdle  whether the board has stopped animating — a win waits for it
     * @param torchLight how brightly the torch is drawn, 0..1 — the death gutters it
     */
    public void update(float delta, boolean boardIdle, float torchLight) {
        // Minimised, the frame goes on - LibGDX keeps rendering - and so does the
        // director, in step with the picture; only the playing is held back.
        if (paused) {
            reportWhileMinimised(delta);
        }
        for (Command command : director.tick(delta, boardIdle)) {
            carryOut(command);
        }
        for (Track track : Track.values()) {
            float gain = director.gain(track);
            logCrossing(track.name(), lastGain.get(track), gain);
            lastGain.put(track, gain);
            Music music = loaded.get(StreamFile.of(track));
            if (music == null) {
                continue;
            }
            float volume = gain * musicGain;
            music.setVolume(volume);
            if (volume > 0f && !music.isPlaying() && !paused) {
                music.play();
            } else if (volume <= 0f && music.isPlaying()) {
                music.pause();
            }
        }
        // A cue is set to the music level when it starts, and follows it after: M,
        // pressed during the death cue, has to silence the death cue.
        for (Cue cue : CUES) {
            Music music = loaded.get(StreamFile.of(cue));
            boolean playing = music != null && music.isPlaying();
            float gain = playing ? musicGain : 0f;
            logCrossing("cue " + cue, lastCueGain.get(cue), gain);
            lastCueGain.put(cue, gain);
            if (playing) {
                music.setVolume(musicGain);
            }
        }
        logCrossing("torch light", lastLight, torchLight);
        lastLight = torchLight;
        Music torch = loaded.get(StreamFile.TORCH);
        if (torch != null) {
            torch.setVolume(soundGain * torchLight);
            if (!torch.isPlaying() && !paused) {
                torch.play();
            }
        }
    }

    /**
     * The window went down: everything playing stops where it is, and nothing
     * starts until {@link #resume}. LibGDX only tells the game; it does not quiet
     * the audio itself, and it keeps calling {@link #update} while minimised.
     */
    public void pause() {
        if (paused) {
            return;
        }
        paused = true;
        pausedSeconds = 0f;
        pausedFrames = 0;
        reported = false;
        held.clear();
        for (Music music : loaded.values()) {
            if (music.isPlaying()) {
                music.pause();
                held.add(music);
            }
        }
        sounds.log("minimised: holding " + held.size() + " streams");
    }

    /** The window is back: what was held picks up where it stopped. */
    public void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        sounds.log(String.format(Locale.ROOT, "restored after %.1f s (%d frames): playing %d held streams",
                pausedSeconds, pausedFrames, held.size()));
        for (Music music : held) {
            music.play();
        }
        held.clear();
    }

    /** For the log: that the frames went on while minimised, and that nothing played. */
    private void reportWhileMinimised(float delta) {
        pausedSeconds += delta;
        pausedFrames++;
        if (!reported && pausedSeconds >= 1f && sounds.logging()) {
            reported = true;
            long playing = loaded.values().stream().filter(Music::isPlaying).count();
            sounds.log("minimised 1 s: " + pausedFrames + " frames, " + playing + " streams playing");
        }
    }

    private void carryOut(Command command) {
        switch (command) {
            case Restart restart -> {
                sounds.log("music restart " + restart.track());
                Music music = loaded.get(StreamFile.of(restart.track()));
                if (music != null) {
                    music.stop(); // back to the top; update() plays it once it is audible
                }
            }
            case Play play when play.cue() == Cue.CHIME -> sounds.play(sounds.choice().chime());
            case Play play -> {
                sounds.log("music cue " + play.cue());
                Music music = loaded.get(StreamFile.of(play.cue()));
                if (music == null) {
                    director.cueEnded(play.cue()); // nothing will ever finish it otherwise
                    return;
                }
                music.stop();
                music.setVolume(musicGain);
                if (paused) {
                    held.add(music); // it starts when the window comes back
                } else {
                    music.play();
                }
            }
        }
    }

    /** Logs a level reaching silence or full, or leaving either — the sequences' milestones. */
    private void logCrossing(String what, float before, float now) {
        if (!sounds.logging()) {
            return;
        }
        String change = null;
        if (before <= 0f && now > 0f) {
            change = "rising";
        } else if (before > 0f && now <= 0f) {
            change = "silent";
        } else if (before < 0.999f && now >= 0.999f) {
            change = "full";
        } else if (before >= 0.999f && now < 0.999f) {
            change = "falling";
        }
        if (change != null) {
            sounds.log(String.format(Locale.ROOT, "%s %s", what, change));
        }
    }

    @Override
    public void dispose() {
        for (Music music : loaded.values()) {
            music.dispose();
        }
        loaded.clear();
    }
}
