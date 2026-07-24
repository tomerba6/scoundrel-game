package com.tomer.scoundrel.rules;

/**
 * Factory for the shipped rulesets: base Scoundrel and its difficulty variants.
 * Every variant keeps the standard 4-card-room turn shape and standard deck,
 * differing only in constants or a swapped strategy.
 */
public final class Rulesets {

    private Rulesets() {
    }

    public static Ruleset standard() {
        return new Ruleset(20, 20, 4, 3, 1,
                new StandardAvoidRule(), new StandardScoring(), new StandardDeck());
    }

    /** Standard, but avoiding is never legal — every room must be faced. */
    public static Ruleset relentless() {
        return new Ruleset(20, 20, 4, 3, 1,
                new NeverAvoidRule(), new StandardScoring(), new StandardDeck());
    }

    /** Standard rules on a thinner life: starting health and the heal cap are both 14. */
    public static Ruleset frail() {
        return new Ruleset(14, 14, 4, 3, 1,
                new StandardAvoidRule(), new StandardScoring(), new StandardDeck());
    }
}
