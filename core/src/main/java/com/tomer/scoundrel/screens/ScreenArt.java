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

    /**
     * The two plate faces with the light off them, for a plate held down.
     * Inverting the bevel alone moves the highlight but leaves 12,000 pixels of
     * face at full brightness, so the plate reads as relit rather than pushed
     * in. Both are the wood ramp's own steps — the accent row the gold lives on
     * is a row of accents, not a ramp, so there is no darker gold in it; wood is
     * the material next door and its top step is that colour in shadow. Nothing
     * outside the eighty, per the palette rule.
     */
    static final int GOLD_PRESSED = 0xa67f4a;
    static final int DARK_PRESSED = 0x100a07;

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

    /**
     * How far a pressed plate's label travels, down and to the right — the
     * bevel's own thickness, so the label lands exactly where the recess puts
     * it. The mock has no pressed state; see {@link Chrome#plate} for why one
     * exists anyway and why it is not the hover glow §11 forbids.
     */
    static final int SINK = THICK;

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
    /**
     * The back plate, as a hit-test id. A screen with more than one kind of
     * target hit-tests into one id space — {@link PressGesture} matches a
     * release against a press by equality and cannot know which family an index
     * came from. Panels and buttons are their own index and −1 is nothing, so
     * shared chrome takes the negatives below that.
     */
    static final int BACK = -2;

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

    // --- the ledger --------------------------------------------------------

    /** The table: a header row on the frame's own colour, then ten striped rows. */
    static final int TABLE_X = 41;
    static final int TABLE_Y = 111;
    static final int TABLE_W = 868;
    static final int TABLE_HEAD_H = 29;
    static final int LEDGER_ROWS = 10;
    static final int ROW_H = 36;

    /**
     * Striping is by <b>flat colour, never alpha</b> — §11 is explicit. A
     * translucent stripe over the torchlit backdrop would shift down the table
     * as the gradient does, so the rows would not read as one surface.
     */
    static final int ROW_ODD = 0x191513;
    static final int ROW_EVEN = 0x141110;

    /** Column edges, measured off the render. Two of the seven are right-aligned. */
    static final int COL_RUN = 57;
    static final int COL_SCORE_RIGHT = 166;
    static final int COL_OUTCOME = 189;
    static final int COL_MODE = 285;
    static final int COL_DATE = 389;
    static final int COL_TIME = 463;
    static final int COL_SLAIN_RIGHT = 892;

    static final int SCORE_POSITIVE = 0xe8ddc7;
    static final int SCORE_NEGATIVE = 0x8c2f22;
    static final int OUTCOME_WON = 0x71b45c;
    static final int OUTCOME_LOST = 0x8c2f22;
    /** The dim columns either side of the score: mode, date, time, slain. */
    static final int CELL_QUIET = 0x746d63;

    /** The totals panel beside it: a gold heading, then eight rows split by rules. */
    static final int TOTALS_X = 943;
    static final int TOTALS_W = 296;
    static final int TOTALS_H = 310;
    static final int TOTALS_HEADING_TOP = 130;
    static final int TOTALS_ROW_Y = 152;
    static final int TOTALS_ROW_H = 32;
    static final int TOTALS_ROWS = 8;
    static final int TOTALS_LABEL_X = 959;
    static final int TOTALS_VALUE_RIGHT = 1222;

    /** The line where a ledger with nothing in it says so. */
    static final int EMPTY_TOP = 300;

    /**
     * The quiet erase control, under the totals panel and sharing its right
     * edge. Not in the mock — the render has no destructive control at all —
     * so it sits in the empty half of the screen where nothing else goes.
     */
    static final int ERASE_W = 232;
    static final int ERASE_H = 36;
    static final int ERASE_Y = 648;

    // --- trophies ----------------------------------------------------------

    /**
     * Ten entries, five to a column, filled down then across.
     *
     * <p>Rows are taller than the render's 55 and sit on an 82 pitch rather than
     * 69. The render's copy is placeholder — its longest description is 26
     * characters against the real catalog's 85 — so at a size that survives a
     * ×1.5 viewport the real descriptions need two lines, and the row has to
     * have somewhere to put the second. There is empty screen below either way.
     */
    static final int TROPHY_X = 37;
    static final int TROPHY_Y = 113;
    static final int TROPHY_W = 582;
    static final int TROPHY_H = 68;
    static final int TROPHY_PITCH = 82;
    static final int TROPHY_COLUMN_PITCH = 614;
    static final int TROPHY_PER_COLUMN = 5;
    /** Two lines of description, and what one line of it may be. */
    static final int TROPHY_DESC_LINES = 2;
    static final int TROPHY_LINE_H = 16;

    static final int SEAL_DX = 13;
    static final int SEAL_DY = 21;
    static final int SEAL_SIZE = 26;
    static final int TROPHY_TEXT_DX = 52;
    static final int TROPHY_TITLE_DY = 12;
    static final int TROPHY_DESC_DY = 32;
    static final int TROPHY_STATUS_INSET = 16;

    static final int ROW_EARNED = 0x191513;
    static final int ROW_LOCKED = 0x131110;
    /** The empty well <em>is</em> the locked state — §11 rules out a padlock glyph. */
    static final int SEAL_EARNED = 0xd9a441;
    static final int SEAL_LOCKED = 0x1e1a17;
    static final int TROPHY_LOCKED_TEXT = 0x4a3524;

    /** The header's progress bar, built like the board's health bar. */
    static final int PROGRESS_X = 203;
    static final int PROGRESS_Y = 35;
    static final int PROGRESS_W = 160;
    static final int PROGRESS_H = 20;
    static final int PROGRESS_SEGMENT = 16;
    static final int PROGRESS_GAP = 2;
    /**
     * "Exactly like the HP bar" turns out to be literal in the render: the empty
     * track is the health bar's own {@link HudArt#BAR_EMPTY}, a dark green under
     * a gold fill. It looks like an oversight and is not — sampled off the
     * reference at 1e2a1c. The separators are the frame colour laid over the
     * track at the same alpha the board uses.
     */
    static final int PROGRESS_EMPTY = HudArt.BAR_EMPTY;
    static final float PROGRESS_SEGMENT_ALPHA = 0.8f;
    /** Three bands over a 16px interior, lightest at the top, as the bar is drawn. */
    static final int PROGRESS_BAND_TOP = 5;
    static final int PROGRESS_BAND_MID = 6;

    private ScreenArt() {
    }

    /**
     * The table's height follows from the rows it actually holds, so the frame
     * and the striping cannot disagree — a fixed height with four runs in it
     * would leave the bottom of the table hanging empty.
     */
    static int tableH(int rows) {
        return TABLE_HEAD_H + rows * ROW_H + THICK;
    }

    static int ledgerRowY(int index) {
        return TABLE_Y + TABLE_HEAD_H + index * ROW_H;
    }

    /** Each totals row carries a 2px rule at its foot, except the last. */
    static int totalsRowY(int index) {
        return TOTALS_ROW_Y + index * TOTALS_ROW_H;
    }

    static int totalsRight() {
        return TOTALS_X + TOTALS_W;
    }

    static int eraseX() {
        return totalsRight() - ERASE_W;
    }

    /** Down the first column, then down the second — the order the catalog is in. */
    static int trophyX(int index) {
        return TROPHY_X + (index / TROPHY_PER_COLUMN) * TROPHY_COLUMN_PITCH;
    }

    static int trophyY(int index) {
        return TROPHY_Y + (index % TROPHY_PER_COLUMN) * TROPHY_PITCH;
    }

    /** How wide a description line may be before the row's right inset. */
    static int trophyTextWidth() {
        return TROPHY_W - TROPHY_TEXT_DX - TROPHY_STATUS_INSET;
    }

    /** How much of the progress bar is filled, in whole pixels of its interior. */
    static int progressFillWidth(int earned, int total) {
        if (total <= 0 || earned <= 0) {
            return 0;
        }
        int interior = PROGRESS_W - 2 * THICK;
        return Math.round(interior * Math.min(earned, total) / (float) total);
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
