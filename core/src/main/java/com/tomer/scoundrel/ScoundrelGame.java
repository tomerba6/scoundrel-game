package com.tomer.scoundrel;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.rules.GameMode;
import com.tomer.scoundrel.runs.RunLog;
import com.tomer.scoundrel.screens.GameScreen;
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

    @Override
    public void create() {
        theme = new Theme();
        Path home = Path.of(System.getProperty("user.home"), ".scoundrel");
        runLog = new RunLog(home.resolve("runs.log"));
        achievements = new AchievementStore(home.resolve("achievements.log"));
        showTitle();
    }

    public void showTitle() {
        switchTo(new TitleScreen(this, theme));
    }

    public void showGame(GameMode mode) {
        switchTo(new GameScreen(this, theme, runLog, achievements, mode));
    }

    public void showRecords() {
        switchTo(new RecordsScreen(this, theme, runLog, achievements));
    }

    public void showTrophies() {
        switchTo(new TrophiesScreen(this, theme, achievements));
    }

    /**
     * Wipes all recorded runs and earned achievements. Both files are moved
     * aside to recoverable {@code .bak} backups rather than deleted. Guarded in
     * the UI behind a confirmation; callers own that safety step. The wipe itself
     * is the pure {@link Progress#eraseAll} so it can be tested headlessly.
     */
    public void eraseAllProgress() {
        Progress.eraseAll(runLog, achievements);
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
