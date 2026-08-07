package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.runs.HighScores;
import com.tomer.scoundrel.runs.RunLog;
import com.tomer.scoundrel.runs.RunRecord;
import com.tomer.scoundrel.runs.RunTotals;

import java.util.List;
import java.util.OptionalInt;

/**
 * The launch screen and navigation anchor. Drawn in immediate mode from the
 * menu kit ({@link Chrome}, {@link ScreenArt}) rather than laid out by Scene2D,
 * for the same reason the board is: the art is specified as pixels at fixed
 * positions, and a layout engine's job is to compute positions.
 *
 * <p>It is the only menu carrying a sprite — The Debt, idling at ×3 in its
 * well. HANDOFF §11 is explicit that this is what makes the screen read as
 * <em>this</em> game rather than a card game in general, so it is not
 * decoration and should not be cut for tidiness.
 */
public final class TitleScreen extends ScreenAdapter {

    private static final String WORDMARK = "SCOUNDREL";
    /**
     * Forty-four, not the mock's fifty-two. The reference render assumes a
     * standard deck; this game removes the six red face cards and the two red
     * aces from one, leaving 44 — asserted by
     * {@code StandardDeckTest.deckHasExactly44Cards}. A title screen that
     * miscounts its own deck is the first thing a player would catch.
     */
    private static final String EYEBROW = "FORTY-FOUR CARDS · ONE DESCENT";
    private static final String CAPTION = "WHAT WAITS AT THE BOTTOM";
    private static final String CREDIT =
            "DESIGNED BY ZACH GAGE & KURT BIEG — AN UNOFFICIAL IMPLEMENTATION";
    /**
     * The Debt: the Ace, and the thing at the bottom of the dungeon. The clubs
     * drawing, which is the red-coated one the reference render uses — the two
     * suits are separate drawings, not a recolour.
     */
    private static final String PORTRAIT_STEM = "creature_14_the_debt_clubs_idle";

    private final ScoundrelGame game;
    private final Theme theme;
    private final Sprites sprites;
    private final RunLog runLog;

    private final PixelViewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final PixelSurface surface;
    private final Backdrop backdrop;
    private final Chrome chrome;

    private final List<Entry> menu;
    private final String bestLine;
    private float elapsed;
    /** The one-time first-run prompt, over the menu. */
    private boolean offeringTutorial;

    /** One menu row: what it says and what it does. */
    private record Entry(String label, Runnable action) {
    }

    public TitleScreen(ScoundrelGame game, Theme theme, Sprites sprites, RunLog runLog) {
        this(game, theme, sprites, runLog, false);
    }

    /** {@code offerTutorial} pops the one-time first-run prompt over the menu. */
    public TitleScreen(ScoundrelGame game, Theme theme, Sprites sprites, RunLog runLog,
                       boolean offerTutorial) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        this.runLog = runLog;
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.surface = new PixelSurface((int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT);
        this.backdrop = new Backdrop(theme);
        this.chrome = new Chrome(theme);
        this.offeringTutorial = offerTutorial;
        this.menu = List.of(
                new Entry("NEW GAME", game::showModeSelect),
                new Entry("HOW TO PLAY", game::showTutorial),
                new Entry("THE LEDGER", game::showRecords),
                new Entry("TROPHIES", game::showTrophies));
        this.bestLine = readBestLine();
    }

