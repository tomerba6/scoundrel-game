package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The menu kit's measurements, read off the reference renders rather than
 * guessed. Pinned here because they are hand-set constants that a screenshot
 * would only catch if someone looked at exactly the right pixel.
 */
class ScreenArtTest {

    /** The palette HANDOFF §11 states outright. If these drift, screens stop matching. */
    @Test
    void theFivePartsAreTheColoursTheSpecNames() {
        assertEquals(0x0f1410, ScreenArt.FRAME);
        assertEquals(0x161210, ScreenArt.FACE_PANEL);
        assertEquals(0x141110, ScreenArt.FACE_TABLE);
        assertEquals(0x12161a, ScreenArt.FACE_WELL);
        assertEquals(0xd9a441, ScreenArt.GOLD);
        assertEquals(0xf2cf7a, ScreenArt.GOLD_LIGHT);
        assertEquals(0xb5651f, ScreenArt.GOLD_DARK);
        assertEquals(0x12161a, ScreenArt.GOLD_LABEL);
        assertEquals(0x1a1410, ScreenArt.DARK);
        assertEquals(0x2f2620, ScreenArt.DARK_LIGHT);
        assertEquals(0x0a0806, ScreenArt.DARK_DARK);
        assertEquals(2, ScreenArt.THICK, "frame, bevel and rule are all 2px");
    }

    /**
     * The gold accents are shared with the sprites and must stay on the ramps,
     * or a menu button would not match the gold on a card.
     */
    @Test
    void theAccentsAreOnThePalette() {
        for (int rgb : new int[] {ScreenArt.GOLD, ScreenArt.GOLD_LIGHT, ScreenArt.GOLD_DARK,
                                  ScreenArt.BODY, ScreenArt.HEADING,
                                  ScreenArt.GOLD_PRESSED, ScreenArt.DARK_PRESSED}) {
            assertTrue(Ramps.contains(rgb),
                    "accent #" + Integer.toHexString(rgb) + " is off the palette");
        }
    }

    /**
     * The label on a gold plate is <b>not</b> one of them, and the board and the
     * menus disagree about it: the board render puts it on {@code 12101c}, which
     * is the bone ramp's darkest step, and the title render on {@code 12161a},
     * which is the well colour. The difference is six values of green and
     * invisible, but the furniture is matched to whatever the design drew rather
     * than snapped to the nearest ramp — that is what lets a render be diffed
     * against its reference. Pinned so the split is deliberate, not drift.
     */
    @Test
    void theLabelOnAGoldPlateIsChromeAndTheTwoRendersDisagree() {
        assertEquals(0x12161a, ScreenArt.GOLD_LABEL);
        assertEquals(0x12101c, HudArt.LABEL_DARK);
        assertFalse(Ramps.contains(ScreenArt.GOLD_LABEL));
        assertTrue(Ramps.contains(HudArt.LABEL_DARK));
    }

    /**
     * The board's Avoid button and a menu button are the same shape at
     * different sizes. The spec calls for 268 wide; the render agrees.
     */
    @Test
    void aButtonIsTwoHundredAndSixtyEightWideOnAFiftySixPitch() {
        assertEquals(268, ScreenArt.BUTTON_W);
        assertEquals(46, ScreenArt.BUTTON_H);
        assertEquals(56, ScreenArt.BUTTON_PITCH);
        assertTrue(ScreenArt.BUTTON_PITCH > ScreenArt.BUTTON_H, "buttons would overlap");
        assertEquals(326, ScreenArt.buttonY(0));
        assertEquals(382, ScreenArt.buttonY(1), "measured off the title render");
        assertEquals(494, ScreenArt.buttonY(3));
    }

    /**
     * A pressed plate's label travels exactly one bevel. Any other number and
     * the label stops somewhere the recess does not explain — and a fractional
     * one would put the Silkscreen glyphs off the pixel grid.
     */
    @Test
    void aPressedPlateTravelsExactlyOneBevel() {
        assertEquals(ScreenArt.THICK, ScreenArt.SINK);
        assertTrue(ScreenArt.SINK * 2 < ScreenArt.BUTTON_H, "the label would sink out of the plate");
    }

