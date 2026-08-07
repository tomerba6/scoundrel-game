package com.tomer.scoundrel;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.rules.GameMode;
import com.tomer.scoundrel.rules.GameModes;
import com.tomer.scoundrel.runs.RunLog;
import com.tomer.scoundrel.tutorial.TutorialFlag;
import com.tomer.scoundrel.tutorial.TutorialGuide;
import com.tomer.scoundrel.tutorial.TutorialScript;
import com.tomer.scoundrel.screens.GameScreen;
import com.tomer.scoundrel.screens.ModeSelectScreen;
import com.tomer.scoundrel.screens.RecordsScreen;
import com.tomer.scoundrel.screens.SpriteLab;
import com.tomer.scoundrel.screens.Sprites;
import com.tomer.scoundrel.screens.Theme;
import com.tomer.scoundrel.screens.TitleScreen;
import com.tomer.scoundrel.screens.TrophiesScreen;

import java.nio.file.Path;

/**
 * {@link com.badlogic.gdx.ApplicationListener} shared by all platforms, and
 * the app's navigator: screens ask it to switch, it owns the shared Theme,
 * RunLog and AchievementStore and disposes whichever screen is being left.
 */
public class ScoundrelGame extends Game {

    private static final int WINDOWED_WIDTH = 1280;
    private static final int WINDOWED_HEIGHT = 720;

    private Theme theme;
    private Sprites sprites;
    private RunLog runLog;
    private AchievementStore achievements;
    private TutorialFlag tutorialFlag;
    // The launcher starts windowed at the design resolution while the art
    // conversion is in progress; flip both back together when it lands.
    private boolean fullscreen = false;

    @Override
    public void create() {
        theme = new Theme();
        sprites = new Sprites();
        Path home = Path.of(System.getProperty("user.home"), ".scoundrel");
        runLog = new RunLog(home.resolve("runs.log"));
        achievements = new AchievementStore(home.resolve("achievements.log"));
        tutorialFlag = new TutorialFlag(home.resolve("tutorial.seen"));
        // First ever launch offers the tutorial; afterward it lives under "How to play".
        switchTo(new TitleScreen(this, theme, sprites, runLog, !tutorialFlag.isSeen()));
    }

    @Override
    public void render() {
        // F11 or Alt+Enter toggles between borderless-fullscreen and windowed. Polled
        // here (not via an input processor) so it works on every screen regardless of
        // which one owns Scene2D input; the resize is handled by each screen's viewport.
        boolean altEnter = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                && (Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)
                    || Gdx.input.isKeyPressed(Input.Keys.ALT_RIGHT));
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11) || altEnter) {
            toggleFullscreen();
        }
        // F9 opens the developer sprite inspector. Polled here for the same
        // reason as F11, and guarded so it can't stack on top of itself.
        if (Gdx.input.isKeyJustPressed(Input.Keys.F9) && !(getScreen() instanceof SpriteLab)) {
            switchTo(new SpriteLab(this, theme, sprites));
        }
        super.render(); // draws the current screen
    }

    private void toggleFullscreen() {
        if (fullscreen) {
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(WINDOWED_WIDTH, WINDOWED_HEIGHT);
        } else {
            Graphics.DisplayMode desktop = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setUndecorated(true);
            Gdx.graphics.setWindowedMode(desktop.width, desktop.height);
        }
        fullscreen = !fullscreen;
    }

    public void showTitle() {
        switchTo(new TitleScreen(this, theme, sprites, runLog));
    }

    /** Records that the first-run tutorial prompt has been answered (played or skipped). */
    public void markTutorialSeen() {
        tutorialFlag.markSeen();
    }

    /** The mode picker — where a run is chosen before it begins. */
    public void showModeSelect() {
        switchTo(new ModeSelectScreen(this, theme));
    }

    public void showGame(GameMode mode) {
        switchTo(new GameScreen(this, theme, sprites, runLog, achievements, mode));
    }

    /**
     * The guided tutorial — a scripted Standard game with narration, recorded
     * nowhere. Entering it marks the tutorial seen, so it is only auto-offered
     * once even if the player leaves partway.
     */
    public void showTutorial() {
        tutorialFlag.markSeen();
        switchTo(new GameScreen(this, theme, sprites, GameModes.STANDARD,
                new TutorialGuide(TutorialScript.steps())));
    }

    public void showRecords() {
        switchTo(new RecordsScreen(this, theme, runLog, achievements));
    }

    public void showTrophies() {
        switchTo(new TrophiesScreen(this, theme, achievements));
    }

    /**
     * Wipes all recorded runs, earned achievements, and the tutorial-seen marker
     * (so a reset makes the player new again). Every file is moved aside to a
     * recoverable {@code .bak} backup rather than deleted. Guarded in the UI
     * behind a confirmation; callers own that safety step. The wipe itself is the
     * pure {@link Progress#eraseAll} so it can be tested headlessly.
     */
    public void eraseAllProgress() {
        Progress.eraseAll(runLog, achievements, tutorialFlag);
    }

    /** setScreen only hides the previous screen; it must also be disposed. */
    private void switchTo(Screen next) {
        Screen previous = getScreen();
        setScreen(next);
        if (previous != null) {
            previous.dispose();
        }
    }

    @Override
    public void dispose() {
        super.dispose(); // hides the current screen
        if (getScreen() != null) {
            getScreen().dispose();
        }
        sprites.dispose();
        theme.dispose();
    }
}
