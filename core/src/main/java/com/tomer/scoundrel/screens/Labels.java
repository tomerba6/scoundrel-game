package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.EquippedWeapon;
import com.tomer.scoundrel.rules.Move;

/**
 * Short UI labels — pure, so they are unit tested (moved verbatim out of
 * GameScreen): the chooser button text for a move, and the trophy-rail plate
 * describing how much bite the equipped weapon has left.
 */
final class Labels {

    private Labels() {
    }

    /** Chooser button text for a move. */
    static String move(Move move) {
        return switch (move) {
            case Move.FightWithWeapon ignored -> "Use weapon";
            case Move.FightBarehanded ignored -> "Barehanded";
            case Move.TakeWeapon ignored -> "Equip";
            case Move.TakePotion ignored -> "Drink";
            case Move.AvoidRoom ignored -> "Avoid";
        };
    }

    /** The trophy-rail plate: {@code slays anything} fresh, {@code slays < N}, or {@code spent}. */
    static String weaponThreshold(EquippedWeapon weapon) {
        if (weapon.threshold().isEmpty()) {
            return "slays anything";
        }
        int threshold = weapon.threshold().getAsInt();
        return threshold <= 2 ? "spent" : "slays < " + threshold;
    }
}