    /**
     * A held plate's face must be darker than its resting one and must not
     * collide with the bevel tone drawn over it — either way round the recess
     * stops reading, and a plate that gets <em>brighter</em> when pushed in is
     * the one bug this state can have.
     */
    @Test
    void aHeldPlateGoesDarkerWithoutSwallowingItsBevel() {
        assertTrue(luma(ScreenArt.GOLD_PRESSED) < luma(ScreenArt.GOLD));
        assertTrue(luma(ScreenArt.DARK_PRESSED) < luma(ScreenArt.DARK));
        assertNotEquals(ScreenArt.GOLD_PRESSED, ScreenArt.GOLD_DARK);
        assertNotEquals(ScreenArt.DARK_PRESSED, ScreenArt.DARK_DARK);
    }

    private static int luma(int rgb) {
        return 2 * (rgb >>> 16 & 0xff) + 5 * (rgb >>> 8 & 0xff) + (rgb & 0xff);
    }

    /**
     * The portrait well is a square field with a caption band under it, all
     * inside one 2px frame. 216 − 192 leaves exactly 12 pixels a side, so the
     * sprite lands on whole pixels at ×3 without rounding.
     */
    @Test
    void thePortraitSitsWholeInsideItsField() {
        assertEquals(216, ScreenArt.FIELD);
        assertEquals(192, ScreenArt.PORTRAIT, "The Debt at x3");
        assertEquals(0, (ScreenArt.FIELD - ScreenArt.PORTRAIT) % 2, "an odd margin would half-pixel it");
        assertEquals(319, ScreenArt.fieldX());
        assertEquals(237, ScreenArt.fieldY());
        assertEquals(331, ScreenArt.portraitX());
        assertEquals(249, ScreenArt.portraitY());
        assertEquals(220, ScreenArt.wellW());
        assertEquals(243, ScreenArt.wellH());
        assertEquals(453, ScreenArt.captionY());
    }

    /** Everything in the right column hangs off one left edge, the buttons' own. */
    @Test
    void theRightColumnIsOneEdge() {
        assertEquals(597, ScreenArt.COLUMN_X);
        assertTrue(ScreenArt.EYEBROW_TOP < ScreenArt.WORDMARK_TOP);
        assertTrue(ScreenArt.WORDMARK_TOP < ScreenArt.TITLE_RULE_Y);
        assertTrue(ScreenArt.TITLE_RULE_Y < ScreenArt.BEST_TOP);
        assertTrue(ScreenArt.BEST_TOP < ScreenArt.BUTTONS_Y);
        // Four buttons and the credit line must not collide.
        assertTrue(ScreenArt.buttonY(3) + ScreenArt.BUTTON_H < ScreenArt.CREDIT_TOP);
        assertTrue(ScreenArt.COLUMN_X + ScreenArt.BUTTON_W < Theme.WORLD_WIDTH,
                "the button column runs off the stage");
    }

    /** The well and the right column must not overlap. */
    @Test
    void theTwoColumnsClearEachOther() {
        assertTrue(ScreenArt.WELL_X + ScreenArt.wellW() < ScreenArt.COLUMN_X);
    }

    /**
     * Hit where drawn. The pointer arrives with y upward and the buttons are
     * specified with y downward — get the flip wrong and the menu works
     * somewhere else entirely.
     *
     * <p>This is the only check the flip gets, and it is enough: the board's
     * Avoid button is also guarded by a "not hit at its design-space y" test,
     * but that cannot be written for a column. Over 268 pixels of buttons
     * straddling the middle of the screen, an unflipped coordinate lands inside
     * some band by coincidence — button 0's own unflipped y is inside its own
     * flipped band. Asserting the positive for all four does catch a missing
     * flip, because buttons 2 and 3 then match nothing at all.
     */
    @Test
    void eachButtonIsHitWhereItIsDrawn() {
        int x = ScreenArt.COLUMN_X;
        for (int i = 0; i < 4; i++) {
            float middleY = CardArt.toWorldY(ScreenArt.buttonY(i), ScreenArt.BUTTON_H)
                    + ScreenArt.BUTTON_H / 2f;
            assertEquals(i, ScreenArt.buttonAt(x, 4, x + ScreenArt.BUTTON_W / 2f, middleY),
                    "button " + i + " should be hit at its own middle");
        }
    }

    @Test
    void theGapsBetweenButtonsHitNothing() {
        int x = ScreenArt.COLUMN_X;
        float gap = CardArt.toWorldY(ScreenArt.buttonY(0) + ScreenArt.BUTTON_H + 2, 0);
        assertEquals(-1, ScreenArt.buttonAt(x, 4, x + ScreenArt.BUTTON_W / 2f, gap));
        float middleY = CardArt.toWorldY(ScreenArt.buttonY(0), ScreenArt.BUTTON_H)
                + ScreenArt.BUTTON_H / 2f;
        assertEquals(-1, ScreenArt.buttonAt(x, 4, x - 1, middleY), "left of the column");
        assertEquals(-1, ScreenArt.buttonAt(x, 4, x + ScreenArt.BUTTON_W, middleY), "right of it");
    }

