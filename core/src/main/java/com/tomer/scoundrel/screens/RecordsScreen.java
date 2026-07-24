package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.model.Status;
import com.tomer.scoundrel.rules.GameMode;
import com.tomer.scoundrel.rules.GameModes;
import com.tomer.scoundrel.runs.HighScores;
import com.tomer.scoundrel.runs.RunLog;
import com.tomer.scoundrel.runs.RunRecord;
import com.tomer.scoundrel.runs.RunTotals;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import static com.tomer.scoundrel.screens.Widgets.dangerButton;
import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;
import static com.tomer.scoundrel.screens.Widgets.mutedButton;
import static com.tomer.scoundrel.screens.Widgets.torchButton;

/**
 * THE LEDGER — the top runs and lifetime totals, read once from the run log
 * on entry. Totals are labeled "finished runs" deliberately: abandoned games
 * are never recorded, so finished games are the whole universe.
 */
public final class RecordsScreen extends ScreenAdapter {

    private static final String[] ROMAN =
            {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final ScoundrelGame game;
    private final Theme theme;
    private final Stage stage;

    public RecordsScreen(ScoundrelGame game, Theme theme, RunLog runLog, AchievementStore achievements) {
        this.game = game;
        this.theme = theme;
        stage = new Stage(new FitViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT));
        List<RunRecord> records = readSafely(runLog);
        int trophies = trophyCount(achievements);
        boolean hasProgress = !records.isEmpty() || trophies > 0;

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(28, 56, 24, 56);
        stage.addActor(root);

        root.add(label("THE LEDGER", theme.title, Theme.BONE)).colspan(2).padBottom(24);
        root.row();
        if (records.isEmpty()) {
            root.add(label("No runs recorded yet — the dungeon awaits.",
                    theme.body, dim(Theme.BONE, 0.6f))).colspan(2).expand();
            root.row();
        } else {
            root.add(scoreTable(theme, records)).top().expandX().left();
            root.add(totalsColumn(theme, RunTotals.of(records))).top().padLeft(56);
            root.row();
            root.add().expand().colspan(2);
            root.row();
        }
        root.add(bottomBar(records.size(), trophies, hasProgress)).colspan(2).growX();
    }

