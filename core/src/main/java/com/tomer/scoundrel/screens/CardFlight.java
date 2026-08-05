package com.tomer.scoundrel.screens;

/**
 * A card leaving the room: swept to the depth ticker when a room is avoided, or
 * carried down to the rail when a weapon is equipped.
 *
 * <p>Both are the same motion — a few whole-pixel hops with the card shrinking
 * as it goes — so they are one set of arithmetic differing only in where they
 * land, how many hops, and how fast they shrink. A hop holds for its whole
 * duration; there is no position between one and the next, which is what makes
 * it read as steps rather than a slide.
 */
final class CardFlight {

    /** Effects run at 12fps, and a hop is always a whole number of frames. */
    private static final float FRAME = 1f / 12f;

    /** The anchors a card can fly to, from the board geometry. */
    static final int TICKER_X = 640;
    static final int TICKER_Y = 60;
    static final int RAIL_X = 96;
    static final int RAIL_Y = 646;

    /**
     * @param toX      where the card lands
     * @param toY      where the card lands
     * @param hopTime  how long each hop holds
     * @param scales   the card's size at each hop, as a percentage
     */
    record Flight(int toX, int toY, float hopTime, int[] scales) {

        int hops() {
            return scales.length;
        }

        float total() {
            return hops() * hopTime;
        }

        boolean finished(float elapsed) {
            return elapsed >= total();
        }
    }

    /**
     * Four cards hop into the ticker together. The art direction quotes 0.20s
     * a hop, which is 2.4 frames at 12fps — not on the grid — so it is the 2
     * frames it was tuned for and the sweep finishes at 667ms rather than the
     * quoted 800. A hop ending mid-frame would slide instead of stepping.
     */
    static final Flight AVOID = new Flight(TICKER_X, TICKER_Y, 2 * FRAME,
            new int[] {100, 72, 44, 16});

    /** Three hops to the rail, shrinking to the icon it becomes. */
    static final Flight EQUIP = new Flight(RAIL_X, RAIL_Y, 3 * FRAME,
            new int[] {100, 55, 18});

    private CardFlight() {
    }

    /** Which hop is showing, clamped to the last one once the flight is over. */
    private static int hopOf(Flight flight, float elapsed) {
        int hop = (int) Math.floor(elapsed / flight.hopTime() + 1e-4);
        return Math.max(0, Math.min(flight.hops() - 1, hop));
    }

    static int x(Flight flight, int fromX, float elapsed) {
        return lerp(fromX, flight.toX(), hopOf(flight, elapsed), flight.hops());
    }

    static int y(Flight flight, int fromY, float elapsed) {
        return lerp(fromY, flight.toY(), hopOf(flight, elapsed), flight.hops());
    }

    /** The card's size right now, as a percentage of its board size. */
    static int scale(Flight flight, float elapsed) {
        return flight.scales()[hopOf(flight, elapsed)];
    }

    /**
     * Hop 0 sits at the start and the last hop sits exactly on the anchor, so a
     * flight always begins where the card was and ends where it belongs.
     */
    private static int lerp(int from, int to, int hop, int hops) {
        if (hops <= 1) {
            return to;
        }
        return Math.round(from + (to - from) * (hop / (float) (hops - 1)));
    }
}
