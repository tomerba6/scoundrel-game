package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.rules.GameEvent;
import com.tomer.scoundrel.rules.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Characterization of the animation-routing decision lifted out of GameScreen:
 * which effect a move drives, and the HP amounts read from a resolve's events.
 * Pins the whole matrix so the extraction is provably behaviour-preserving.
 */
class ResolveEffectTest {

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
    void everyMoveTypeMapsToItsEffect() {
        assertEquals(ResolveEffect.AVOID, ResolveEffect.of(new Move.AvoidRoom()));
        assertEquals(ResolveEffect.STRIKE, ResolveEffect.of(new Move.FightBarehanded(monster("7S", 7))));
        assertEquals(ResolveEffect.EQUIP, ResolveEffect.of(new Move.TakeWeapon(weapon("5D", 5))));
        assertEquals(ResolveEffect.POTION, ResolveEffect.of(new Move.TakePotion(potion("4H", 4))));
        assertEquals(ResolveEffect.SLICE, ResolveEffect.of(new Move.FightWithWeapon(monster("6C", 6))));
    }

    @Test
    void damageTakenSumsMonsterDefeatedEvents() {
        List<GameEvent> events = List.of(
                new GameEvent.MonsterDefeated(monster("7S", 7), false, 7),
                new GameEvent.MonsterDefeated(monster("6C", 6), true, 1),
                new GameEvent.PotionUsed(potion("4H", 4), 4),
                new GameEvent.RoomDealt(List.of()));
        assertEquals(8, ResolveEffect.damageTaken(events));
    }

    @Test
    void damageTakenIsZeroWithoutAKill() {
        assertEquals(0, ResolveEffect.damageTaken(List.of(
                new GameEvent.WeaponEquipped(weapon("5D", 5)),
                new GameEvent.RoomDealt(List.of()))));
    }

    @Test
    void healedSumsPotionUsedEvents() {
        List<GameEvent> events = List.of(
                new GameEvent.PotionUsed(potion("4H", 4), 4),
                new GameEvent.PotionUsed(potion("3H", 3), 0),
                new GameEvent.PotionWasted(potion("2H", 2)));
        assertEquals(4, ResolveEffect.healed(events));
    }

    @Test
    void healedIsZeroWithoutADrink() {
        assertEquals(0, ResolveEffect.healed(List.of(
                new GameEvent.MonsterDefeated(monster("7S", 7), false, 7))));
    }
}
