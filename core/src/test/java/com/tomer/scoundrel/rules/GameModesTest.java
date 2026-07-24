package com.tomer.scoundrel.rules;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameModesTest {

    @Test
    void catalogHasUniqueIdsAndCopyWithStandardFirst() {
        List<GameMode> all = GameModes.all();
        assertEquals(3, all.size());
        assertEquals("standard", all.get(0).id(), "Standard leads the menu");
        Set<String> ids = new HashSet<>();
        for (GameMode m : all) {
            assertTrue(ids.add(m.id()), "duplicate id " + m.id());
            assertFalse(m.title().isBlank(), m.id() + " has a blank title");
            assertFalse(m.description().isBlank(), m.id() + " has a blank description");
            assertNotNull(m.ruleset(), m.id() + " has no ruleset");
        }
    }

    @Test
    void onlyStandardTracksAchievements() {
        for (GameMode m : GameModes.all()) {
            assertEquals(m.id().equals("standard"), m.tracksAchievements(), m.id());
        }
    }

    @Test
    void eachModeCarriesItsMatchingRuleset() {
        assertTrue(GameModes.byId("relentless").orElseThrow().ruleset().avoidRule()
                instanceof NeverAvoidRule);
        assertEquals(14, GameModes.byId("frail").orElseThrow().ruleset().startingHealth());
        assertEquals(20, GameModes.STANDARD.ruleset().startingHealth());
    }

    @Test
    void byIdFindsAKnownModeAndIsEmptyForAnUnknownOne() {
        assertEquals("Relentless", GameModes.byId("relentless").orElseThrow().title());
        assertEquals(Optional.empty(), GameModes.byId("no-such-mode"));
        assertEquals(Optional.empty(), GameModes.byId(""));
    }
}
