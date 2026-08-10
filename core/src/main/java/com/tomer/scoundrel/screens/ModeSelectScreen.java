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
     * Which panel wears the gold frame. It starts on the first — the state the
     * reference render shows — and thereafter stays wherever the pointer last
     * put it, so the frame is doing the work a hover glow would do elsewhere.
     * §11 forbids the glow, not the feedback.
     */
    private int selected;
    /**
     * Where the pointer was when this screen opened, and whether it has moved
     * since. The click that opens this screen is the title's NEW GAME button,
     * which sits at a point that lands inside the third panel — so without
     * this the screen opens with Frail lit for no reason the player can see.
     * The pointer has to actually move before it means anything.
     */
    private int restingX = -1;
    private int restingY = -1;
    private boolean pointerMoved;
    /** Which panel or plate is held, and when a release has earned the right to act. */
    private final PressGesture press = new PressGesture();

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

    /**
     * What a window-space point is on: a panel by index, the header's back plate
     * as {@link ScreenArt#BACK}, or nothing. One id space, because the gesture
     * matches a release against a press by equality.
     */
    private int hit(int screenX, int screenY) {
        Vector2 point = viewport.unproject(new Vector2(screenX, screenY));
        if (ScreenArt.backContains(point.x, point.y)) {
            return ScreenArt.BACK;
        }
        return ScreenArt.panelAt(modes.size(), point.x, point.y);
    }

    /** What a released target does. Only reached once its press has been seen. */
    private void activate(int target) {
        if (target == ScreenArt.BACK) {
            game.showTitle();
            return;
        }
        game.showGame(modes.get(target));
    }

    /**
     * Panels and the back plate act on release, and only where the press began —
     * see {@link PressGesture}. The keys do not: a key has no travel to slide
     * off, so there is nothing to take back.
     */
    private final class PickerInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            return press.press(hit(screenX, screenY));
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            press.moveOver(hit(screenX, screenY));
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            return press.release(hit(screenX, screenY));
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

        // A target acts here rather than the instant it comes up, once it has
        // been down long enough to have been seen. Both of them navigate, and
        // navigating disposes this screen along with its batch and surface — so
        // nothing may be drawn afterwards.
        int fired = press.advance(delta);
        if (fired != PressGesture.NONE) {
            activate(fired);
            if (game.getScreen() != this) {
                return;
            }
        }
        followPointer();

        surface.begin(new Color((CardArt.BACKDROP << 8) | 0xff));
        batch.setProjectionMatrix(surface.projection());
        batch.begin();
        backdrop.render(batch, 1f);
        chrome.header(batch, "NEW GAME", "CHOOSE YOUR DESCENT",
                press.sunk() == ScreenArt.BACK);
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
     * The gold frame follows the pointer and stays where it was left — moving
     * off the panels does not reset it, because the last thing you looked at is
     * still the thing you were considering.
     */
    private void followPointer() {
        int x = Gdx.input.getX();
        int y = Gdx.input.getY();
        if (!pointerMoved) {
            if (restingX < 0) {
                restingX = x;
                restingY = y;
            }
            if (x == restingX && y == restingY) {
                return; // still sitting where the opening click left it
            }
            pointerMoved = true;
        }
        Vector2 point = viewport.unproject(new Vector2(x, y));
        int over = ScreenArt.panelAt(modes.size(), point.x, point.y);
        if (over >= 0) {
            selected = over;
        }
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
        boolean down = press.sunk() == index;
        // A panel is the button here, so it takes the plate's press treatment:
        // the face on its shadowed step, the bevel inverted inside the frame,
        // and everything written on it travelling one bevel into the recess.
        // The frame itself does not move — 1200px of panel shifting bodily would
        // eat the 14px gap to the one below and read as a layout fault rather
        // than a press.
        int travel = down ? ScreenArt.SINK : 0;
        int cx = x + travel;
        int cy = y + travel;

        chrome.face(batch, x, y, ScreenArt.PANEL_W, ScreenArt.PANEL_H,
                down ? ScreenArt.DARK_PRESSED : ScreenArt.FACE_PANEL);
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
        if (down) {
            // Inside the frame, so a selected panel keeps its gold edge and
            // gains the recess rather than choosing between them.
            int t = ScreenArt.THICK;
            chrome.bevel(batch, x + t, y + t, ScreenArt.PANEL_W - 2 * t,
                    ScreenArt.PANEL_H - 2 * t, ScreenArt.DARK_DARK, ScreenArt.DARK_LIGHT);
        }

        // The number key, in its own well.
        int wellX = cx + ScreenArt.WELL_DX;
        int wellY = cy + ScreenArt.WELL_DY;
        chrome.face(batch, wellX, wellY, ScreenArt.WELL_SIZE, ScreenArt.WELL_SIZE,
                ScreenArt.FRAME);
        chrome.centred(batch, theme.pixelLabel, String.valueOf(index + 1), wellX, wellY,
                ScreenArt.WELL_SIZE, ScreenArt.WELL_SIZE,
                on ? ScreenArt.GOLD : ScreenArt.WELL_DIGIT_OFF, 1f);

        String name = mode.title().toUpperCase(Locale.ROOT);
        int nameX = cx + ScreenArt.NAME_DX;
        chrome.text(batch, theme.pixelBody, name, nameX, cy + ScreenArt.NAME_DY,
                ScreenArt.BODY);

        // Achievements are Standard-only; say so where the choice is made.
        boolean trophies = mode.tracksAchievements();
        String badge = trophies ? "TROPHIES COUNT" : "NO TROPHIES";
        int badgeX = nameX + chrome.width(theme.pixelBody, name) + ScreenArt.BADGE_GAP;
        int badgeW = chrome.width(theme.pixelLabel, badge) + 2 * ScreenArt.BADGE_PAD_X;
        chrome.face(batch, badgeX, cy + ScreenArt.BADGE_DY, badgeW, ScreenArt.BADGE_H,
                trophies ? ScreenArt.BADGE_ON : ScreenArt.BADGE_OFF);
        chrome.centred(batch, theme.pixelLabel, badge, badgeX, cy + ScreenArt.BADGE_DY,
                badgeW, ScreenArt.BADGE_H,
                trophies ? ScreenArt.GOLD_LABEL : ScreenArt.BADGE_OFF_LABEL, 1f);

        chrome.textRight(batch, theme.pixelLabel,
                "START " + mode.ruleset().startingHealth(),
                ScreenArt.panelRight() - ScreenArt.START_INSET + travel,
                cy + ScreenArt.START_DY, ScreenArt.START_COLOUR, 1f);

        chrome.text(batch, theme.pixelLabel, mode.description().toUpperCase(Locale.ROOT),
                cx + ScreenArt.DESC_DX, cy + ScreenArt.DESC_DY,
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
