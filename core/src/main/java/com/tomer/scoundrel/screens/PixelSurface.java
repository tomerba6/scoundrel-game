package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/**
 * An offscreen surface exactly the size of the design resolution. The board is
 * drawn onto it at 1:1 and the finished image is then scaled to the window in a
 * single step.
 *
 * <p>Without it every draw call is scaled independently, and at a scale that is
 * not a whole number they do not agree. At 1920×1080 the viewport fits at ×1.5,
 * and the five identical 2px stems of the word AVOID came out 3, 2, 2, 3 and 2
 * screen pixels wide — each quad rounded on its own, so identical strokes
 * disagreed by half their width. Drawing them onto one integer grid first makes
 * every 2px feature exactly 3 screen pixels, because the whole image is
 * resampled once with one phase instead of a few hundred times with many.
 *
 * <p>It cannot make ×1.5 pixel-perfect — nothing can, since a single design
 * pixel has no whole number of screen pixels to occupy. Features one pixel
 * across still land on one screen pixel or two depending where they fall. What
 * changes is that the choice is now made consistently across the whole frame
 * rather than per draw call, which is the difference between a coarse image and
 * a scruffy one.
 */
final class PixelSurface implements Disposable {

    private final FrameBuffer buffer;
    private final TextureRegion region;
    private final OrthographicCamera camera = new OrthographicCamera();

    PixelSurface(int width, int height) {
        buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
        // Nearest, obviously: this texture is the pixel art, and the one scale
        // it will ever undergo is the one that happens when it is drawn.
        buffer.getColorBufferTexture().setFilter(
                Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        region = new TextureRegion(buffer.getColorBufferTexture());
        // A frame buffer's texture comes out bottom-up; SpriteBatch expects the
        // other way round, so the region is flipped once here rather than the
        // image being upside down everywhere.
        region.flip(false, true);
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    /** Everything drawn until {@link #end} lands on the surface's own grid. */
    void begin(Color clearTo) {
        buffer.begin();
        ScreenUtils.clear(clearTo);
    }

    void end() {
        buffer.end();
    }

    /** The projection to draw with while the surface is bound: 1:1, y-up. */
    com.badlogic.gdx.math.Matrix4 projection() {
        return camera.combined;
    }

    /** The finished image, to be drawn at world size exactly once. */
    TextureRegion region() {
        return region;
    }

    @Override
    public void dispose() {
        buffer.dispose();
    }
}
