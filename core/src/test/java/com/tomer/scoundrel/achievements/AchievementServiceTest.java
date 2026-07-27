package com.tomer.scoundrel.achievements;

import com.tomer.scoundrel.model.Status;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.tomer.scoundrel.achievements.AchievementsTestData.context;
import static com.tomer.scoundrel.achievements.AchievementsTestData.run;
import static com.tomer.scoundrel.achievements.AchievementsTestData.summary;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementServiceTest {

    private static Achievement always(String id) {
        return new Achievement(id, id, "always", false, ctx -> true);
    }

    private static Achievement never(String id) {
        return new Achievement(id, id, "never", false, ctx -> false);
    }

    private static final AchievementContext ANY =
            context(summary(Status.WON, 10, 10, 60, 0, 0, false));

    private static List<String> ids(List<Achievement> earned) {
        return earned.stream().map(Achievement::id).toList();
    }

    @Test
    void returnsEveryEarnedAchievementNotYetUnlocked() {
        List<Achievement> catalog = List.of(always("a"), always("b"), never("c"));
        assertEquals(List.of("a", "b"),
                ids(AchievementService.newlyEarned(catalog, ANY, Set.of())));
    }

    @Test
    void skipsAchievementsAlreadyUnlocked() {
        List<Achievement> catalog = List.of(always("a"), always("b"));
        assertEquals(List.of("b"),
                ids(AchievementService.newlyEarned(catalog, ANY, Set.of("a"))));
    }

    @Test
    void returnsEmptyWhenNothingNewIsEarned() {
        List<Achievement> catalog = List.of(never("a"), never("b"));
        assertTrue(AchievementService.newlyEarned(catalog, ANY, Set.of()).isEmpty());
    }

    @Test
    void theRealCatalogUnlocksSeveralAtOnceFromOneContext() {
        // A first, full-health, sub-80s win with no bare-handed kills and a flawless room.
        AchievementContext ctx = context(
                summary(Status.WON, 20, 20, 45, 0, 0, true),
                run(Status.WON, 20, 12));
        List<String> earned = ids(AchievementService.newlyEarned(Achievements.all(), ctx, Set.of()));

        assertTrue(earned.contains("first_blood"));
        assertTrue(earned.contains("untarnished"));
        assertTrue(earned.contains("speedrunner"));
        assertTrue(earned.contains("blademaster"));
        assertTrue(earned.contains("flawless_room"));
        assertFalse(earned.contains("on_the_brink"));
        assertFalse(earned.contains("rock_bottom"));
    }
}
