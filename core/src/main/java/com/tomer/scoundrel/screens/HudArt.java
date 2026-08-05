package com.tomer.scoundrel.screens;

/**
 * The board HUD's measurements and colours — the health bar, the depth ticker
 * and the Avoid button. Every number here was read off the reference render
 * rather than guessed, and this is the one place they live.
 *
 * <p>Coordinates are 1280×720 with y measured downward, as the art is
 * specified; {@link CardArt#toWorldY} converts when drawing.
 *
 * <p>Note that the chrome colours are <b>not</b> all on the sprite ramps. The
 * frame and the bar's bands come from the reference render and sit between
 * ramp steps. Sprites are strictly on-palette; the furniture around them is
 * whatever the design drew, and matching it exactly is what lets a render be
 * diffed against the reference pixel for pixel.
 */
final class HudArt {

    // --- shared colours ----------------------------------------------------

    /** The 2px recess around every widget. */
    static final int FRAME = 0x0f1410;
    static final int GOLD = 0xd9a441;
    static final int GOLD_LIGHT = 0xf2cf7a;
    static final int GOLD_DARK = 0xb5651f;
    /** Label colour on a gold plate. */
    static final int LABEL_DARK = 0x12101c;

    // --- health bar --------------------------------------------------------

    static final int BAR_X = 24;
    static final int BAR_Y = 34;
    static final int BAR_W = 216;
    static final int BAR_H = 24;

    /**
     * Three bands, lightest at the top, so the bar reads as a lit surface
     * rather than a flat rectangle.
     */
    static final int BAND_TOP = 6;
    static final int BAND_MID = 8;
    static final int BAND_LOW = 6;
    static final int FILL_TOP = 0xe8ddc7;
    static final int FILL_MID = 0x9a8b70;
    static final int FILL_LOW = 0x6b5f4c;
    /**
     * What is left when health has gone: a flat body under a 2px lip, rather
     * than the three bands the filled part carries. The spent track reads as a
     * recess that way, not as an unlit copy of the bar.
     */
    static final int BAR_EMPTY = 0x1e2a1c;
    static final int BAR_EMPTY_LIP = 0x3b4334;
    static final int BAR_LIP_H = 2;
    /** Heal repaints the fill in this before settling back. */
    static final int FILL_HEAL = 0x71b45c;

    /**
     * Separators are an overlay on a continuous bar, not one cell per point of
     * health — the fill is proportional and these are drawn on top of it.
     */
    static final int SEGMENT_PITCH = 10;
    static final int SEGMENT_GAP = 2;
    static final int SEGMENT_LINE = 0x2d3029;
    /**
     * The separators are translucent, so each takes the tone of the band it
     * crosses rather than cutting a flat grey line through all three. Over the
     * top band the reference reads #535349 and over the spent track's lip
     * #30342b; solving both blends gives this colour at 0.798, which
     * reproduces each exactly.
     */
    static final float SEGMENT_ALPHA = 0.798f;

    // --- depth ticker ------------------------------------------------------

    static final int TICKER_X = 656;
    static final int TICKER_Y = 26;
    static final int TICKER_H = 20;
    static final int TICK_W = 2;
    static final int TICK_PITCH = 4;
    static final int TICK_DIM = 0x20180e;

    // --- avoid button ------------------------------------------------------

    static final int AVOID_X = 1143;
    static final int AVOID_Y = 26;
    static final int AVOID_W = 111;
    static final int AVOID_H = 41;

    private HudArt() {
    }

    static int barInteriorWidth() {
        return BAR_W - 2 * 2;
    }

    static int barInteriorHeight() {
        return BAR_H - 2 * 2;
    }

    /** How much of the bar is filled, clamped so a negative score draws nothing. */
    static int barFillWidth(int health, int maxHealth) {
        if (health <= 0 || maxHealth <= 0) {
            return 0;
        }
        int clamped = Math.min(health, maxHealth);
        return Math.round(barInteriorWidth() * clamped / (float) maxHealth);
    }

    /** One tick per card still face-down, so the ticker is a real gauge. */
    static int ticksLit(int depth) {
        return Math.max(0, depth);
    }

    static int tickerWidth(int deckSize) {
        return deckSize <= 0 ? 0 : deckSize * TICK_PITCH - (TICK_PITCH - TICK_W);
    }
}
