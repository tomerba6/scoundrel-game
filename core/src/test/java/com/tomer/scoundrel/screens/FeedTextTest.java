package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.rules.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Characterization of the event-feed text lifted out of GameScreen — the exact
 * one-liners the player reads. Pins the current wording so the extraction is
 * provably behaviour-preserving and any future edit is deliberate.
 */
class FeedTextTest {

    private static Card monster(String id, int value) {
        return new Card(id, CardType.MONSTER, value);
    }

    private static Card weapon(String id, int value) {
        return new Card(id, CardType.WEAPON, value);
    }

    private static Card potion(String id, int value) {
        return new Card(id, CardType.POTION, value);
    }

    @Test
    void barehandedKillReadsTheFullValueTaken() {
        assertEquals("Fought the Queen of clubs barehanded — took 12",
                FeedText.line(new GameEvent.MonsterDefeated(monster("QC", 12), false, 12)));
    }

    @Test
    void weaponKillReadsTheDamageOrUnharmed() {
        assertEquals("Slew the 7 of spades — took 2",
                FeedText.line(new GameEvent.MonsterDefeated(monster("7S", 7), true, 2)));
        assertEquals("Slew the 6 of clubs — unharmed",
                FeedText.line(new GameEvent.MonsterDefeated(monster("6C", 6), true, 0)));
    }

    @Test
    void potionReadsHealedOrAlreadyFull() {
        assertEquals("Drank the 4 of hearts — healed 4",
                FeedText.line(new GameEvent.PotionUsed(potion("4H", 4), 4)));
        assertEquals("Drank the 3 of hearts — already full",
                FeedText.line(new GameEvent.PotionUsed(potion("3H", 3), 0)));
    }

    @Test
    void wastedPotionReadsOnePotionATurn() {
        assertEquals("the 3 of hearts wasted — one potion a turn",
                FeedText.line(new GameEvent.PotionWasted(potion("3H", 3))));
    }

    @Test
    void weaponEquipReadsTheCard() {
        assertEquals("Equipped the 5 of diamonds",
                FeedText.line(new GameEvent.WeaponEquipped(weapon("5D", 5))));
    }

    @Test
    void weaponDegradeReadsTheThresholdOrSpent() {
        assertEquals("The weapon dulls — slays < 6",
                FeedText.line(new GameEvent.WeaponDegraded(weapon("5D", 5), 6)));
        assertEquals("The weapon is spent",
                FeedText.line(new GameEvent.WeaponDegraded(weapon("5D", 5), 2)));
    }

    @Test
    void avoidReadsAvoidedTheRoom() {
        assertEquals("Avoided the room", FeedText.line(new GameEvent.RoomAvoided(List.of())));
    }

    @Test
    void eventsTheBoardAlreadyShowsHaveNoLine() {
        assertNull(FeedText.line(new GameEvent.RoomDealt(List.of())));
        assertNull(FeedText.line(new GameEvent.GameWon(20)));
        assertNull(FeedText.line(new GameEvent.GameLost(-5)));
    }

    @Test
    void cardNameNamesEveryRankAndSuit() {
        assertEquals("the Ace of spades", FeedText.cardName(monster("AS", 14)));
        assertEquals("the King of clubs", FeedText.cardName(monster("KC", 13)));
        assertEquals("the Queen of spades", FeedText.cardName(monster("QS", 12)));
        assertEquals("the Jack of clubs", FeedText.cardName(monster("JC", 11)));
        assertEquals("the 10 of diamonds", FeedText.cardName(weapon("10D", 10)));
        assertEquals("the 2 of hearts", FeedText.cardName(potion("2H", 2)));
    }

    @Test
    void cardNameFallsBackToTypeAndValueForAnUnrecognisedId() {
        // Defensive branch: a card whose id ends in no known suit letter.
        assertEquals("monster 2", FeedText.cardName(monster("2X", 2)));
    }
}