    /**
     * A screen with two kinds of target — three mode panels and the header's
     * back plate — hit-tests into <b>one</b> id space, because
     * {@link PressGesture} compares a press against a release by equality and
     * cannot know which family an index came from. Indices are their own value,
     * −1 is nothing, so the chrome takes the negatives below that.
     */
    @Test
    void theBackPlateIsItsOwnHitIdAndNotNothing() {
        assertNotEquals(PressGesture.NONE, ScreenArt.BACK);
        assertTrue(ScreenArt.BACK < PressGesture.NONE, "a chrome id must not collide with a panel");
    }

    /**
     * And the two families cannot overlap on screen, or one point would have two
     * answers and which one won would come down to the order of the ifs.
     */
    @Test
    void theBackPlateCannotBeConfusedWithAPanel() {
        assertTrue(ScreenArt.BACK_Y + ScreenArt.BACK_H <= ScreenArt.HEADER_H,
                "the back plate hangs out of the header band");
        assertTrue(ScreenArt.HEADER_H <= ScreenArt.PANEL_Y,
                "the header band overlaps the first panel");
        float backX = ScreenArt.BACK_X + ScreenArt.BACK_W / 2f;
        float backY = CardArt.toWorldY(ScreenArt.BACK_Y, ScreenArt.BACK_H) + ScreenArt.BACK_H / 2f;
        assertTrue(ScreenArt.backContains(backX, backY));
        assertEquals(-1, ScreenArt.panelAt(3, backX, backY), "the back plate answered as a panel");
    }

    /** And the reverse: a point on a panel must not answer as the back plate. */
    @Test
    void aPanelCannotBeConfusedWithTheBackPlate() {
        for (int i = 0; i < 3; i++) {
            float middleY = CardArt.toWorldY(ScreenArt.panelY(i), ScreenArt.PANEL_H)
                    + ScreenArt.PANEL_H / 2f;
            float x = ScreenArt.PANEL_X + ScreenArt.PANEL_W / 2f;
            assertEquals(i, ScreenArt.panelAt(3, x, middleY));
            assertFalse(ScreenArt.backContains(x, middleY));
        }
    }

    /**
     * The ledger's table, measured off the render. The height follows from the
     * rows rather than being its own constant, so a row count and a frame
     * cannot disagree — the failure that leaves a table with a floating edge.
     */
    @Test
    void theLedgerTableIsItsRowsPlusItsHeader() {
        assertEquals(41, ScreenArt.TABLE_X);
        assertEquals(111, ScreenArt.TABLE_Y);
        assertEquals(868, ScreenArt.TABLE_W);
        assertEquals(140, ScreenArt.ledgerRowY(0), "measured off the render");
        assertEquals(176, ScreenArt.ledgerRowY(1));
        assertEquals(464, ScreenArt.ledgerRowY(9));
        assertEquals(391, ScreenArt.tableH(ScreenArt.LEDGER_ROWS));
        assertEquals(ScreenArt.TABLE_Y + ScreenArt.tableH(ScreenArt.LEDGER_ROWS) - ScreenArt.THICK,
                ScreenArt.ledgerRowY(ScreenArt.LEDGER_ROWS - 1) + ScreenArt.ROW_H,
                "the last row must land on the frame's inside edge");
        // And a short log shrinks the frame rather than leaving it hanging.
        assertEquals(ScreenArt.TABLE_Y + ScreenArt.tableH(3) - ScreenArt.THICK,
                ScreenArt.ledgerRowY(2) + ScreenArt.ROW_H);
    }

    /** The columns run left to right and none of them overlaps the next. */
    @Test
    void theLedgerColumnsAreInOrderInsideTheTable() {
        int[] edges = {ScreenArt.COL_RUN, ScreenArt.COL_SCORE_RIGHT, ScreenArt.COL_OUTCOME,
                       ScreenArt.COL_MODE, ScreenArt.COL_DATE, ScreenArt.COL_TIME,
                       ScreenArt.COL_SLAIN_RIGHT};
        for (int i = 1; i < edges.length; i++) {
            assertTrue(edges[i - 1] < edges[i], "column " + i + " runs backwards");
        }
        assertTrue(ScreenArt.TABLE_X < edges[0]);
        assertTrue(edges[edges.length - 1] < ScreenArt.TABLE_X + ScreenArt.TABLE_W);
    }

