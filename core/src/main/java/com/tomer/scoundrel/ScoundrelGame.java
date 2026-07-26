package com.tomer.scoundrel;

import com.badlogic.gdx.Game;
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

    private Theme theme;
    private RunLog runLog;
    private AchievementStore achievements;
    private TutorialFlag tutorialFlag;

    @Override
    public void create() {
        theme = new Theme();
        Path home = Path.of(System.getProperty("user.home"), ".scoundrel");
        runLog = new RunLog(home.resolve("runs.log"));
        achievements = new AchievementStore(home.resolve("achievements.log"));
        tutorialFlag = new TutorialFlag(home.resolve("tutorial.seen"));
        showTitle();
    }

    public void showTitle() {
        switchTo(new TitleScreen(this, theme));
    }

    /** The mode picker — where a run is chosen before it begins. */
    public void showModeSelect() {
        switchTo(new ModeSelectScreen(this, theme));
    }

    public void showGame(GameMode mode) {
        switchTo(new GameScreen(this, theme, runLog, achievements, mode));
    }

    /**
     * The guided tutorial — a scripted Standard game with narration, recorded
     * nowhere. Entering it marks the tutorial seen, so it is only auto-offered
     * once even if the player leaves partway.
     */
    public void showTutorial() {
        tutorialFlag.markSeen();
        switchTo(new GameScreen(this, theme, GameModes.STANDARD, new TutorialGuide(TutorialScript.steps())));
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
        theme.dispose();
    }
}
