package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.CardType;

/**
 * How a card is built: its measurements and the ramp its type is drawn from.
 * The one place these numbers live — they were set by hand against the art
 * handoff and its reference mock, so check there before changing one.
 *
 * <p>Pure, so the arithmetic is testable and {@link CardFrame} is left with
 * nothing but draw calls.
 *
 * <p>Coordinates are 1280×720 with <b>y measured downward</b> from the top of
 * the screen, which is how the art is specified. The viewport measures y
 * upward, so {@link #toWorldY} converts, in one place.
 */
final class CardArt {

    /** The five colours a card type is drawn from. */
    record Palette(int plate, int light, int dark, int well, int label) {
    }

    private static final Palette MONSTER = new Palette(0x230d16, 0x4f1d1e, 0x12060f, 0x0e050c, 0xa85338);
    private static final Palette WEAPON = new Palette(0x141a24, 0x333f4c, 0x090c14, 0x070a10, 0x8a9ea8);
    private static final Palette POTION = new Palette(0x17281a, 0x325a31, 0x0e1a12, 0x0a1310, 0x71b45c);

    /** The bezel around every card, whatever its type. */
    static final int OUTER = 0x0f1410;
    /** Stage background. */
    static final int BACKDROP = 0x100c09;

    static final int CARD_W = 176;
    static final int CARD_H = 256;
    /**
     * The written spec quotes 220; the reference render puts the row's top
     * bezel on 214, which is where centring the row between the HUD strip and
     * the rail actually lands it. The render is the visual target, so it wins.
     */
    static final int SLOT_Y = 214;
    private static final int SLOT_X0 = 252;
    private static final int SLOT_PITCH = 200;

    /** The outer frame the plate is inset by, and the bevel drawn inside it. */
    static final int FRAME = 2;
    static final int BEVEL = 2;
    /** Rank and type sit here; the well begins below it. */
    static final int HEADER_H = 26;
    static final int WELL_H = 140;
    private static final int WELL_MARGIN = 6;
    /** 64×64 drawn at ×2. */
    static final int SPRITE = 128;

    private CardArt() {
    }

    static Palette paletteFor(CardType type) {
        return switch (type) {
            case MONSTER -> MONSTER;
            case WEAPON -> WEAPON;
            case POTION -> POTION;
        };
    }

    /** Left edge of the i-th of four slots; the row is centred in the world. */
    static int slotX(int index) {
        return SLOT_X0 + index * SLOT_PITCH;
    }

    /**
     * Top of the well. The plate is inset by the frame, so the 26px header runs
     * from {@code SLOT_Y + FRAME} and the well starts {@code FRAME} lower than
     * the {@code SLOT_Y + 26} the written spec quotes, which omits the frame.
     * The mock is the visual target and it insets, so it wins.
     */
    static int wellTop() {
        return SLOT_Y + FRAME + HEADER_H;
    }

    static int wellLeft(int slotX) {
        return slotX + FRAME + WELL_MARGIN;
    }

    static int wellWidth() {
        return CARD_W - 2 * (FRAME + WELL_MARGIN);
    }

    static int spriteLeft(int slotX) {
        return wellLeft(slotX) + (wellWidth() - SPRITE) / 2;
    }

    static int spriteTop() {
        return wellTop() + (WELL_H - SPRITE) / 2;
    }

    /**
     * Design-space y (down from the top) to world y (up from the bottom), for
     * an element {@code height} tall.
     */
    static int toWorldY(int designY, int height) {
        return (int) Theme.WORLD_HEIGHT - designY - height;
    }
}