    /**
     * Eight totals rows with a 2px rule at the foot of each but the last, and
     * the whole stack inside the panel. The rule positions are the render's.
     */
    @Test
    void theTotalsPanelHoldsEightRowsAndItsRules() {
        assertEquals(943, ScreenArt.TOTALS_X);
        assertEquals(296, ScreenArt.TOTALS_W);
        assertEquals(152, ScreenArt.totalsRowY(0));
        assertEquals(182, ScreenArt.totalsRowY(0) + ScreenArt.TOTALS_ROW_H - ScreenArt.THICK,
                "the first rule is at 182 on the render");
        assertEquals(374, ScreenArt.totalsRowY(6) + ScreenArt.TOTALS_ROW_H - ScreenArt.THICK,
                "and the last one at 374");
        int bottom = ScreenArt.totalsRowY(ScreenArt.TOTALS_ROWS - 1) + ScreenArt.TOTALS_ROW_H;
        assertTrue(bottom < ScreenArt.TABLE_Y + ScreenArt.TOTALS_H, "the rows run out of the panel");
        assertTrue(ScreenArt.TABLE_X + ScreenArt.TABLE_W < ScreenArt.TOTALS_X,
                "the table and the panel overlap");
        assertTrue(ScreenArt.totalsRight() < Theme.WORLD_WIDTH);
    }

    /**
     * Trophies fill down the first column and then down the second — the order
     * the catalog is authored in. Getting this row-major would silently reorder
     * ten achievements and look entirely plausible.
     */
    @Test
    void trophiesFillDownThenAcross() {
        assertEquals(ScreenArt.TROPHY_X, ScreenArt.trophyX(0));
        assertEquals(ScreenArt.TROPHY_X, ScreenArt.trophyX(4), "still the first column");
        assertEquals(651, ScreenArt.trophyX(5), "the second column starts at the sixth");
        assertEquals(651, ScreenArt.trophyX(9));
        assertEquals(113, ScreenArt.trophyY(0));
        assertEquals(441, ScreenArt.trophyY(4));
        assertEquals(113, ScreenArt.trophyY(5), "the second column starts at the top again");
    }

    /**
     * A trophy row has to hold a title and both description lines. The render's
     * 55px row does not, because its copy is placeholder — 26 characters against
     * the real catalog's 85 — so the row grew rather than the type shrinking.
     */
    @Test
    void aTrophyRowHasRoomForBothDescriptionLines() {
        int lastLineBottom = ScreenArt.TROPHY_DESC_DY
                + (ScreenArt.TROPHY_DESC_LINES - 1) * ScreenArt.TROPHY_LINE_H
                + ScreenArt.TROPHY_LINE_H;
        assertTrue(lastLineBottom <= ScreenArt.TROPHY_H,
                "the second description line hangs out of its row");
        assertTrue(ScreenArt.TROPHY_TITLE_DY < ScreenArt.TROPHY_DESC_DY);
        assertTrue(ScreenArt.SEAL_DY + ScreenArt.SEAL_SIZE <= ScreenArt.TROPHY_H);
        assertTrue(ScreenArt.trophyTextWidth() > 0);
    }

    @Test
    void theTwoTrophyColumnsClearEachOtherAndTheStage() {
        assertTrue(ScreenArt.trophyX(0) + ScreenArt.TROPHY_W < ScreenArt.trophyX(5));
        assertTrue(ScreenArt.trophyX(5) + ScreenArt.TROPHY_W <= Theme.WORLD_WIDTH);
        assertTrue(ScreenArt.TROPHY_PITCH > ScreenArt.TROPHY_H, "rows would overlap");
        assertTrue(ScreenArt.trophyY(4) + ScreenArt.TROPHY_H < Theme.WORLD_HEIGHT);
    }

    /** The progress bar fills by whole pixels and never past its own interior. */
    @Test
    void theProgressBarFillsInWholePixelsAndClamps() {
        int interior = ScreenArt.PROGRESS_W - 2 * ScreenArt.THICK;
        assertEquals(0, ScreenArt.progressFillWidth(0, 10));
        assertEquals(interior, ScreenArt.progressFillWidth(10, 10));
        assertEquals(62, ScreenArt.progressFillWidth(4, 10), "the render shows 4 of 10 as 62px");
        assertEquals(interior, ScreenArt.progressFillWidth(99, 10), "more earned than exist");
        assertEquals(0, ScreenArt.progressFillWidth(1, 0), "no catalog, no division");
    }

