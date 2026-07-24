package com.tomer.scoundrel.rules;

/**
 * A selectable difficulty: a stable {@code id} (persisted as the run's
 * {@code rulesetId}), player-facing text for the menu, the {@link Ruleset} it
 * plays with, and whether runs in it count toward achievements. Pure data — the
 * catalog of shipped modes is {@link GameModes}.
 */
public record GameMode(
        String id,
        String title,
        String description,
        Ruleset ruleset,
        boolean tracksAchievements) {
}
