package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * The three pieces a weapon kill needs beyond the sprite itself: the two halves
 * the card is cleaved into, and the bone bar that cuts it.
 *
 * <p>All three are built as pixel masks at load rather than drawn with rotation
 * or a shader. The bar in particular is generated already diagonal instead of
 * being a straight bar turned 56° at draw time — rotating it would resample its
 * edges into colours that are not on the ramp, which is the one thing the art
 * cannot tolerate.
 */
final class SliceArt implements Disposable {

    /** The cleaved card's cut faces, from the reference mock. */
    private static final int UPPER_FILL = 0x3a1d18;
    private static final int LOWER_FILL = 0x2c1512;
    private static final int EDGE = 0x0a0806;
    private static final int BONE = 0xe8ddc7;

    /** The bar is drawn long enough to cross the card corner to corner. */
    private static final int BAR_THICKNESS = 8;

    private final Texture upperTexture;
    private final Texture lowerTexture;
    private final Texture barTexture;

    private final TextureRegion upper;
    private final TextureRegion lower;
    private final TextureRegion bar;

    SliceArt(int cardWidth, int cardHeight) {
        upperTexture = triangle(cardWidth, cardHeight, true);
        lowerTexture = triangle(cardWidth, cardHeight, false);
        barTexture = diagonalBar(cardWidth, cardHeight);
        upper = new TextureRegion(upperTexture);
        lower = new TextureRegion(lowerTexture);
        bar = new TextureRegion(barTexture);
    }

    /** The half above-left of the cut, or the half below-right of it. */
    TextureRegion upper() {
        return upper;
    }

    TextureRegion lower() {
        return lower;
    }

    TextureRegion bar() {
        return bar;
    }

    /**
     * One half of the cleaved card: a filled triangle with a dark cut edge. The
     * cut runs top-right to bottom-left, so the upper half is the top-left
     * triangle and the lower half the bottom-right one.
     */
    private static Texture triangle(int w, int h, boolean upperHalf) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0);
        pixmap.fill();

        // Draw the edge colour first, then the fill inset by the border, so the
        // cut face reads as a dark lip on every side including the diagonal.
        pixmap.setColor(new Color((EDGE << 8) | 0xff));
        fillHalf(pixmap, w, h, upperHalf, 0);
        pixmap.setColor(new Color(((upperHalf ? UPPER_FILL : LOWER_FILL) << 8) | 0xff));
        fillHalf(pixmap, w, h, upperHalf, 4);

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    /**
     * Fills one side of the corner-to-corner diagonal, inset by {@code inset}
     * pixels on all three sides. Done per row rather than with fillTriangle so
     * the inset is exact and every edge lands on a whole pixel.
     */
    private static void fillHalf(Pixmap pixmap, int w, int h, boolean upperHalf, int inset) {
        for (int y = inset; y < h - inset; y++) {
            // The cut: x/w + y/h = 1. Rows above it are wider on the left.
            int cut = Math.round(w * (1f - y / (float) h));
            int from = upperHalf ? inset : cut + inset;
            int to = upperHalf ? cut - inset : w - inset;
            if (to > from) {
                pixmap.drawLine(from, y, to - 1, y);
            }
        }
    }

    /** A bone bar already lying along the cut, so it never has to be rotated. */
    private static Texture diagonalBar(int w, int h) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        pixmap.setColor(0);
        pixmap.fill();
        pixmap.setColor(new Color((BONE << 8) | 0xff));
        for (int y = 0; y < h; y++) {
            int cut = Math.round(w * (1f - y / (float) h));
            int from = Math.max(0, cut - BAR_THICKNESS / 2);
            int to = Math.min(w, cut + BAR_THICKNESS / 2);
            if (to > from) {
                pixmap.drawLine(from, y, to - 1, y);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        upperTexture.dispose();
        lowerTexture.dispose();
        barTexture.dispose();
    }
}
