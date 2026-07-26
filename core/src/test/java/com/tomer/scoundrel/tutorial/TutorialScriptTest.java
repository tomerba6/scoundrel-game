package com.tomer.scoundrel.tutorial;

import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.model.GameState;
import com.tomer.scoundrel.model.Status;
import com.tomer.scoundrel.rules.Move;
import com.tomer.scoundrel.rules.Rulesets;
import com.tomer.scoundrel.rules.ScoundrelEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorialScriptTest {

    /**
     * The load-bearing test: play every action step's move through the real
     * engine, in order, and prove each is legal at that point, health never
     * hits zero, and the run ends in a win. If the curated deck and the scripted
     * moves ever fall out of sync, this fails with the offending beat.
     */
    @Test
    void theScriptedRunIsLegalStaysAliveAndWins() {
        ScoundrelEngine engine = new ScoundrelEngine(Rulesets.standard());
        GameState state = engine.newGame(TutorialScript.deck());
        TutorialGuide guide = new TutorialGuide(TutorialScript.steps());

        for (TutorialStep step : TutorialScript.steps()) {
            if (!step.isAction()) {
                guide.next();
                continue;
            }
            Move move = step.expectedMove();
            assertTrue(engine.legalMoves(state).contains(move),
                    "illegal at [" + step.narration() + "] -> " + move
                            + "  legal=" + engine.legalMoves(state));
            assertTrue(guide.accepts(move), "guide out of sync at [" + step.narration() + "]");
            state = engine.apply(state, move).state();
            guide.onMoveApplied(move);
            assertTrue(state.health() > 0 || state.status() == Status.WON,
                    "health hit " + state.health() + " at [" + step.narration() + "]");
        }

        assertTrue(guide.isComplete(), "every step should be consumed");
        assertEquals(Status.WON, state.status(), "the tutorial must end in a win");
        assertTrue(state.health() > 0, "should finish with health to spare, was " + state.health());
    }

    @Test
    void thebeatsAreWellFormedAndTeachEachCardType() {
        assertFalse(TutorialScript.steps().isEmpty());
        for (TutorialStep step : TutorialScript.steps()) {
            assertFalse(step.narration().isBlank(), "every beat needs narration");
        }
        // The deck must actually contain all three card types to teach them.
        assertTrue(TutorialScript.deck().stream().anyMatch(c -> c.type() == CardType.MONSTER));
        assertTrue(TutorialScript.deck().stream().anyMatch(c -> c.type() == CardType.WEAPON));
        assertTrue(TutorialScript.deck().stream().anyMatch(c -> c.type() == CardType.POTION));
    }

    @Test
    void everyActionStepTargetsACardActuallyInTheDeck() {
        // A move against a card the deck never deals could never be made.
        for (TutorialStep step : TutorialScript.steps()) {
            if (step.expectedMove() instanceof Move.CardMove card) {
                assertTrue(TutorialScript.deck().contains(card.targetCard()),
                        "move targets a card not in the deck: " + card.targetCard());
            }
        }
    }
}
