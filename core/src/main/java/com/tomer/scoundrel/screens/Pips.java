package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;

/**
 * The four suit pips beside a card's rank, as 12×12 masks tinted at draw time.
 *
 * <p>They are rasterised from the same circles and triangles the reference mock
 * draws them with, scaled from its 64-unit box down to 12 and thresholded at
 * each pixel's centre. A hard test rather than coverage sampling: anti-aliasing
 * a 12px glyph would spend half its pixels on grey edges, and grey is not a
 * colour this board has.
 */
final class Pips implements Disposable {

    /** The size the mock draws a pip at, and the box the geometry is scaled into. */
    static final int SIZE = 12;
    /** The mock's SVG viewBox, which every coordinate below is in. */
    private static final float BOX = 64f;

    private final Texture[] textures = new Texture[4];
    private final TextureRegion[] regions = new TextureRegion[4];

    /** A filled disc in the 64-unit box. */
    private record Disc(float cx, float cy, float r) {
        boolean holds(float x, float y) {
            float dx = x - cx;
            float dy = y - cy;
            return dx * dx + dy * dy <= r * r;
        }
    }

    /** A filled triangle in the 64-unit box. */
    private record Tri(float ax, float ay, float bx, float by, float cx, float cy) {
        boolean holds(float x, float y) {
            float d1 = side(x, y, ax, ay, bx, by);
            float d2 = side(x, y, bx, by, cx, cy);
            float d3 = side(x, y, cx, cy, ax, ay);
            boolean negative = d1 < 0 || d2 < 0 || d3 < 0;
            boolean positive = d1 > 0 || d2 > 0 || d3 > 0;
            return !(negative && positive);
        }

        private static float side(float px, float py, float x1, float y1, float x2, float y2) {
            return (px - x2) * (y1 - y2) - (x1 - x2) * (py - y2);
        }
    }

    private static final int CLUBS = 0;
    private static final int DIAMONDS = 1;
    private static final int HEARTS = 2;
    private static final int SPADES = 3;

    Pips() {
        build(CLUBS,
                new Disc[] {new Disc(32, 16, 13), new Disc(18, 36, 13), new Disc(46, 36, 13)},
                new Tri[] {new Tri(32, 40, 22, 62, 42, 62)});
        build(DIAMONDS,
                new Disc[] {},
                new Tri[] {new Tri(32, 2, 6, 32, 58, 32), new Tri(6, 32, 58, 32, 32, 62)});
        build(HEARTS,
                new Disc[] {new Disc(19, 20, 14), new Disc(45, 20, 14)},
                new Tri[] {new Tri(5, 26, 59, 26, 32, 60)});
        build(SPADES,
                new Disc[] {new Disc(18, 38, 14), new Disc(46, 38, 14)},
                new Tri[] {new Tri(32, 2, 4, 34, 60, 34), new Tri(32, 42, 22, 62, 42, 62)});
    }

    private void build(int index, Disc[] discs, Tri[] tris) {
        Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                // The pixel's centre, back in the mock's coordinates.
                float sx = (x + 0.5f) * BOX / SIZE;
                float sy = (y + 0.5f) * BOX / SIZE;
                boolean on = false;
                for (Disc disc : discs) {
                    on |= disc.holds(sx, sy);
                }
                for (Tri tri : tris) {
                    on |= tri.holds(sx, sy);
                }
                pixmap.drawPixel(x, y, on ? 0xffffffff : 0x00000000);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        textures[index] = texture;
        regions[index] = new TextureRegion(texture);
    }

    /**
     * The pip for a card: monsters carry their real suit, read from the id the
     * same way the sprite is; weapons are diamonds and potions hearts, which is
     * what they are in the deck.
     */
    TextureRegion forCard(Card card) {
        if (card.type() == CardType.MONSTER) {
            return regions[card.id().endsWith("C") ? CLUBS : SPADES];
        }
        return regions[card.type() == CardType.WEAPON ? DIAMONDS : HEARTS];
    }

    @Override
    public void dispose() {
        for (Texture texture : textures) {
            texture.dispose();
        }
    }
}
