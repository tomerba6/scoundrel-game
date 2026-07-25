package com.tomer.scoundrel.tutorial;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.rules.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TutorialGuideTest {

    private static final Card GOBLIN = new Card("2C", CardType.MONSTER, 2);
    private static final Move FIGHT = new Move.FightBarehanded(GOBLIN);

    private static TutorialGuide guide() {
        return new TutorialGuide(List.of(
                TutorialStep.say("intro"),
                TutorialStep.act("fight it", FIGHT),
                TutorialStep.say("done")));
    }

    @Test
    void anExplanationBeatAcceptsNoMoveAndAdvancesOnNext() {
        TutorialGuide g = guide();
        assertFalse(g.accepts(FIGHT), "explanation steps gate all moves");
        g.next();
        assertTrue(g.current().isAction(), "Next moved on to the action beat");
    }

    @Test
    void anActionBeatAcceptsOnlyItsExpectedMove() {
        TutorialGuide g = guide();
        g.next();
        assertTrue(g.accepts(FIGHT));
        assertFalse(g.accepts(new Move.AvoidRoom()));
        assertFalse(g.accepts(new Move.TakePotion(GOBLIN)));
    }

    @Test
    void nextDoesNotSkipAnActionBeat() {
        TutorialGuide g = guide();
        g.next();          // onto the action beat
        g.next();          // must not advance past it
        assertTrue(g.accepts(FIGHT), "still waiting on the action");
    }

    @Test
    void onlyTheExpectedMoveAdvancesAnActionBeat() {
        TutorialGuide g = guide();
        g.next();
        g.onMoveApplied(new Move.AvoidRoom()); // wrong move: no advance
        assertTrue(g.accepts(FIGHT));
        g.onMoveApplied(FIGHT);                // right move: advances
        assertFalse(g.isComplete());
        assertFalse(g.current().isAction(), "now on the closing explanation beat");
    }

    @Test
    void runsToCompletionAndThenHasNoCurrentStep() {
        TutorialGuide g = guide();
        g.next();
        g.onMoveApplied(FIGHT);
        g.next();
        assertTrue(g.isComplete());
        assertThrows(IllegalStateException.class, g::current);
    }
}
