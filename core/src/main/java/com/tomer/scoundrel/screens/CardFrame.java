package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.tomer.scoundrel.model.CardType;

/**
 * Draws the card frame from HANDOFF.md §6/§9 — outer bezel, plate, the two 2px
 * bevels and the recessed well the sprite sits in.
 *
 * <p>Everything is a tinted rectangle on a single white pixel, so the whole card
 * is one texture and never breaks the batch. All measurements come from
 * {@link CardArt}; this class only converts them to draw calls, which is why it
 * has no arithmetic worth testing and {@code CardArt} has all of it.
 */
final class CardFrame {

    private final TextureRegion pixel;
    private final Color tint = new Color();

    CardFrame(Theme theme) {
        this.pixel = theme.whiteRegion();
    }

    /**
     * Draws one card with its top-left at the design-space point
     * {@code (slotX, slotY)} — y measured downward, as in HANDOFF.md.
     */
    void draw(Batch batch, CardType type, int slotX, int slotY) {
        CardArt.Palette palette = CardArt.paletteFor(type);
        int frame = CardArt.FRAME;
        int bevel = CardArt.BEVEL;

        // The bezel every card sits in, then the plate inset inside it.
        fill(batch, slotX, slotY, CardArt.CARD_W, CardArt.CARD_H, CardArt.OUTER);
        int plateX = slotX + frame;
        int plateY = slotY + frame;
        int plateW = CardArt.CARD_W - 2 * frame;
        int plateH = CardArt.CARD_H - 2 * frame;
        fill(batch, plateX, plateY, plateW, plateH, palette.plate());

        // Bevels: light along the top and left, dark along the bottom and right,
        // so the plate reads as raised. Each is 2px and they stop short of each
        // other's corner exactly as the mock's borders do.
        fill(batch, plateX, plateY, plateW - bevel, bevel, palette.light());
        fill(batch, plateX, plateY, bevel, plateH - bevel, palette.light());
        fill(batch, plateX + bevel, plateY + plateH - bevel, plateW - bevel, bevel, palette.dark());
        fill(batch, plateX + plateW - bevel, plateY + bevel, bevel, plateH - bevel, palette.dark());

        // The well, recessed by a hard shadow along its top edge and the faintest
        // cream lip along its bottom — the mock's two inset box-shadows.
        int wellX = CardArt.wellLeft(slotX);
        int wellY = slotY + (CardArt.wellTop() - CardArt.SLOT_Y);
        int wellW = CardArt.wellWidth();
        fill(batch, wellX, wellY, wellW, CardArt.WELL_H, palette.well());
        fill(batch, wellX, wellY, wellW, 2, 0x000000, 0.6f);
        fill(batch, wellX, wellY + CardArt.WELL_H - 2, wellW, 2, 0xe8ddc7, 0.05f);

        // The hairline above the value numeral in the footer.
        fill(batch, plateX + 6, wellY + CardArt.WELL_H, plateW - 12, 2, 0x000000, 0.55f);
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
