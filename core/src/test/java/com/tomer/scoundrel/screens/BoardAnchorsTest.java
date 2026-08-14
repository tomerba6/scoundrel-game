package com.tomer.scoundrel.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The board's anchors exist once.
 *
 * <p>{@link CardFlight} used to declare its own copy of where the rail is, and
 * the two drifted apart without anything noticing: the equip flight landed its
 * card centred on (96, 646) while the icon it turns into is drawn centred on
 * (64, 650). Measured off a slow-motion capture, the card came to rest at
 * (95, 645) — 31px right of the well, more than its own width at the flight's
 * final 18% scale, and it then snapped left into place.
 *
 * <p>These assertions are the thing that stopped that being expressible twice.
 */
class BoardAnchorsTest {

    @Test
    void equipFlightLandsOnTheRailIcon() {
        int iconCentreX = BoardArt.railIconX() + BoardArt.RAIL_ICON / 2;
        int iconCentreY = BoardArt.railIconY() + BoardArt.RAIL_ICON / 2;

        assertEquals(iconCentreX, CardFlight.EQUIP.toX(),
                "the equip flight must land where the rail icon is drawn");
        assertEquals(iconCentreY, CardFlight.EQUIP.toY(),
                "the equip flight must land where the rail icon is drawn");
    }

    /**
     * The lit block of the depth strip is kept on the board's centre, which is
     * where a swept room is aimed. Only the x is pinned: the sweep's y was tuned
     * against the strip's text line rather than its bar, and nothing here has
     * shown that to be wrong.
     */
    @Test
    void avoidSweepAimsAtTheBoardCentre() {
        assertEquals(640, CardFlight.AVOID.toX(),
                "a swept room flies to the ticker, which sits on the board's centre");
    }
}
