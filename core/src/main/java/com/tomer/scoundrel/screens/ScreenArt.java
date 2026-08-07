package com.tomer.scoundrel.screens;

/**
 * The menu screens' kit: the five parts every screen outside the board is
 * assembled from, and where each screen puts them.
 *
 * <p>HANDOFF §11 is unusually generous — it states that the six screens are
 * built from a frame, a face, a bevel, a label and a rule, and nothing else.
 * That is what this holds. Learn these and each screen is assembly work rather
 * than design; the per-screen numbers below were measured off the reference
 * renders the same way {@link BoardArt} was, not taken from the prose.
 *
 * <p>Coordinates are 1280×720 with y measured downward, as the art is
 * specified; {@link CardArt#toWorldY} converts when drawing.
 */
final class ScreenArt {

    /** The frame, the bevel and the rule are all the same 2px. */
    static final int THICK = 2;

    // --- the five parts ----------------------------------------------------

    /** The recess every widget sits in — the thing that unifies the screens. */
    static final int FRAME = 0x0f1410;
    static final int FACE_PANEL = 0x161210;
    static final int FACE_TABLE = 0x141110;
    static final int FACE_WELL = 0x12161a;

    static final int GOLD = 0xd9a441;
    static final int GOLD_LIGHT = 0xf2cf7a;
    static final int GOLD_DARK = 0xb5651f;
    static final int GOLD_LABEL = 0x12161a;

    static final int DARK = 0x1a1410;
    static final int DARK_LIGHT = 0x2f2620;
    static final int DARK_DARK = 0x0a0806;
    static final int DARK_LABEL = 0xe8ddc7;
    static final float DARK_LABEL_ALPHA = 0.72f;

    /** Section headings. */
    static final int HEADING = 0xd9a441;
    /** Descriptions and secondary readings. */
    static final int BODY = 0xe8ddc7;
    static final float BODY_ALPHA = 0.55f;
    /** Dividers. */
    static final int RULE = 0xe8ddc7;
    static final float RULE_ALPHA = 0.08f;

    // --- buttons -----------------------------------------------------------

    /**
     * Measured off the title render: 268 wide over a `frame 2 · bevel 2 · plate
     * · bevel 2 · frame 2` structure, on a 56 pitch. The board's Avoid button is
     * the same shape at its own size — see {@link Chrome#plate}.
     */
    static final int BUTTON_W = 268;
    static final int BUTTON_H = 46;
    static final int BUTTON_PITCH = 56;

    // --- the title ---------------------------------------------------------

    /** The portrait well, whose field is a 216 square inside the frame. */
    static final int WELL_X = 317;
    static final int WELL_Y = 235;
    static final int FIELD = 216;
    /** The Debt at ×3 — the only sprite on any menu, and why the screen is this game. */
    static final int PORTRAIT = Sprites.SIZE * 3;
    /** The band beneath the portrait, in the frame's own colour so the two merge. */
    static final int CAPTION_H = 23;
    static final int PORTRAIT_FIELD = 0x0e050c;
    static final int CAPTION = 0x6b5f4c;

    /** Everything in the right-hand column shares this left edge. */
    static final int COLUMN_X = 597;
    static final int EYEBROW_TOP = 176;
    static final int WORDMARK_TOP = 215;
    /** A hard offset, not a blur — the same rule the card's value numeral follows. */
    static final int WORDMARK_SHADOW_DY = 4;
    static final int WORDMARK_SHADOW = 0x0a0806;
    static final int TITLE_RULE_Y = 276;
    static final int TITLE_RULE = 0x4a3524;
    static final int BEST_TOP = 290;
    static final int BUTTONS_Y = 326;
    static final int CREDIT_TOP = 680;
    static final float CREDIT_ALPHA = 0.22f;

    // --- the header band, on every screen except the title -----------------

