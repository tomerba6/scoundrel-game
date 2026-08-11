package com.tomer.scoundrel.screens;

/**
 * Where the tutorial's callout goes, and where its notch points.
 *
 * <p>Design space, y measured downward. The callout prefers to sit above the
 * card it is talking about and drops below when there is no room; the notch
 * always points back at the card, so the two have to agree. A callout that moved
 * below its target while its notch kept pointing down is the bug this exists to
 * make impossible, and it is invisible in a still frame.
 */
final class CalloutPlacement {

    /** Clearance from the stage edge, and from the panel's own edge for the notch. */
    static final int MARGIN = 12;
    static final int NOTCH_W = 20;
    static final int NOTCH_H = 14;

    /**
     * @param below    which way the notch points: down at the card when the callout
     *                 is above it, up at it when below
     * @param notchX   the notch's left edge, or -1 when there is no notch
     */
    record Placement(int x, int y, boolean below, int notchX) {
        boolean hasNotch() {
            return notchX >= 0;
        }
    }

    private CalloutPlacement() {
    }

    /**
     * Under the room row and centred across the stage, with no notch — an
     * explanation beat points at nothing in particular.
     *
     * <p>Two placings were rejected. Dead-centre covers the room, and a player
     * being told how cards work cannot see the cards while being told. Above the
     * row is where a <em>targeted</em> callout goes, but an explanation beat runs
     * to five lines and that pushes it into the HUD; the space under the room is
     * empty and deep enough for the tallest of them.
     */
    static Placement belowRow(int rowY, int rowH, int calloutW, int calloutH,
                              int gap, int worldW, int worldH) {
        int y = Math.min(rowY + rowH + gap, worldH - calloutH - MARGIN);
        return new Placement((worldW - calloutW) / 2, Math.max(MARGIN, y), false, -1);
    }

    static Placement place(int targetX, int targetY, int targetW, int targetH,
                           int calloutW, int calloutH, int gap, int worldW) {
        int above = targetY - gap - calloutH;
        boolean below = above < MARGIN;
        int y = below ? targetY + targetH + gap : above;

        int centre = targetX + targetW / 2;
        int x = clamp(centre - calloutW / 2, MARGIN, worldW - calloutW - MARGIN);
        // The notch tracks the card, not the panel — but once the panel has been
        // clamped away from the card the notch has to stay on it, or it detaches
        // and points at nothing.
        int notchX = clamp(centre - NOTCH_W / 2,
                x + MARGIN, x + calloutW - NOTCH_W - MARGIN);
        return new Placement(x, y, below, notchX);
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }
}
