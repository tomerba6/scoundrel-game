package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.achievements.Achievement;
import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.achievements.Achievements;
import com.tomer.scoundrel.achievements.UnlockedAchievement;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;
import static com.tomer.scoundrel.screens.Widgets.torchButton;

/**
 * TROPHIES — the whole achievement catalog as a book of deeds, read once from
 * the store on entry. Earned trophies are lit with the date they were won;
 * still-locked ones are dimmed but show what to aim for, except hidden ones,
 * which stay "???" until earned. Reachable only between games, like THE LEDGER.
 */
public final class TrophiesScreen extends ScreenAdapter {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    private final Stage stage;

    public TrophiesScreen(ScoundrelGame game, Theme theme, AchievementStore store) {
        stage = new Stage(new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT));
        stage.addActor(new Backdrop(theme));
        Map<String, Instant> earned = readSafely(store);
        List<Achievement> all = Achievements.all();
        long earnedCount = all.stream().filter(a -> earned.containsKey(a.id())).count();

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(28, 56, 24, 56);
        stage.addActor(root);

        root.add(label("TROPHIES", theme.title, Theme.BONE)).padBottom(6);
        root.row();
        root.add(label(earnedCount + " of " + all.size() + " earned",
                theme.body, dim(Theme.BONE, 0.6f))).padBottom(22);
        root.row();
        root.add(catalogTable(theme, all, earned)).growX().top();
        root.row();
        root.add().expand();
        root.row();
        TextButton back = torchButton(theme, "Back");
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTitle();
            }
        });
        root.add(back).left().padTop(16);
    }

    private Table catalogTable(Theme theme, List<Achievement> all, Map<String, Instant> earned) {
        Table table = new Table();
        for (Achievement achievement : all) {
            boolean got = earned.containsKey(achievement.id());
            boolean concealed = achievement.hidden() && !got;
            String title = concealed ? "???" : achievement.title();
            String description = concealed
                    ? "A hidden trophy — earn it to reveal."
                    : achievement.description();
            Color titleColor = got ? Theme.TORCHLIGHT : dim(Theme.BONE, 0.5f);
            Color descColor = got ? dim(Theme.BONE, 0.8f) : dim(Theme.BONE, 0.4f);

            table.add(label(title, theme.bodyBold, titleColor)).left().width(240).padRight(24);
            table.add(label(description, theme.body, descColor)).left().expandX();
            table.add(label(got ? "earned " + DAY.format(earned.get(achievement.id())) : "locked",
                    theme.small, dim(Theme.BONE, got ? 0.5f : 0.3f))).right().padLeft(24);
            table.row();
            table.add(new Image(theme.solid(dim(Theme.BONE, 0.13f))))
                    .colspan(3).growX().height(1).padTop(9).padBottom(9);
            table.row();
        }
        return table;
    }

    private static Map<String, Instant> readSafely(AchievementStore store) {
        try {
            Map<String, Instant> earned = new HashMap<>();
            for (UnlockedAchievement unlocked : store.readAll()) {
                earned.put(unlocked.id(), unlocked.earnedAt());
            }
            return earned;
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "failed to read achievements", e);
            return Map.of();
        }
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
