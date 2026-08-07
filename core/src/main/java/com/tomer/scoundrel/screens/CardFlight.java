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
     * @param toX         where the card lands
     * @param toY         where the card lands
     * @param hopTime     how long each hop holds
     * @param staggerTime how long after the card to its left a card sets off
     * @param scales      the card's size at each hop, as a percentage
     */
    record Flight(int toX, int toY, float hopTime, float staggerTime, int[] scales) {

        int hops() {
            return scales.length;
        }

        float total() {
            return hops() * hopTime;
        }

        /** How long until the last of {@code cards} has landed. */
        float totalFor(int cards) {
            return total() + Math.max(0, cards - 1) * staggerTime;
        }

        boolean finished(float elapsed) {
            return elapsed >= total();
        }
    }

    /**
     * The whole room hops into the ticker together, as release 1's did — one
     * parallel action per card with no delay between them. Avoiding is a single
     * decision about four cards, and emptying them left to right made it read
     * as four; it also cost a frame a card for the privilege.
     *
     * <p>The art direction quotes 0.20s a hop, which is 2.4 frames at 12fps —
     * not on the grid — and at the 2 frames it was rounded to, a room you had
     * already decided to be rid of took two thirds of a second to leave. Three
     * hops of one frame: the room is gone in 250ms, release 1's beat.
     */
    static final Flight AVOID = new Flight(TICKER_X, TICKER_Y, FRAME, 0f,
            new int[] {100, 58, 16});

    /**
     * Three hops to the rail, shrinking to the icon it becomes. One card, so
     * nothing to stagger against. The quoted 0.24s a hop put three quarters of
     * a second between taking a weapon and being able to use it.
     */
    static final Flight EQUIP = new Flight(RAIL_X, RAIL_Y, FRAME, 0f,
            new int[] {100, 55, 18});

    /**
     * A card arriving in the room, growing as it comes, three hops of one
     * frame. Release 1 dealt on a 0.04s stagger, which at 12fps rounds to
     * nothing — but dealt with no stagger at all the room lands as one event
     * rather than as four cards. So it is a whole frame between cards: the
     * smallest gap this grid can express, and enough to see each one land.
     */
    static Flight dealTo(int toX, int toY) {
        return new Flight(toX, toY, FRAME, FRAME, new int[] {28, 64, 100});
    }

    /**
     * A card that was already on the board moving to its new slot as the room
     * closes up around a resolved card. It never changes size — it is already
     * the right one — and, unlike a deal, it does not stagger.
     *
     * <p>That difference is deliberate. Cards coming up out of the dungeon are
     * four separate events and read better one after another. The survivors
     * shifting along are one row re-centring itself, and cascading them made a
     * resolved card look as though it had set off a second deal.
     */
    static Flight slideTo(int toX, int toY) {
        return new Flight(toX, toY, FRAME, 0f, new int[] {100, 100, 100});
    }

    private CardFlight() {
    }

    /**
     * A card's own clock. On a staggered flight each card sets off after the one
     * before it and sits where it was until then; on an unstaggered one every
     * card shares the same clock and they move together.
     */
    static float localTime(Flight flight, int index, float elapsed) {
        return elapsed - index * flight.staggerTime();
    }

    /** Whether this card has set off yet. */
    static boolean started(Flight flight, int index, float elapsed) {
        return localTime(flight, index, elapsed) >= 0f;
    }

    /**
     * Whether this card has arrived. The depth ticker asks it of every card
     * still on its way up out of the dungeon — the engine gave the card up when
     * the move was applied, but until it lands it is still between the ticks and
     * the table, and its tick has to stay lit.
     */
    static boolean landed(Flight flight, int index, float elapsed) {
        return localTime(flight, index, elapsed) >= flight.total();
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
