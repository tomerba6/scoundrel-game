package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.rules.GameEvent;

/**
 * The event feed's text — the one-line messages the player reads as a move
 * resolves. Pure, so it is unit tested (moved verbatim out of GameScreen): it
 * maps a {@link GameEvent} to its feed line, or null for events the board
 * already shows.
 */
final class FeedText {

    private FeedText() {
    }

    /** Events the player should read; null for ones the board already shows. */
    static String line(GameEvent event) {
        return switch (event) {
            case GameEvent.MonsterDefeated m -> {
                String name = cardName(m.monster());
                if (!m.withWeapon()) {
                    yield "Fought " + name + " barehanded — took " + m.damageTaken();
                }
                yield m.damageTaken() > 0
                        ? "Slew " + name + " — took " + m.damageTaken()
                        : "Slew " + name + " — unharmed";
            }
            case GameEvent.PotionUsed p -> p.healed() > 0
                    ? "Drank " + cardName(p.potion()) + " — healed " + p.healed()
                    : "Drank " + cardName(p.potion()) + " — already full";
            case GameEvent.PotionWasted p ->
                    cardName(p.potion()) + " wasted — one potion a turn";
            case GameEvent.WeaponEquipped w -> "Equipped " + cardName(w.weapon());
            case GameEvent.WeaponDegraded d -> d.newThreshold() <= 2
                    ? "The weapon is spent"
                    : "The weapon dulls — slays < " + d.newThreshold();
            case GameEvent.RoomAvoided ignored -> "Avoided the room";
            default -> null; // RoomDealt is visible on the board; win/loss get the overlay
        };
    }

    /** "the Queen of clubs", "the 7 of hearts" — the fonts have no suit glyphs. */
    static String cardName(Card card) {
        String id = card.id();
        char suitChar = id.charAt(id.length() - 1);
        String suit = switch (suitChar) {
            case 'S' -> "spades";
            case 'H' -> "hearts";
            case 'D' -> "diamonds";
            case 'C' -> "clubs";
            default -> null;
        };
        if (suit == null || id.length() < 2) {
            return card.type().name().toLowerCase() + " " + card.value();
        }
        String rank = switch (id.substring(0, id.length() - 1)) {
            case "J" -> "Jack";
            case "Q" -> "Queen";
            case "K" -> "King";
            case "A" -> "Ace";
            default -> id.substring(0, id.length() - 1);
        };
        return "the " + rank + " of " + suit;
    }
}
