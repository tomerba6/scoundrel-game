package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.achievements.Achievement;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * One entry in TROPHIES: what the row says and whether its seal is filled.
 *
 * <p>The seal's fill is the <em>only</em> difference between earned and locked
 * — HANDOFF §11 rules out a padlock glyph and a greyscale filter, so an empty
 * well is the locked state. A hidden trophy gives away neither its name nor its
 * rule until it is won, which is the whole point of hiding it.
 *
 * <p>The date a trophy was won is kept, which the reference render drops. It is
 * real information the row has the width for.
 */
record TrophyEntry(String title, String description, String status, boolean earned) {

    private static final DateTimeFormatter DAY =
            DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH).withZone(ZoneId.systemDefault());

    /** {@code earnedAt} is null for a trophy that has not been won. */
    static TrophyEntry of(Achievement achievement, Instant earnedAt) {
        boolean earned = earnedAt != null;
        boolean concealed = achievement.hidden() && !earned;
        return new TrophyEntry(
                concealed ? "???" : achievement.title().toUpperCase(Locale.ROOT),
                concealed ? "A HIDDEN TROPHY — EARN IT TO REVEAL"
                        : achievement.description().toUpperCase(Locale.ROOT),
                earned ? "EARNED " + DAY.format(earnedAt).toUpperCase(Locale.ROOT) : "LOCKED",
                earned);
    }

    int rowColour() {
        return earned ? ScreenArt.ROW_EARNED : ScreenArt.ROW_LOCKED;
    }

    int sealColour() {
        return earned ? ScreenArt.SEAL_EARNED : ScreenArt.SEAL_LOCKED;
    }

    /** Earned rows are cream; locked ones sit back without being unreadable. */
    int textColour() {
        return earned ? ScreenArt.BODY : ScreenArt.TROPHY_LOCKED_TEXT;
    }
}
