package com.tomer.scoundrel.screens;

/**
 * Everything on the board that is not the card frame or the top HUD: the card's
 * own header and value numeral, the trophy rail along the bottom left, the
 * potion marker opposite it, and the event feed down the right.
 *
 * <p>Coordinates are 1280×720 with y measured downward, as the art is
 * specified; {@link CardArt#toWorldY} converts when drawing. Every number was
 * measured off the reference render rather than guessed, and this is the one
 * place they live.
 */
final class BoardArt {

    /** The 2px recess every widget on the board sits in. */
    static final int FRAME = 2;

    // --- the card's header and value --------------------------------------

    /** The mock's 6px padding inside the plate, which is itself inset by the frame. */
    private static final int HEADER_PAD = 6;
    /** The suit pip beside the rank — 12px, the size the mock draws it. */
    static final int PIP = 12;
    /** Between the rank and its pip. */
    static final int PIP_GAP = 4;
    /**
     * The numeral's baseline, measured down from the top of the card. The
     * reference puts the glyph's foot at y=432 with the row at 214.
     */
    static final int VALUE_BOTTOM = 218;
    /** A hard offset, not a blur: the shadow is the glyph moved down four. */
    static final int VALUE_SHADOW_DY = 4;
    static final int VALUE_SHADOW = 0x0a0806;
    static final float VALUE_SHADOW_ALPHA = 0.7f;
    static final int VALUE_COLOUR = 0xe8ddc7;

    // --- the trophy rail ---------------------------------------------------

    static final int RAIL_X = 24;
    static final int RAIL_Y = 610;
    static final int RAIL_BOX = 80;
    /**
     * The card sprite at ×1. The reference draws it at 72, which is ×1.125 —
     * uneven, so some source pixels would take two screen pixels and their
     * neighbours one. The well keeps the render's size; only the icon shrinks.
     */
    static final int RAIL_ICON = Sprites.SIZE;
    /** The recess the icon sits in, the same iron the mock uses. */
    static final int RAIL_WELL = 0x12161a;

    /** The label, chips and plate stack up in a column to the right of the well. */
    static final int COLUMN_X = 116;
    /**
     * Text is placed by its top, not its baseline: that is what a Batch draw
     * takes, and pretending otherwise puts every label a font's ascent out.
     */
    static final int NAME_TOP = 628;
    static final int NAME_COLOUR = 0x74838f;

    static final int CHIP_Y = 646;
    static final int CHIP_W = 22;
    static final int CHIP_H = 30;
    static final int CHIP_PITCH = 26;
    static final int CHIP_FACE = 0x4e2620;
    static final int CHIP_LABEL = 0xa35543;

    static final int PLATE_Y = 649;
    static final int PLATE_H = 24;
    static final int PLATE_PAD_X = 11;
    /** Clear of the last chip's own trailing gap. */
    private static final int PLATE_GAP = 8;

    // --- the potion marker -------------------------------------------------

    static final int MARKER_X = 1108;
    static final int MARKER_Y = 632;
    static final int MARKER_BOX = 36;
    /**
     * The sprite halved — every 2×2 source block becomes one pixel, which is
     * the only shrink that keeps the grid even. The reference's 26px does not.
     */
    static final int MARKER_ICON = Sprites.SIZE / 2;
    static final int MARKER_LABEL_X = 1152;
    static final int MARKER_LABEL_TOP = 645;
    static final int MARKER_READY = 0x4a3524;
    static final int MARKER_USED = 0xd9a441;

    // --- the event feed ----------------------------------------------------

    /** Right-aligned against the same margin the Avoid button keeps. */
    static final int FEED_RIGHT = 1256;
    static final int FEED_TOP = 96;
    static final int FEED_PITCH = 20;
    static final int FEED_COLOUR = 0x9a8b70;

    // --- the depth line under the ticker -----------------------------------

    static final int DEPTH_TOP = 54;
    static final int DEPTH_COLOUR = 0x9a8b70;
    /** The dim tail on the health readout: "/20 HP". */
    static final int HP_SUFFIX_COLOUR = 0x6b5f4c;
    static final int HP_SUFFIX_GAP = 8;

    /**
     * The line YOU DIED grows outward from, above the middle of the screen.
     * Drawing it by the top of its line instead — which is what a Batch draw
     * takes — made it grow downward, so it read as being pushed rather than as
     * arriving.
     */
    static final int DEATH_TITLE_CENTRE_Y = 300;

    private BoardArt() {
    }

    static int rankX(int slotX) {
        return slotX + CardArt.FRAME + HEADER_PAD;
    }

    /** The type label is right-aligned; this is the edge it ends on. */
    static int typeRightX(int slotX) {
        return slotX + CardArt.CARD_W - CardArt.FRAME - HEADER_PAD;
    }

    static int pipY() {
        return CardArt.SLOT_Y + CardArt.FRAME + (CardArt.HEADER_H - PIP) / 2;
    }

    static int valueCentreX(int slotX) {
        return slotX + CardArt.CARD_W / 2;
    }

    static int railIconX() {
        return RAIL_X + (RAIL_BOX - RAIL_ICON) / 2;
    }

    static int railIconY() {
        return RAIL_Y + (RAIL_BOX - RAIL_ICON) / 2;
    }

    /** The i-th slain chip, marching right from the column. */
    static int chipX(int index) {
        return COLUMN_X + index * CHIP_PITCH;
    }

    /** The degradation plate sits past the last chip, so it moves as kills mount. */
    static int slaysPlateX(int slainCount) {
        return COLUMN_X + Math.max(0, slainCount) * CHIP_PITCH + PLATE_GAP;
    }

    static int markerIconX() {
        return MARKER_X + (MARKER_BOX - MARKER_ICON) / 2;
    }

    static int markerIconY() {
        return MARKER_Y + (MARKER_BOX - MARKER_ICON) / 2;
    }

    /** Feed lines stack downward, newest last, under the Avoid button. */
    static int feedLineY(int index) {
        return FEED_TOP + index * FEED_PITCH;
    }
}
