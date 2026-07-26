package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.tomer.scoundrel.ScoundrelGame;

import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;
import static com.tomer.scoundrel.screens.Widgets.mutedButton;
import static com.tomer.scoundrel.screens.Widgets.torchButton;

/**
 * The launch screen and navigation anchor: every future menu (achievements,
 * variants) hangs off this list of buttons.
 */
public final class TitleScreen extends ScreenAdapter {

    private final Stage stage;

    public TitleScreen(ScoundrelGame game, Theme theme) {
        this(game, theme, false);
    }

    /** {@code offerTutorial} pops the one-time first-run prompt over the menu. */
    public TitleScreen(ScoundrelGame game, Theme theme, boolean offerTutorial) {
        stage = new Stage(new FitViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT));
        stage.addActor(new Backdrop(theme));
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        root.add(label("SCOUNDREL", theme.display, Theme.BONE)).padBottom(56);
        root.row();
        TextButton newGame = torchButton(theme, "New game");
        newGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showModeSelect();
            }
        });
        root.add(newGame).width(240).padBottom(12);
        root.row();
        TextButton howToPlay = torchButton(theme, "How to play");
        howToPlay.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTutorial();
            }
        });
        root.add(howToPlay).width(240).padBottom(12);
        root.row();
        TextButton records = torchButton(theme, "Records");
        records.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showRecords();
            }
        });
        root.add(records).width(240).padBottom(12);
        root.row();
        TextButton trophies = torchButton(theme, "Trophies");
        trophies.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTrophies();
            }
        });
        root.add(trophies).width(240);
        root.row();
        root.add(label("Scoundrel was designed by Zach Gage & Kurt Bieg — an unofficial fan implementation",
                theme.small, dim(Theme.BONE, 0.4f))).padTop(72);

        if (offerTutorial) {
            stage.addActor(firstRunPrompt(game, theme));
        }
    }

    /**
     * The one-time welcome: shown over the menu on the very first launch. Both
     * choices mark the tutorial seen, so it never nags twice — Play jumps
     * straight in, Maybe later dismisses to the menu.
     */
    private Actor firstRunPrompt(ScoundrelGame game, Theme theme) {
        Table overlay = new Table();
        overlay.setFillParent(true);
        // Modal: a background alone does not block input on a childrenOnly Table.
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(theme.solid(dim(Theme.SOOT, 0.85f)));
        overlay.add(label("NEW HERE?", theme.title, Theme.TORCHLIGHT)).padBottom(10);
        overlay.row();
        Label blurb = label("Scoundrel has a few rules worth knowing — a short guided run walks "
                + "through all of them. Take it now, or find it any time under \"How to play\".",
                theme.body, Theme.BONE);
        blurb.setWrap(true);
        blurb.setAlignment(Align.center);
        overlay.add(blurb).width(560).padBottom(26);
        overlay.row();
        TextButton play = torchButton(theme, "Play tutorial");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTutorial();
            }
        });
        overlay.add(play).padBottom(10);
        overlay.row();
        TextButton later = mutedButton(theme, "Maybe later");
        later.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.markTutorialSeen();
                overlay.remove();
            }
        });
        overlay.add(later);
        return overlay;
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