    /**
     * The first-run prompt's copy and its plates must not overlap. They did:
     * the prompt borrowed the menu column's {@link ScreenArt#buttonY}, which put
     * the first plate at 326 — straight through the second line of its own text.
     * It only shows on a first launch, so nobody had ever seen it.
     */
    @Test
    void theFirstRunPromptsTextClearsItsButtons() {
        int lastLineBottom = ScreenArt.PROMPT_Y + ScreenArt.PROMPT_LINE_DY
                + ScreenArt.PROMPT_LINE_GAP + PixelType.LABEL;
        assertTrue(lastLineBottom <= ScreenArt.promptButtonY(0),
                "the prompt's copy runs into its first button");
        int lastButtonBottom = ScreenArt.promptButtonY(1) + ScreenArt.BUTTON_H;
        assertTrue(lastButtonBottom <= ScreenArt.PROMPT_Y + ScreenArt.PROMPT_H,
                "the second button hangs out of the panel");
        assertTrue(ScreenArt.PROMPT_Y + ScreenArt.PROMPT_HEADING_DY
                < ScreenArt.PROMPT_Y + ScreenArt.PROMPT_LINE_DY);
    }

    /** And it is hit where it is drawn, in its own space rather than the menu's. */
    @Test
    void theFirstRunPromptIsHitWhereItIsDrawn() {
        for (int i = 0; i < 2; i++) {
            float middleY = CardArt.toWorldY(ScreenArt.promptButtonY(i), ScreenArt.BUTTON_H)
                    + ScreenArt.BUTTON_H / 2f;
            float x = ScreenArt.promptButtonX() + ScreenArt.BUTTON_W / 2f;
            assertEquals(i, ScreenArt.promptButtonAt(x, middleY));
        }
        float gap = CardArt.toWorldY(ScreenArt.promptButtonY(0) + ScreenArt.BUTTON_H + 2, 0);
        assertEquals(-1, ScreenArt.promptButtonAt(
                ScreenArt.promptButtonX() + 10, gap), "the gap between them");
    }

    /**
     * The run-end panel has two heights: with the trophy band and without. A run
     * that unlocked nothing used to keep the taller one, leaving a hole between
     * the rule and the buttons. Whichever height it is, the panel stays centred
     * and the buttons keep the same clearance from its foot.
     */
    @Test
    void theRunEndPanelShrinksWhenNothingWasUnlocked() {
        assertEquals(444, ScreenArt.endH(true), "the render's own height");
        assertTrue(ScreenArt.endH(false) < ScreenArt.endH(true));
        for (boolean trophies : new boolean[] {true, false}) {
            int top = ScreenArt.endY(trophies);
            int bottom = top + ScreenArt.endH(trophies);
            assertEquals(Theme.WORLD_HEIGHT - bottom, top, "the panel is off centre");
            int buttonsBottom = ScreenArt.endButtonsY(trophies) + ScreenArt.END_BUTTON_H;
            assertTrue(buttonsBottom < bottom, "the buttons hang out of the panel");
            assertEquals(30, bottom - buttonsBottom, "the foot clearance moved");
        }
    }

    /** Both trophy rows have to land inside the taller panel, above the buttons. */
    @Test
    void theUnlockedTrophiesFitAboveTheButtons() {
        int last = ScreenArt.endTrophyY(ScreenArt.END_TROPHIES_SHOWN - 1)
                + ScreenArt.END_TROPHY_SEAL;
        assertTrue(last <= ScreenArt.endButtonsY(true), "a trophy row runs into the buttons");
        assertTrue(ScreenArt.endY(true) + ScreenArt.END_RULE_DY
                < ScreenArt.endTrophyY(0), "the rule is below the first trophy");
    }

    /** A shorter menu must not answer for buttons it does not draw. */
    @Test
    void aShorterColumnDoesNotAnswerForMissingButtons() {
        int x = ScreenArt.COLUMN_X;
        float fourth = CardArt.toWorldY(ScreenArt.buttonY(3), ScreenArt.BUTTON_H)
                + ScreenArt.BUTTON_H / 2f;
        assertEquals(3, ScreenArt.buttonAt(x, 4, x + 10, fourth));
        assertEquals(-1, ScreenArt.buttonAt(x, 3, x + 10, fourth));
    }
}