    /** Back on the left; the quiet Erase control far right, disabled with nothing to lose. */
    private Actor bottomBar(int runs, int trophies, boolean hasProgress) {
        Table bar = new Table();
        TextButton back = torchButton(theme, "Back");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTitle();
            }
        });
        bar.add(back).left();
        bar.add().expandX();
        TextButton erase = mutedButton(theme, "Erase all progress");
        erase.setDisabled(!hasProgress);
        if (hasProgress) {
            erase.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showEraseConfirmation(runs, trophies);
                }
            });
        }
        bar.add(erase).right();
        return bar;
    }

    /**
     * A modal, two-button confirmation — a destructive erase must never be one
     * click. It names exactly what will be lost, and both buttons use release
     * semantics so a press that slides off is cancelled. "Keep it" is the
     * prominent default; "Erase everything" wears the danger colour.
     */
    private void showEraseConfirmation(int runs, int trophies) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        // A Table is childrenOnly by default, which would let presses fall through.
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(theme.solid(dim(Theme.SOOT, 0.9f)));

        overlay.add(label("Erase all progress?", theme.title, Theme.DRIED_BLOOD)).padBottom(12);
        overlay.row();
        overlay.add(label("This clears all " + runs + " recorded " + plural(runs, "run", "runs")
                + " and " + trophies + " " + plural(trophies, "trophy", "trophies") + ".",
                theme.body, Theme.BONE)).padBottom(4);
        overlay.row();
        overlay.add(label("A backup is kept on disk, but the game will not restore it for you.",
                theme.small, dim(Theme.BONE, 0.6f))).padBottom(28);
        overlay.row();

        Table buttons = new Table();
        TextButton keep = torchButton(theme, "Keep it");
        keep.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                overlay.remove();
            }
        });
        buttons.add(keep).padRight(48);
        TextButton erase = dangerButton(theme, "Erase everything");
        erase.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.eraseAllProgress();
                game.showRecords(); // rebuild, now showing the empty ledger
            }
        });
        buttons.add(erase);
        overlay.add(buttons);
        stage.addActor(overlay);
    }

    private static String plural(int count, String one, String many) {
        return count == 1 ? one : many;
    }

    private static int trophyCount(AchievementStore achievements) {
        try {
            return achievements.unlockedIds().size();
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "failed to read achievements", e);
            return 0;
        }
    }

    private static List<RunRecord> readSafely(RunLog runLog) {
        try {
            return runLog.readAll();
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "failed to read the run log", e);
            return List.of();
        }
    }

    private Table scoreTable(Theme theme, List<RunRecord> records) {
        Table table = new Table();
        List<RunRecord> top = HighScores.top(records, ROMAN.length);
        for (int i = 0; i < top.size(); i++) {
            RunRecord run = top.get(i);
            boolean won = run.outcome() == Status.WON;
            table.add(label(ROMAN[i], theme.bodyBold, Theme.TORCHLIGHT)).right().padRight(20);
            table.add(label(String.valueOf(run.score()), theme.bodyBold,
                    run.score() < 0 ? Theme.DRIED_BLOOD : Theme.BONE)).right().padRight(28);
            table.add(label(won ? "cleared" : "defeated", theme.body,
                    won ? dim(Theme.TORCHLIGHT, 0.9f) : dim(Theme.BONE, 0.5f))).left().padRight(28);
            // Scores are ranked per mode, so a row must say which one it was won in.
            table.add(label(modeLabel(run.rulesetId()), theme.small, dim(Theme.BONE, 0.5f)))
                    .left().padRight(28);
            table.add(label(DAY.format(run.endedAt()), theme.small, dim(Theme.BONE, 0.5f)))
                    .left().padRight(28);
            table.add(label(duration(run.seconds()), theme.small, dim(Theme.BONE, 0.5f)))
                    .left().padRight(28);
            table.add(label(run.monstersDefeated() + " slain", theme.small, dim(Theme.BONE, 0.5f)))
                    .left();
            table.row();
            table.add(new Image(theme.solid(dim(Theme.BONE, 0.13f))))
                    .colspan(7).growX().height(1).padTop(7).padBottom(7);
            table.row();
        }
        return table;
    }

    /** The mode's menu name; an unknown or retired id falls back to the raw id. */
    private static String modeLabel(String rulesetId) {
        return GameModes.byId(rulesetId).map(GameMode::title).orElse(rulesetId);
    }

    private Table totalsColumn(Theme theme, RunTotals totals) {
        Table side = new Table();
        side.add(label("ACROSS " + totals.runs() + " FINISHED RUNS",
                theme.bodyBold, Theme.TORCHLIGHT)).colspan(2).left().padBottom(14);
        side.row();
        stat(side, theme, "cleared", totals.wins() + "  (" + Math.round(totals.winRate() * 100) + "%)");
        stat(side, theme, "defeated", String.valueOf(totals.losses()));
        stat(side, theme, "monsters slain", String.valueOf(totals.monstersDefeated()));
        stat(side, theme, "damage taken", String.valueOf(totals.damageTaken()));
        stat(side, theme, "health healed", String.valueOf(totals.healthHealed()));
        stat(side, theme, "potions drunk", totals.potionsDrunk() + "  (" + totals.potionsWasted() + " wasted)");
        stat(side, theme, "weapons equipped", String.valueOf(totals.weaponsEquipped()));
        stat(side, theme, "rooms avoided", String.valueOf(totals.roomsAvoided()));
        stat(side, theme, "time below", duration(totals.secondsPlayed()));
        return side;
    }

    private void stat(Table side, Theme theme, String name, String value) {
        side.add(label(name, theme.body, dim(Theme.BONE, 0.55f))).left().padRight(28).padBottom(5);
        side.add(label(value, theme.bodyBold, Theme.BONE)).right().padBottom(5);
        side.row();
    }

    private static String duration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long rest = seconds % 60;
        return hours > 0 ? hours + "h " + minutes + "m" : minutes + ":" + String.format("%02d", rest);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Theme.SOOT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