    /**
     * The line under the wordmark. Reading the log can fail on a corrupt file
     * and a menu must still come up, so a failure simply says nothing rather
     * than taking the screen down with it.
     */
    private String readBestLine() {
        try {
            List<RunRecord> all = runLog.readAll();
            if (all.isEmpty()) {
                return "NO RUNS YET";
            }
            // Cleared, not finished. Finishing a run is not an achievement —
            // every run finishes, most of them by dying — so counting them
            // flatters the number and says nothing. Getting out says something.
            int cleared = RunTotals.of(all).wins();
            OptionalInt best = HighScores.best(all);
            String runs = cleared == 1 ? "1 RUN CLEARED" : cleared + " RUNS CLEARED";
            return best.isPresent() ? "BEST " + best.getAsInt() + " · " + runs : runs;
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "could not read the run log for the title", e);
            return "";
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new MenuInput());
    }

    /** The menu takes presses; the prompt, while it is up, takes them first. */
    private final class MenuInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            Vector2 point = viewport.unproject(new Vector2(screenX, screenY));
            if (offeringTutorial) {
                int picked = ScreenArt.buttonAt(promptButtonX(), 2, point.x, point.y);
                if (picked == 0) {
                    game.showTutorial();
                } else if (picked == 1) {
                    game.markTutorialSeen();
                    offeringTutorial = false;
                }
                return true; // modal: nothing behind it responds
            }
            int picked = ScreenArt.buttonAt(ScreenArt.COLUMN_X, menu.size(), point.x, point.y);
            if (picked >= 0) {
                menu.get(picked).action().run();
                return true;
            }
            return false;
        }
    }

    @Override
    public void render(float delta) {
        elapsed += delta;
        backdrop.advance(delta);

        surface.begin(new Color((CardArt.BACKDROP << 8) | 0xff));
        batch.setProjectionMatrix(surface.projection());
        batch.begin();
        backdrop.render(batch, 1f);
        drawPortrait();
        drawColumn();
        if (offeringTutorial) {
            drawPrompt();
        }
        batch.end();
        surface.end();

        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        surface.draw(batch, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        batch.end();
    }

    /** The Debt in its well, breathing on the same 6 fps clock the board uses. */
    private void drawPortrait() {
        chrome.frame(batch, ScreenArt.WELL_X, ScreenArt.WELL_Y,
                ScreenArt.wellW(), ScreenArt.wellH());
        chrome.face(batch, ScreenArt.fieldX(), ScreenArt.fieldY(),
                ScreenArt.FIELD, ScreenArt.FIELD, ScreenArt.PORTRAIT_FIELD);
        chrome.face(batch, ScreenArt.fieldX(), ScreenArt.captionY(),
                ScreenArt.FIELD, ScreenArt.CAPTION_H, ScreenArt.FRAME);

        Array<TextureRegion> frames = sprites.frames(PORTRAIT_STEM);
        TextureRegion frame = frames.get(IdleCycle.frameIndex(elapsed, 0f, frames.size, true));
        batch.draw(frame, ScreenArt.portraitX(),
                CardArt.toWorldY(ScreenArt.portraitY(), ScreenArt.PORTRAIT),
                ScreenArt.PORTRAIT, ScreenArt.PORTRAIT);

        chrome.centredOn(batch, theme.pixelSmall, CAPTION,
                ScreenArt.fieldX() + ScreenArt.FIELD / 2,
                ScreenArt.captionY() + 6, ScreenArt.CAPTION, 1f);
    }

    private void drawColumn() {
        int x = ScreenArt.COLUMN_X;
        chrome.text(batch, theme.pixelLabel, EYEBROW, x, ScreenArt.EYEBROW_TOP,
                ScreenArt.HEADING);

        // 52px is the 26px face at x2 -- a whole multiple, so the wordmark is
        // pixel-exact without a sixth font size. The shadow is the same glyphs
        // moved down four whole pixels, not a blur.
        theme.pixelTitle.getData().setScale(2f);
        chrome.text(batch, theme.pixelTitle, WORDMARK, x,
                ScreenArt.WORDMARK_TOP + ScreenArt.WORDMARK_SHADOW_DY,
                ScreenArt.WORDMARK_SHADOW);
        chrome.text(batch, theme.pixelTitle, WORDMARK, x, ScreenArt.WORDMARK_TOP,
                ScreenArt.BODY);
        theme.pixelTitle.getData().setScale(1f);

        chrome.rule(batch, x, ScreenArt.TITLE_RULE_Y, ScreenArt.BUTTON_W,
                ScreenArt.TITLE_RULE);
        chrome.text(batch, theme.pixelLabel, bestLine, x, ScreenArt.BEST_TOP,
                ScreenArt.BODY, ScreenArt.BODY_ALPHA);

        for (int i = 0; i < menu.size(); i++) {
            // The first is gold, the rest dark: one thing to do, and three
            // places to look at first.
            chrome.plate(batch, x, ScreenArt.buttonY(i), ScreenArt.BUTTON_W,
                    ScreenArt.BUTTON_H, menu.get(i).label(),
                    i == 0 ? Chrome.Plate.GOLD : Chrome.Plate.DARK);
        }

        chrome.centredOn(batch, theme.pixelSmall, CREDIT, (int) (Theme.WORLD_WIDTH / 2),
                ScreenArt.CREDIT_TOP, ScreenArt.BODY, ScreenArt.CREDIT_ALPHA);
    }

    // --- the first-run prompt ---

    private static final int PROMPT_W = 600;
    private static final int PROMPT_X = (int) (Theme.WORLD_WIDTH - PROMPT_W) / 2;
    private static final int PROMPT_Y = 232;
    private static final int PROMPT_H = 260;

    private static int promptButtonX() {
        return (int) (Theme.WORLD_WIDTH - ScreenArt.BUTTON_W) / 2;
    }

    /**
     * The one-time welcome, shown over the menu on the very first launch. Both
     * choices mark the tutorial seen, so it never nags twice.
     *
     * <p>It is not in the mock — the reference has no first run — so it is built
     * from the same five parts as everything else rather than invented.
     */
    private void drawPrompt() {
        chrome.frame(batch, PROMPT_X, PROMPT_Y, PROMPT_W, PROMPT_H);
        chrome.face(batch, PROMPT_X + ScreenArt.THICK, PROMPT_Y + ScreenArt.THICK,
                PROMPT_W - 2 * ScreenArt.THICK, PROMPT_H - 2 * ScreenArt.THICK,
                ScreenArt.FACE_PANEL);
        chrome.centredOn(batch, theme.pixelBody, "NEW HERE?",
                (int) (Theme.WORLD_WIDTH / 2), PROMPT_Y + 28, ScreenArt.HEADING, 1f);
        chrome.centredOn(batch, theme.pixelLabel,
                "SCOUNDREL HAS A FEW RULES WORTH KNOWING.",
                (int) (Theme.WORLD_WIDTH / 2), PROMPT_Y + 66,
                ScreenArt.BODY, ScreenArt.BODY_ALPHA);
        chrome.centredOn(batch, theme.pixelLabel,
                "A SHORT GUIDED RUN WALKS THROUGH ALL OF THEM.",
                (int) (Theme.WORLD_WIDTH / 2), PROMPT_Y + 88,
                ScreenArt.BODY, ScreenArt.BODY_ALPHA);
        // Reusing the menu's own button geometry, so the prompt's two choices
        // are the same shape and pitch as the four behind them.
        chrome.plate(batch, promptButtonX(), ScreenArt.buttonY(0), ScreenArt.BUTTON_W,
                ScreenArt.BUTTON_H, "PLAY TUTORIAL", Chrome.Plate.GOLD);
        chrome.plate(batch, promptButtonX(), ScreenArt.buttonY(1), ScreenArt.BUTTON_W,
                ScreenArt.BUTTON_H, "MAYBE LATER", Chrome.Plate.DARK);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        surface.dispose();
        batch.dispose();
    }
}
