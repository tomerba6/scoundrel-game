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

/**
 * The frame every navigable screen draws inside: the 1280×720 surface, the
 * viewport that scales it to the window, the torchlit backdrop behind it, and
 * the press gesture that turns clicks into targets.
 *
 * <p><b>{@link #render(float)} is final, and that is the point of this class.</b>
 * Activating a target navigates, and navigating disposes this screen along with
 * its batch and surface — drawing afterwards reads freed native memory and takes
 * the JVM down with an {@code EXCEPTION_ACCESS_VIOLATION} rather than throwing
 * something a test could catch. That guard used to be hand-copied into five
 * screens. Here it is written once, above a {@link #drawContent} a subclass
 * cannot reach until the check has passed.
 *
 * <p>The two passes are equally load-bearing. Everything is drawn at 1:1 onto
 * the surface, and that one image is scaled to the window once — drawn straight
 * to the window instead, every quad rounds separately and identical features
 * disagree by a pixel. See {@code HANDOFF.md} §4.
 *
 * <p>{@code SpriteLab} deliberately does not extend this: it has no press
 * gesture and no backdrop, and it is a developer tool rather than a screen
 * anyone navigates to.
 */
public abstract class PixelScreen extends ScreenAdapter {

    /** Hoisted rather than built per frame, as {@code GameScreen} already did. */
    private static final Color CLEAR = new Color((CardArt.BACKDROP << 8) | 0xff);

    protected final ScoundrelGame game;
    protected final Theme theme;
    protected final PixelViewport viewport;
    protected final SpriteBatch batch = new SpriteBatch();
    protected final PixelSurface surface;
    protected final Backdrop backdrop;
    protected final Chrome chrome;
    protected final PressGesture press = new PressGesture();

    protected PixelScreen(ScoundrelGame game, Theme theme) {
        this.game = game;
        this.theme = theme;
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.surface = new PixelSurface((int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT);
        this.backdrop = new Backdrop(theme);
        this.chrome = new Chrome(theme);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new FrameInput());
    }

    @Override
    public final void render(float delta) {
        // A target acts here rather than the instant it comes up, once its
        // plate has been down long enough to have been seen.
        int fired = press.advance(delta);
        if (fired != PressGesture.NONE) {
            activate(fired);
            if (game.getScreen() != this) {
                return;
            }
        }
        advance(delta);

        // Everything at 1:1 on the surface's own grid first.
        surface.begin(CLEAR);
        batch.setProjectionMatrix(surface.projection());
        batch.begin();
        backdrop.render(batch, backdropLight());
        drawContent(delta);
        batch.end();
        surface.end();

        // Then that one image to the window, in one scale.
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        surface.draw(batch, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        batch.end();
    }

    @Override
    public final void resize(int width, int height) {
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

    /** A window-space point in the 1280×720 design space. */
    protected final Vector2 unproject(int screenX, int screenY) {
        return viewport.unproject(new Vector2(screenX, screenY));
    }

    /** Moves this screen's clocks on. The backdrop always; more if overridden. */
    protected void advance(float delta) {
        backdrop.advance(delta);
    }

    /** How brightly the torch burns, 0..1. Only the death gutters it. */
    protected float backdropLight() {
        return 1f;
    }

    /**
     * Where ESC goes. Every screen with a back plate leaves to the title; the
     * title itself overrides this, having nowhere to go.
     */
    protected void escape() {
        game.showTitle();
    }

    /** Keys beyond ESC. Return true if handled. */
    protected boolean keyPressed(int keycode) {
        return false;
    }

    protected abstract void drawContent(float delta);

    /**
     * What a window-space point is on, as a target id, or
     * {@link PressGesture#NONE}.
     */
    protected abstract int hit(int screenX, int screenY);

    /** What a released target does. Only reached once its press has been seen. */
    protected abstract void activate(int target);

    private final class FrameInput extends InputAdapter {
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
                escape();
                return true;
            }
            return keyPressed(keycode);
        }
    }
}
