package com.tomer.scoundrel.model;

import java.util.List;

/**
 * Immutable snapshot of a whole game. Index 0 of {@code dungeon} is the top
 * of the face-down pile. {@code weapon}, {@code lastResolvedCard} and
 * {@code score} are null when absent ({@code score} is set only once the
 * game is over).
 */
public record GameState(
        List<Card> dungeon,
        List<Card> room,
        EquippedWeapon weapon,
        int health,
        int potionsUsedThisRoom,
        int cardsResolvedThisTurn,
        boolean previousRoomAvoided,
        Card lastResolvedCard,
        Status status,
        Integer score) {

    public GameState {
        dungeon = List.copyOf(dungeon);
        room = List.copyOf(room);
    }

    /** True once any card in the current room has been resolved; gates avoiding. */
    public boolean roomResolutionStarted() {
        return cardsResolvedThisTurn > 0;
    }

    /**
     * How many monsters are still face-down in the dungeon.
     *
     * <p>The same cards a loss score is charged for — the room's are not
     * counted, nor the weapons and potions down there with them. It is the one
     * figure that explains a death score, which is otherwise an unexplained
     * negative number on the run-end panel.
     */
    public int monstersRemaining() {
        return (int) dungeon.stream().filter(card -> card.type() == CardType.MONSTER).count();
    }
}
