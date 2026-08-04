package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.tomer.scoundrel.ScoundrelGame;

/**
 * A developer-only sprite inspector, opened with F9 and closed with Escape. It
 * exists to answer the verify questions in HANDOFF.md §11 — is the art crisp at
 * an integer scale, are all 31 objects on their right ranks, do the idle cycles
 * run — without needing a run of the actual game to reach them.
 *
 * <p>Not reachable from any menu, and drawn with a plain batch rather than
 * Scene2D: it is a measuring instrument, not part of the game.
 */
public final class SpriteLab extends ScreenAdapter {

    /** The stage background from HANDOFF.md §6 — dark enough to show fringing. */
    private static final Color BACKDROP = Color.valueOf("100c09");

    private static final String SUBJECT = "creature_02_cellar_rat_clubs";
    private static final int SCALE = 2;

    private final ScoundrelGame game;
    private final Theme theme;
    private final Sprites sprites;
    private final FitViewport viewport;
    private final SpriteBatch batch;

    public SpriteLab(ScoundrelGame game, Theme theme, Sprites sprites) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        // §5: one fixed virtual resolution, so every number in HANDOFF.md is
        // literal and the art is guaranteed to land on whole pixels.
        this.viewport = new FitViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.batch = new SpriteBatch();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showTitle();
            return;
        }
        ScreenUtils.clear(BACKDROP);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        TextureRegion region = sprites.region(SUBJECT);
        int drawn = Sprites.SIZE * SCALE;
        // Whole-pixel placement (§4). These divide evenly at 1280×720, but the
        // rounding is what makes that a guarantee rather than a coincidence.
        int x = Math.round((Theme.WORLD_WIDTH - drawn) / 2f);
        int y = Math.round((Theme.WORLD_HEIGHT - drawn) / 2f);
        batch.draw(region, x, y, drawn, drawn);

        theme.body.setColor(Theme.BONE);
        theme.body.draw(batch, SUBJECT + "  ×" + SCALE + "  (" + drawn + "px)", 40, 680);
        theme.body.draw(batch, "Esc to leave", 40, 48);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
