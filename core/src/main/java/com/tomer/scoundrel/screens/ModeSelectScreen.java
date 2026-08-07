package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.rules.GameMode;
import com.tomer.scoundrel.rules.GameModes;

import java.util.List;
import java.util.Locale;

/**
 * The mode picker: every shipped {@link GameMode} with what it changes and
 * whether its runs count toward trophies. Choosing one starts the run at once.
 *
 * <p>Purely a menu — the modes and their rules all come from the GameModes
 * catalog, so a fourth mode appears here with no change to this screen. The
 * panels are laid out from {@link ScreenArt} and drawn with {@link Chrome},
 * like every other screen outside the board.
 */
public final class ModeSelectScreen extends ScreenAdapter {

    private final ScoundrelGame game;
    private final Theme theme;

    private final PixelViewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final PixelSurface surface;
    private final Backdrop backdrop;
    private final Chrome chrome;

    private final List<GameMode> modes = GameModes.all();
    /**
     * Which panel wears the gold frame. It starts on the first — which is the
     * state the reference render shows — and follows the pointer, so the frame
     * is doing the work a hover glow would do elsewhere. §11 forbids the glow,
     * not the feedback.
     */
    private int selected;

    public ModeSelectScreen(ScoundrelGame game, Theme theme) {
        this.game = game;
        this.theme = theme;
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.surface = new PixelSurface((int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT);
        this.backdrop = new Backdrop(theme);
        this.chrome = new Chrome(theme);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new PickerInput());
    }

    private final class PickerInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            Vector2 point = viewport.unproject(new Vector2(screenX, screenY));
            if (ScreenArt.backContains(point.x, point.y)) {
                game.showTitle();
                return true;
            }
            int picked = ScreenArt.panelAt(modes.size(), point.x, point.y);
            if (picked >= 0) {
                game.showGame(modes.get(picked));
                return true;
            }
            return false;
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                game.showTitle();
                return true;
            }
            // The number in each panel's well is not decoration.
            int index = keycode - Input.Keys.NUM_1;
            if (index >= 0 && index < modes.size()) {
                game.showGame(modes.get(index));
                return true;
            }
            return false;
        }
    }

    @Override
    public void render(float delta) {
        backdrop.advance(delta);
        followPointer();

        surface.begin(new Color((CardArt.BACKDROP << 8) | 0xff));
        batch.setProjectionMatrix(surface.projection());
        batch.begin();
        backdrop.render(batch, 1f);
        chrome.header(batch, "NEW GAME", "CHOOSE YOUR DESCENT");
        for (int i = 0; i < modes.size(); i++) {
            drawPanel(modes.get(i), i);
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

    /**
     * The gold frame follows the pointer, and falls back to the first panel
     * when it is over none of them — which is the state the reference render
     * shows, and stops the selection being wherever the click that opened this
     * screen happened to leave the cursor.
     */
    private void followPointer() {
        Vector2 point = viewport.unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        int over = ScreenArt.panelAt(modes.size(), point.x, point.y);
        selected = Math.max(over, 0);
    }

    /**
     * One mode. The <b>only</b> difference between selected and not is the
     * frame's colour — §11 is explicit that this is the whole treatment: no
     * glow, no scale, no change of face.
     */
    private void drawPanel(GameMode mode, int index) {
        int y = ScreenArt.panelY(index);
        int x = ScreenArt.PANEL_X;
        boolean on = index == selected;

        chrome.face(batch, x, y, ScreenArt.PANEL_W, ScreenArt.PANEL_H, ScreenArt.FACE_PANEL);
        if (on) {
            chrome.rule(batch, x, y, ScreenArt.PANEL_W, ScreenArt.GOLD);
            chrome.rule(batch, x, y + ScreenArt.PANEL_H - ScreenArt.THICK,
                    ScreenArt.PANEL_W, ScreenArt.GOLD);
            chrome.face(batch, x, y, ScreenArt.THICK, ScreenArt.PANEL_H, ScreenArt.GOLD);
            chrome.face(batch, x + ScreenArt.PANEL_W - ScreenArt.THICK, y,
                    ScreenArt.THICK, ScreenArt.PANEL_H, ScreenArt.GOLD);
        } else {
            chrome.frame(batch, x, y, ScreenArt.PANEL_W, ScreenArt.PANEL_H);
        }

        // The number key, in its own well.
        int wellX = x + ScreenArt.WELL_DX;
        int wellY = y + ScreenArt.WELL_DY;
        chrome.face(batch, wellX, wellY, ScreenArt.WELL_SIZE, ScreenArt.WELL_SIZE,
                ScreenArt.FRAME);
        chrome.centred(batch, theme.pixelLabel, String.valueOf(index + 1), wellX, wellY,
                ScreenArt.WELL_SIZE, ScreenArt.WELL_SIZE,
                on ? ScreenArt.GOLD : ScreenArt.WELL_DIGIT_OFF, 1f);

        String name = mode.title().toUpperCase(Locale.ROOT);
        int nameX = x + ScreenArt.NAME_DX;
        chrome.text(batch, theme.pixelBody, name, nameX, y + ScreenArt.NAME_DY,
                ScreenArt.BODY);

        // Achievements are Standard-only; say so where the choice is made.
        boolean trophies = mode.tracksAchievements();
        String badge = trophies ? "TROPHIES COUNT" : "NO TROPHIES";
        int badgeX = nameX + chrome.width(theme.pixelBody, name) + ScreenArt.BADGE_GAP;
        int badgeW = chrome.width(theme.pixelLabel, badge) + 2 * ScreenArt.BADGE_PAD_X;
        chrome.face(batch, badgeX, y + ScreenArt.BADGE_DY, badgeW, ScreenArt.BADGE_H,
                trophies ? ScreenArt.BADGE_ON : ScreenArt.BADGE_OFF);
        chrome.centred(batch, theme.pixelLabel, badge, badgeX, y + ScreenArt.BADGE_DY,
                badgeW, ScreenArt.BADGE_H,
                trophies ? ScreenArt.GOLD_LABEL : ScreenArt.BADGE_OFF_LABEL, 1f);

        chrome.textRight(batch, theme.pixelLabel,
                "START " + mode.ruleset().startingHealth(),
                ScreenArt.panelRight() - ScreenArt.START_INSET, y + ScreenArt.START_DY,
                ScreenArt.START_COLOUR, 1f);

        chrome.text(batch, theme.pixelLabel, mode.description().toUpperCase(Locale.ROOT),
                x + ScreenArt.DESC_DX, y + ScreenArt.DESC_DY,
                ScreenArt.BODY, ScreenArt.BODY_ALPHA);
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
