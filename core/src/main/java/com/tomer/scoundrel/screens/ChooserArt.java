package com.tomer.scoundrel.screens;

/**
 * Where the move chooser's plates go. It opens on the card you pressed when
 * that card has more than one legal move — an armed monster, most often — and
 * offers one plate per move.
 *
 * <p>The plates are the Avoid button's plate, not a popup's buttons: the board
 * has exactly one button shape, and a chooser in its own style would read as a
 * dialog arriving over the game rather than as part of it. There is no panel
 * behind them and no shadow under them; the stack is the whole widget.
 *
 * <p>Coordinates are 1280×720 with y measured downward, as the art is
 * specified; {@link CardArt#toWorldY} converts when drawing and when hit-testing
 * a pointer, which arrives the other way up.
 */
final class ChooserArt {

    /** The Avoid button's plate, exactly — one button shape on the board. */
    static final int PLATE_H = HudArt.AVOID_H;
    /** Breathing room either side of the label inside the plate. */
    static final int PAD_X = 16;
    /** Between one plate and the next. */
    static final int GAP = 6;

    private ChooserArt() {
    }

    /**
     * A plate wide enough for its label. Every plate in a stack takes the
     * widest label's width — a ragged stack reads as two unrelated buttons
     * rather than as a choice between two things.
     */
    static int plateW(int labelWidth) {
        return labelWidth + 2 * PAD_X;
    }

    static int stackH(int count) {
        return count * PLATE_H + Math.max(0, count - 1) * GAP;
    }

    /**
     * The top of the i-th plate. The stack straddles the card's middle, so the
     * choice appears over the thing it is about and the card stays readable
     * above and below it.
     */
    static int plateY(int index, int count) {
        int top = CardArt.SLOT_Y + (CardArt.CARD_H - stackH(count)) / 2;
        return top + index * (PLATE_H + GAP);
    }

    static int plateX(int slotX, int plateW) {
        return slotX + (CardArt.CARD_W - plateW) / 2;
    }

    /**
     * Which plate a point in <b>world</b> coordinates is on, or -1 for none —
     * including the gaps between them, which must not resolve anything.
     */
    static int indexAt(int slotX, int plateW, int count, float worldX, float worldY) {
        int left = plateX(slotX, plateW);
        if (worldX < left || worldX >= left + plateW) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            float bottom = CardArt.toWorldY(plateY(i, count), PLATE_H);
            if (worldY >= bottom && worldY < bottom + PLATE_H) {
                return i;
            }
        }
        return -1;
    }
}
