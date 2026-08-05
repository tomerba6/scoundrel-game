package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.model.CardType;

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

    /** One of each palette, plus a repeat, so all three ramps are on screen. */
    private static final CardType[] ROW = {
        CardType.MONSTER, CardType.WEAPON, CardType.POTION, CardType.MONSTER,
    };

    private final ScoundrelGame game;
    private final Theme theme;
    private final Sprites sprites;
    private final CardFrame cardFrame;
    private final PixelViewport viewport;
    private final SpriteBatch batch;

    public SpriteLab(ScoundrelGame game, Theme theme, Sprites sprites) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        this.cardFrame = new CardFrame(theme);
        // §5: one fixed virtual resolution, so every number in HANDOFF.md is
        // literal and the art is guaranteed to land on whole pixels.
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
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

        // The four-slot room row at the §9 geometry, for comparing against the
        // mock's BOARD tab. Wells are empty until step 5 puts sprites on cards.
        for (int i = 0; i < ROW.length; i++) {
            cardFrame.draw(batch, ROW[i], CardArt.slotX(i), CardArt.SLOT_Y);
        }

        // The lone sprite from step 2, kept above the row as the crispness check.
        TextureRegion region = sprites.region(SUBJECT);
        int drawn = Sprites.SIZE * SCALE;
        // Whole-pixel placement (§4). These divide evenly at 1280×720, but the
        // rounding is what makes that a guarantee rather than a coincidence.
        int x = Math.round((Theme.WORLD_WIDTH - drawn) / 2f);
        batch.draw(region, x, CardArt.toWorldY(48, drawn), drawn, drawn);

        theme.body.setColor(Theme.BONE);
        theme.body.draw(batch, SUBJECT + "  ×" + SCALE + "  (" + drawn + "px)", 40, 700);
        theme.body.draw(batch, "card frame: plate + 2px bevels + well", 40, 676);
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