    /** 88px of band with the rule at its foot, measured to 89 on the render. */
    static final int HEADER_H = 89;
    static final int HEADER_X = 40;
    static final int HEADER_TITLE_TOP = 39;
    /** The caption sits on the title's baseline, not its top. */
    static final int HEADER_CAPTION_TOP = 48;
    static final int HEADER_CAPTION_GAP = 20;
    static final int HEADER_CAPTION = 0x9a8b70;
    static final int BACK_X = 1111;
    static final int BACK_Y = 27;
    static final int BACK_W = 127;
    static final int BACK_H = 36;

    // --- new game ----------------------------------------------------------

    static final int PANEL_X = 38;
    static final int PANEL_W = 1200;
    static final int PANEL_H = 89;
    static final int PANEL_Y = 115;
    /** 89 of panel and 14 of gap, as §11 says. */
    static final int PANEL_PITCH = 103;

    static final int WELL_DX = 20;
    static final int WELL_DY = 18;
    static final int WELL_SIZE = 24;
    static final int NAME_DX = 59;
    static final int NAME_DY = 26;
    static final int BADGE_DY = 21;
    static final int BADGE_H = 19;
    static final int BADGE_GAP = 18;
    static final int BADGE_PAD_X = 11;
    static final int BADGE_ON = 0xd9a441;
    static final int BADGE_OFF = 0x241d16;
    /** The label on an unearned badge, and on an unselected panel's number. */
    static final int BADGE_OFF_LABEL = 0x6b5f4c;
    static final int WELL_DIGIT_OFF = 0x494336;
    static final int START_DY = 27;
    static final int START_INSET = 24;
    static final int START_COLOUR = 0x74838f;
    static final int DESC_DX = 20;
    static final int DESC_DY = 58;

    private ScreenArt() {
    }

    static int panelY(int index) {
        return PANEL_Y + index * PANEL_PITCH;
    }

    /** The right edge everything in a panel is right-aligned against. */
    static int panelRight() {
        return PANEL_X + PANEL_W;
    }

    /**
     * Which mode panel a point in <b>world</b> coordinates is on, or -1. Same
     * flip as everywhere else: the pointer arrives y-up, the panels are
     * specified y-down.
     */
    static int panelAt(int count, float worldX, float worldY) {
        if (worldX < PANEL_X || worldX >= PANEL_X + PANEL_W) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            float bottom = CardArt.toWorldY(panelY(i), PANEL_H);
            if (worldY >= bottom && worldY < bottom + PANEL_H) {
                return i;
            }
        }
        return -1;
    }

    /** And whether a point is on the header's back button. */
    static boolean backContains(float worldX, float worldY) {
        float bottom = CardArt.toWorldY(BACK_Y, BACK_H);
        return worldX >= BACK_X && worldX < BACK_X + BACK_W
                && worldY >= bottom && worldY < bottom + BACK_H;
    }

    static int fieldX() {
        return WELL_X + THICK;
    }

    static int fieldY() {
        return WELL_Y + THICK;
    }

    static int wellW() {
        return FIELD + 2 * THICK;
    }

    static int wellH() {
        return FIELD + CAPTION_H + 2 * THICK;
    }

    /** The portrait is centred in the field; 216 − 192 leaves 12 a side. */
    static int portraitX() {
        return fieldX() + (FIELD - PORTRAIT) / 2;
    }

    static int portraitY() {
        return fieldY() + (FIELD - PORTRAIT) / 2;
    }

    static int captionY() {
        return fieldY() + FIELD;
    }

    static int buttonY(int index) {
        return BUTTONS_Y + index * BUTTON_PITCH;
    }

    /**
     * Which button of a column a point in <b>world</b> coordinates is on, or -1
     * — including the gaps between them, which must not activate anything. The
     * pointer arrives with y upward and the buttons are specified with y
     * downward, so the flip happens here rather than at every call.
     */
    static int buttonAt(int x, int count, float worldX, float worldY) {
        if (worldX < x || worldX >= x + BUTTON_W) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            float bottom = CardArt.toWorldY(buttonY(i), BUTTON_H);
            if (worldY >= bottom && worldY < bottom + BUTTON_H) {
                return i;
            }
        }
        return -1;
    }
}
