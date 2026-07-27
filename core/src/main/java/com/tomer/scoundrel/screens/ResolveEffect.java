package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.rules.GameEvent;
import com.tomer.scoundrel.rules.Move;

import java.util.List;

/**
 * The cosmetic effect a resolved move drives — the pure decision behind
 * GameScreen's animation routing, lifted out so it is unit tested. The effect is
 * fully determined by the move type (avoiding sweeps, a bare-handed fight
 * strikes, a weapon equips, a potion pours, a weapon kill slices); the resolve's
 * events only carry the amounts, exposed here for the HP feedback.
 */
enum ResolveEffect {
    AVOID, STRIKE, EQUIP, POTION, SLICE;

    static ResolveEffect of(Move move) {
        return switch (move) {
            case Move.AvoidRoom ignored -> AVOID;
            case Move.FightBarehanded ignored -> STRIKE;
            case Move.TakeWeapon ignored -> EQUIP;
            case Move.TakePotion ignored -> POTION;
            case Move.FightWithWeapon ignored -> SLICE;
        };
    }

    /** Total damage taken this resolve — drives the HP-bar shudder. */
    static int damageTaken(List<GameEvent> events) {
        return events.stream()
                .filter(e -> e instanceof GameEvent.MonsterDefeated)
                .mapToInt(e -> ((GameEvent.MonsterDefeated) e).damageTaken())
                .sum();
    }

    /** Total healed this resolve — drives the green flash and the HP hold. */
    static int healed(List<GameEvent> events) {
        return events.stream()
                .filter(e -> e instanceof GameEvent.PotionUsed)
                .mapToInt(e -> ((GameEvent.PotionUsed) e).healed())
                .sum();
    }
}
