package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Card;

/**
 * Maps a card to the sprite region drawn for it, per the naming contract in
 * HANDOFF.md §1: {@code creature_<value>_<name>_<suit>},
 * {@code weapon_<value>_<name>}, {@code potion_<value>_<name>}, with the value
 * zero-padded and the creature's suit taken from the card's id.
 *
 * <p>Pure string work, so the whole 44-card mapping is provable headlessly —
 * the failure it guards against is a card quietly showing the wrong creature.
 */
final class CardSprites {

    /** Creature names by ordered value; index 0 is value 2. */
    private static final String[] CREATURES = {
        "cellar_rat", "carrion_bat", "grave_slime", "bone_crawler", "goblin_cutter",
        "ghoul", "tomb_spider", "gaunt_knight", "deep_ogre", "flayed_priest",
        "widow_queen", "warden", "the_debt",
    };

    /** Weapon names by value; index 0 is value 2. */
    private static final String[] WEAPONS = {
        "rusted_shiv", "iron_nail", "hatchet", "short_sword", "mace",
        "broadsword", "war_pick", "broadaxe", "greatsword",
    };

    /** Potion names by value; index 0 is value 2. */
    private static final String[] POTIONS = {
        "dram", "vial", "phial", "draught", "flask",
        "carafe", "decanter", "amphora", "flagon",
    };

    private CardSprites() {
    }

    /** The base region for a card — frame 1 of its idle cycle, pixel-identical. */
    static String regionName(Card card) {
        int value = card.value();
        return switch (card.type()) {
            case MONSTER -> "creature_" + pad(value) + "_" + name(CREATURES, value) + "_" + suit(card);
            case WEAPON -> "weapon_" + pad(value) + "_" + name(WEAPONS, value);
            case POTION -> "potion_" + pad(value) + "_" + name(POTIONS, value);
        };
    }

    /**
     * The stem to pass to {@code atlas.findRegions}, which returns the five idle
     * frames in index order (§7).
     */
    static String idleStem(Card card) {
        return regionName(card) + "_idle";
    }

    /**
     * Clubs or spades, read from the card's id rather than anything derived —
     * the two suits are separate drawings, not a runtime recolour (§6).
     */
    private static String suit(Card card) {
        return card.id().endsWith("C") ? "clubs" : "spades";
    }

    private static String name(String[] names, int value) {
        int index = value - 2;
        if (index < 0 || index >= names.length) {
            throw new IllegalArgumentException("no sprite for value " + value);
        }
        return names[index];
    }

    private static String pad(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }
}
