package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * The five parts every screen outside the board is assembled from: a frame, a
 * face, a bevel, a label and a rule.
 *
 * <p>HANDOFF §11 states that the six menu screens are these and nothing else,
 * so this is the whole vocabulary — a screen that needs a sixth part is either
 * wrong or is telling us the spec was. Everything is a tinted rectangle on a
 * single white pixel, so a whole screen is one texture and never breaks the
 * batch, exactly as {@link CardFrame} and {@link BoardHud} already work.
 *
 * <p>No arithmetic lives here; it is all in {@link ScreenArt} where it can be
 * tested. This class only turns measurements into draw calls.
 */
final class Chrome {

    /** The two button faces §11 specifies, plus the board's disabled one. */
    enum Plate { GOLD, DARK, SPENT }

    private final TextureRegion pixel;
    private final Theme theme;
    private final GlyphLayout layout = new GlyphLayout();
    private final Color tint = new Color();

    Chrome(Theme theme) {
        this.theme = theme;
        this.pixel = theme.whiteRegion();
    }

    // --- the parts ---------------------------------------------------------

    /** Flat fill. {@code y} arrives in design space and is flipped once, here. */
    void face(Batch batch, int x, int y, int w, int h, int rgb) {
        face(batch, x, y, w, h, rgb, 1f);
    }

    void face(Batch batch, int x, int y, int w, int h, int rgb, float alpha) {
        Color previous = batch.getColor().cpy();
        batch.setColor(rgb(rgb, alpha));
        batch.draw(pixel, x, CardArt.toWorldY(y, h), w, h);
        batch.setColor(previous);
    }

    /**
     * The 2px recess around a widget, drawn as four bars rather than a filled
     * rect behind it — a widget's face may be translucent, and a fill behind it
     * would show through and darken it.
     */
    void frame(Batch batch, int x, int y, int w, int h) {
        int t = ScreenArt.THICK;
        face(batch, x, y, w, t, ScreenArt.FRAME);
        face(batch, x, y + h - t, w, t, ScreenArt.FRAME);
        face(batch, x, y + t, t, h - 2 * t, ScreenArt.FRAME);
        face(batch, x + w - t, y + t, t, h - 2 * t, ScreenArt.FRAME);
    }

    /**
     * Light along the top and left, dark along the bottom and right, so the
     * face reads as raised. Each pair stops short of the other's corner, which
     * is the same mitre the card bevels use.
     */
    void bevel(Batch batch, int x, int y, int w, int h, int light, int dark) {
        int t = ScreenArt.THICK;
        face(batch, x, y, w - t, t, light);
        face(batch, x, y, t, h - t, light);
        face(batch, x + t, y + h - t, w - t, t, dark);
        face(batch, x + w - t, y, t, h, dark);
    }

    void rule(Batch batch, int x, int y, int w) {
        face(batch, x, y, w, ScreenArt.THICK, ScreenArt.RULE, ScreenArt.RULE_ALPHA);
    }

    void rule(Batch batch, int x, int y, int w, int rgb) {
        face(batch, x, y, w, ScreenArt.THICK, rgb);
    }

    // --- the one button shape ----------------------------------------------

    /**
     * A button: frame, bevel, face and a centred Silkscreen label. The board's
     * Avoid button, every choice in the move chooser and every menu button are
     * this method at different sizes — there is one button shape in the game and
     * one place that draws it, so they cannot drift apart.
     */
    void plate(Batch batch, int x, int y, int w, int h, String text, Plate style) {
        int face;
        int light;
        int dark;
        int label;
        float labelAlpha = 1f;
        switch (style) {
            case GOLD -> {
                face = ScreenArt.GOLD;
                light = ScreenArt.GOLD_LIGHT;
                dark = ScreenArt.GOLD_DARK;
                label = ScreenArt.GOLD_LABEL;
            }
            case DARK -> {
                face = ScreenArt.DARK;
                light = ScreenArt.DARK_LIGHT;
                dark = ScreenArt.DARK_DARK;
                label = ScreenArt.DARK_LABEL;
                labelAlpha = ScreenArt.DARK_LABEL_ALPHA;
            }
            default -> {
                // Spent: the plate is there but dead, so the bevel goes flat.
                face = HudArt.TICK_DIM;
                light = ScreenArt.FRAME;
                dark = ScreenArt.FRAME;
                label = BoardArt.NAME_COLOUR;
            }
        }
        int t = ScreenArt.THICK;
        frame(batch, x, y, w, h);
        face(batch, x + t, y + t, w - 2 * t, h - 2 * t, face);
        bevel(batch, x + t, y + t, w - 2 * t, h - 2 * t, light, dark);
        centred(batch, theme.pixelLabel, text, x, y, w, h, label, labelAlpha);
    }

    // --- text --------------------------------------------------------------

    /** Text placed by its top, which is what a Batch draw takes. */
    void text(Batch batch, BitmapFont font, String s, int x, int top, int rgb, float alpha) {
        font.setColor(rgb(rgb, alpha));
        font.draw(batch, s, x, CardArt.toWorldY(top, 0));
        font.setColor(Color.WHITE);
    }

    void text(Batch batch, BitmapFont font, String s, int x, int top, int rgb) {
        text(batch, font, s, x, top, rgb, 1f);
    }

    /** Right-aligned against an edge — the column a table's numbers end on. */
    void textRight(Batch batch, BitmapFont font, String s, int right, int top,
                   int rgb, float alpha) {
        layout.setText(font, s);
        text(batch, font, s, right - Math.round(layout.width), top, rgb, alpha);
    }

    /** Centred in a box, horizontally and vertically — how a label sits on a plate. */
    void centred(Batch batch, BitmapFont font, String s, int x, int y, int w, int h,
                 int rgb, float alpha) {
        layout.setText(font, s);
        int left = x + Math.round((w - layout.width) / 2f);
        int top = y + Math.round((h - layout.height) / 2f);
        text(batch, font, s, left, top, rgb, alpha);
    }

    /** Centred on a vertical line, for a caption under a well. */
    void centredOn(Batch batch, BitmapFont font, String s, int centreX, int top,
                   int rgb, float alpha) {
        layout.setText(font, s);
        text(batch, font, s, centreX - Math.round(layout.width / 2f), top, rgb, alpha);
    }

    int width(BitmapFont font, String s) {
        layout.setText(font, s);
        return Math.round(layout.width);
    }

    private Color rgb(int rgb, float alpha) {
        tint.set((rgb >>> 16 & 0xff) / 255f, (rgb >>> 8 & 0xff) / 255f,
                (rgb & 0xff) / 255f, alpha);
        return tint;
    }
}
