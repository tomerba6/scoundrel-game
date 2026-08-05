package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Draws the board's chrome — the health bar, the depth ticker and the Avoid
 * button — from the measurements in {@link HudArt}.
 *
 * <p>Like the card frame, everything is a tinted rectangle on a single white
 * pixel, so the whole HUD is one texture and never breaks the batch. No
 * arithmetic lives here; it is all in {@code HudArt} where it can be tested.
 */
final class BoardHud {

    private final TextureRegion pixel;
    private final BitmapFont font;
    private final Color tint = new Color();

    BoardHud(Theme theme) {
        this.pixel = theme.whiteRegion();
        this.font = theme.pixelBody;
    }

    /**
     * @param health    current health, which may be zero or negative
     * @param maxHealth the ruleset's cap, which the bar is scaled against
     * @param healing   paints the fill in the heal colour for the pulse
     */
    void drawHealth(Batch batch, int health, int maxHealth, boolean healing) {
        drawHealth(batch, health, maxHealth, healing, false, 0,
                HudArt.barFillWidth(health, maxHealth));
    }

    /**
     * @param bleeding  paints the fill in dried blood while it drains
     * @param offsetX   the jolt a hit gives the whole bar
     * @param fillWidth how much is filled, so a change can step a segment at a time
     */
    void drawHealth(Batch batch, int health, int maxHealth, boolean healing,
                    boolean bleeding, int offsetX, int fillWidth) {
        int x = HudArt.BAR_X + offsetX;
        int y = HudArt.BAR_Y;
        fill(batch, x, y, HudArt.BAR_W, HudArt.BAR_H, HudArt.FRAME);

        int inX = x + 2;
        int inY = y + 2;
        int inW = HudArt.barInteriorWidth();
        fill(batch, inX, inY, inW, HudArt.barInteriorHeight(), HudArt.BAR_EMPTY);
        fill(batch, inX, inY, inW, HudArt.BAR_LIP_H, HudArt.BAR_EMPTY_LIP);

        // Three bands rather than one flat colour, so the bar reads as lit.
        int filled = Math.min(fillWidth, inW);
        if (filled > 0) {
            // A change repaints the whole fill so the bar reads as one event:
            // green while it grows, dried blood while it drains.
            int wash = healing ? HudArt.FILL_HEAL : HudArt.FILL_BLOOD;
            boolean changing = healing || bleeding;
            int top = changing ? wash : HudArt.FILL_TOP;
            int mid = changing ? wash : HudArt.FILL_MID;
            int low = changing ? wash : HudArt.FILL_LOW;
            fill(batch, inX, inY, filled, HudArt.BAND_TOP, top);
            fill(batch, inX, inY + HudArt.BAND_TOP, filled, HudArt.BAND_MID, mid);
            fill(batch, inX, inY + HudArt.BAND_TOP + HudArt.BAND_MID, filled, HudArt.BAND_LOW, low);
        }

        // Separators go on top of the fill, not between cells of it — the bar
        // underneath is continuous, which is why partial health lands mid-cell.
        for (int sx = inX + HudArt.SEGMENT_PITCH - HudArt.SEGMENT_GAP;
                sx < inX + inW; sx += HudArt.SEGMENT_PITCH) {
            fill(batch, sx, inY, HudArt.SEGMENT_GAP, HudArt.barInteriorHeight(),
                    HudArt.SEGMENT_LINE, HudArt.SEGMENT_ALPHA);
        }

        // The readout takes the colour of whatever is happening to the bar, and
        // holds it for as long as the bar is still changing.
        int colour = healing ? HudArt.FILL_HEAL
                : bleeding ? HudArt.FILL_BLOOD : HudArt.NUMBER_REST;
        tint.set((colour >>> 16 & 0xff) / 255f, (colour >>> 8 & 0xff) / 255f,
                (colour & 0xff) / 255f, 1f);
        font.setColor(tint);
        font.draw(batch, String.valueOf(Math.max(0, health)),
                HudArt.NUMBER_X + offsetX,
                CardArt.toWorldY(HudArt.NUMBER_BASELINE, 0));
        font.setColor(Color.WHITE);
    }

    /** One tick per card still face-down; the rest of the dungeon sits dim. */
    void drawTicker(Batch batch, int depth, int deckSize) {
        for (int i = 0; i < deckSize; i++) {
            int x = HudArt.TICKER_X + i * HudArt.TICK_PITCH;
            fill(batch, x, HudArt.TICKER_Y, HudArt.TICK_W, HudArt.TICKER_H,
                    i < HudArt.ticksLit(depth) ? HudArt.GOLD : HudArt.TICK_DIM);
        }
    }

    /** The gold plate, bevelled light on top and dark below like every button. */
    void drawAvoid(Batch batch, boolean enabled) {
        int x = HudArt.AVOID_X;
        int y = HudArt.AVOID_Y;
        int w = HudArt.AVOID_W;
        int h = HudArt.AVOID_H;
        int plate = enabled ? HudArt.GOLD : HudArt.TICK_DIM;
        int light = enabled ? HudArt.GOLD_LIGHT : HudArt.FRAME;
        int dark = enabled ? HudArt.GOLD_DARK : HudArt.FRAME;
        fill(batch, x, y, w, h, plate);
        // Each pair stops short of the other's corner, so the bottom-left and
        // top-right stay plate — the same mitre the card bevels use.
        fill(batch, x, y, w - 2, 2, light);
        fill(batch, x, y, 2, h - 2, light);
        fill(batch, x + 2, y + h - 2, w - 2, 2, dark);
        fill(batch, x + w - 2, y, 2, h, dark);
    }

    private void fill(Batch batch, int x, int y, int w, int h, int rgb) {
        fill(batch, x, y, w, h, rgb, 1f);
    }

    /** {@code y} arrives in design space and is flipped once, here. */
    private void fill(Batch batch, int x, int y, int w, int h, int rgb, float alpha) {
        tint.set((rgb >>> 16 & 0xff) / 255f, (rgb >>> 8 & 0xff) / 255f, (rgb & 0xff) / 255f, alpha);
        Color previous = batch.getColor().cpy();
        batch.setColor(tint);
        batch.draw(pixel, x, CardArt.toWorldY(y, h), w, h);
        batch.setColor(previous);
    }
}
