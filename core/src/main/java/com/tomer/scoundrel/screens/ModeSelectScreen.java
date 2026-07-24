package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.rules.GameMode;
import com.tomer.scoundrel.rules.GameModes;

import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;
import static com.tomer.scoundrel.screens.Widgets.torchButton;

/**
 * The mode picker: every shipped {@link GameMode} with what it changes and
 * whether its runs count toward trophies. Choosing one starts the run at once.
 * Purely a menu — the modes and their rules all come from the GameModes catalog,
 * so a new mode appears here with no change to this screen.
 */
public final class ModeSelectScreen extends ScreenAdapter {

    private final Stage stage;

    public ModeSelectScreen(ScoundrelGame game, Theme theme) {
        stage = new Stage(new FitViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT));

        Table root = new Table();
        root.setFillParent(true);
        root.top().pad(28, 56, 24, 56);
        stage.addActor(root);

        root.add(label("NEW GAME", theme.title, Theme.BONE)).padBottom(6);
        root.row();
        root.add(label("Choose your descent.", theme.body, dim(Theme.BONE, 0.6f))).padBottom(22);
        root.row();
        root.add(modeTable(game, theme)).growX().top();
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

    private Table modeTable(ScoundrelGame game, Theme theme) {
        Table table = new Table();
        for (GameMode mode : GameModes.all()) {
            TextButton play = torchButton(theme, mode.title());
            play.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.showGame(mode);
                }
            });
            table.add(play).left().width(240).padRight(24);
            table.add(label(mode.description(), theme.body, dim(Theme.BONE, 0.8f)))
                    .left().expandX();
            // Achievements are Standard-only; say so where the choice is made.
            table.add(label(mode.tracksAchievements() ? "trophies count" : "no trophies",
                    theme.small, dim(Theme.BONE, mode.tracksAchievements() ? 0.5f : 0.3f)))
                    .right().padLeft(24);
            table.row();
            table.add(new Image(theme.solid(dim(Theme.BONE, 0.13f))))
                    .colspan(3).growX().height(1).padTop(9).padBottom(9);
            table.row();
        }
        return table;
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
