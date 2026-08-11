package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A row of buttons whose widths come from their labels, laid out centred with a
 * fixed gap — the run-end panel's four, which the render shows at 112/119/106/122
 * rather than at one shared width.
 *
 * <p>The measuring needs a font, so it is passed in and the placing is
 * arithmetic. Same seam as {@link TextWrap}.
 */
class ButtonRowTest {

    /** A fake face: ten pixels a character plus fixed padding either side. */
    private static final ToIntFunction<String> TEN = s -> s.length() * 10 + 20;

    @Test
    void oneButtonIsCentredOnTheRow() {
        List<ButtonRow.Slot> slots = ButtonRow.lay(List.of("AB"), 0, 1000, 10, TEN);
        assertEquals(1, slots.size());
        assertEquals(40, slots.get(0).width());
        assertEquals(480, slots.get(0).x(), "centred on 1000");
    }

    @Test
    void widthsFollowTheLabelsRatherThanBeingShared() {
        List<ButtonRow.Slot> slots = ButtonRow.lay(List.of("A", "BBBB"), 0, 1000, 10, TEN);
        assertEquals(30, slots.get(0).width());
        assertEquals(60, slots.get(1).width());
    }

    @Test
    void theGapBetweenNeighboursIsExact() {
        List<ButtonRow.Slot> slots = ButtonRow.lay(List.of("AA", "BB", "CC"), 0, 1000, 10, TEN);
        for (int i = 1; i < slots.size(); i++) {
            ButtonRow.Slot previous = slots.get(i - 1);
            assertEquals(10, slots.get(i).x() - (previous.x() + previous.width()));
        }
    }

    /** The whole run, gaps included, sits centred — that is what "a row" means. */
    @Test
    void theWholeRunIsCentredNotJustTheFirstButton() {
        List<ButtonRow.Slot> slots = ButtonRow.lay(List.of("AA", "BBBB", "C"), 0, 1000, 10, TEN);
        ButtonRow.Slot first = slots.get(0);
        ButtonRow.Slot last = slots.get(slots.size() - 1);
        int left = first.x();
        int right = last.x() + last.width();
        assertEquals(1000 - right, left, "unequal margins either side");
    }

    /** Whole pixels only; a button on a half pixel is the blur the kit avoids. */
    @Test
    void everyEdgeLandsOnAWholePixel() {
        List<ButtonRow.Slot> slots = ButtonRow.lay(List.of("ODD", "LENGTHS", "HERE"), 0, 1001, 11, TEN);
        for (ButtonRow.Slot slot : slots) {
            assertEquals(slot.x(), Math.round((float) slot.x()));
            assertTrue(slot.width() > 0);
        }
    }

    @Test
    void anEmptyRowLaysNothingRatherThanThrowing() {
        assertEquals(List.of(), ButtonRow.lay(List.of(), 0, 1000, 10, TEN));
    }

    /**
     * The real row: four labels at the render's own widths must land on the
     * render's own x positions. Note it is centred on the <b>panel</b>, whose
     * centre is 638 — centring on the stage's 640 puts every button two pixels
     * right of where the reference has it.
     */
    @Test
    void theRunEndRowMatchesTheReferenceRender() {
        ToIntFunction<String> measured = label -> switch (label) {
            case "NEW GAME" -> 112;
            case "MAIN MENU" -> 119;
            case "TROPHIES" -> 106;
            case "THE LEDGER" -> 122;
            default -> throw new AssertionError(label);
        };
        List<ButtonRow.Slot> slots = ButtonRow.lay(
                List.of("NEW GAME", "MAIN MENU", "TROPHIES", "THE LEDGER"),
                ScreenArt.END_X, ScreenArt.END_W, 10, measured);
        assertEquals(393, slots.get(0).x());
        assertEquals(515, slots.get(1).x());
        assertEquals(644, slots.get(2).x());
        assertEquals(760, slots.get(3).x());
    }
}
