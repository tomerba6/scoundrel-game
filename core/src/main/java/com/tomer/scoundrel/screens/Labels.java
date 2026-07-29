package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.EquippedWeapon;
import com.tomer.scoundrel.rules.Move;

/**
 * Short UI labels — pure, so they are unit tested (moved verbatim out of
 * GameScreen): the chooser button text for a move, the trophy-rail plate
 * describing how much bite the equipped weapon has left, and the
 * Tutorial-complete line that reads a winning score back as its scoring rule.
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

    /**
     * The line under the end-screen score, naming where that number came from —
     * the death score especially, which charges you for monsters still in the
     * face-down dungeon that the player never saw. The penalty needs no extra
     * state: {@code StandardScoring} makes it exactly {@code health - score}.
     */
    static String scoreBreakdown(int score, int health, int healthCap, boolean won) {
        if (!won) {
            int monstersLeft = health - score;
            return monstersLeft == 0
                    ? health + " health, an empty dungeon"
                    : health + " health, minus " + monstersLeft + " still in the dungeon";
        }
        if (score > healthCap) {
            return healthCap + " at the cap + the " + (score - healthCap) + " you finished on";
        }
        return "the health you kept";
    }

    /**
     * The Tutorial-complete line: a winning score read back as the rule that
     * produced it, then the losing rule as a parting recap. Covers both of
     * {@code StandardScoring}'s win branches — the health you kept, or the cap
     * plus the potion you finished on when the score runs over the cap.
     */
    static String tutorialScore(int score, int healthCap) {
        String win = score > healthCap
                ? "You ended at a full " + healthCap + " on a potion, so it scores "
                        + healthCap + " + " + (score - healthCap) + " — the only way past "
                        + healthCap + "."
                : "A cleared dungeon scores the health you kept — your " + score + ".";
        return win + " Die instead and the score goes negative: your health, minus every "
                + "monster still left in the dungeon.";
    }
}
