package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.model.EquippedWeapon;
import com.tomer.scoundrel.rules.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization of the short UI labels lifted out of GameScreen — the chooser
 * button text and the weapon-threshold plate. Pins the exact wording so the
 * extraction is provably behaviour-preserving.
 */
class LabelsTest {

    private static Card monster(String id, int value) {
        return new Card(id, CardType.MONSTER, value);
    }

    private static final Card WEAPON = new Card("5D", CardType.WEAPON, 5);

    @Test
    void moveLabelsCoverEveryMove() {
        Card any = monster("7S", 7);
        assertEquals("Use weapon", Labels.move(new Move.FightWithWeapon(any)));
        assertEquals("Barehanded", Labels.move(new Move.FightBarehanded(any)));
        assertEquals("Equip", Labels.move(new Move.TakeWeapon(WEAPON)));
        assertEquals("Drink", Labels.move(new Move.TakePotion(new Card("4H", CardType.POTION, 4))));
        assertEquals("Avoid", Labels.move(new Move.AvoidRoom()));
    }

    @Test
    void aFreshWeaponSlaysAnything() {
        assertEquals("slays anything", Labels.weaponThreshold(new EquippedWeapon(WEAPON)));
    }

    @Test
    void aWeaponReadsItsRemainingBite() {
        EquippedWeapon slewSix = new EquippedWeapon(WEAPON, List.of(monster("6C", 6)));
        assertEquals("slays < 6", Labels.weaponThreshold(slewSix));
    }

    @Test
    void aWeaponThatSlewATwoIsSpent() {
        EquippedWeapon slewTwo = new EquippedWeapon(WEAPON, List.of(monster("2S", 2)));
        assertEquals("spent", Labels.weaponThreshold(slewTwo));
    }

    @Test
    void anOrdinaryWinReadsAsTheHealthYouKept() {
        String blurb = Labels.tutorialScore(10, 20);
        assertTrue(blurb.contains("10"), blurb);
        assertTrue(blurb.contains("negative"), "it should recap the losing score too: " + blurb);
    }

    /** Same font limit as the tutorial narration — an unmapped glyph renders blank. */
    @Test
    void everyLabelUsesCharactersTheFontCanRender() {
        String extras = "—–×•"; // must track Theme.EXTRA_CHARS
        List<String> labels = List.of(
                Labels.scoreBreakdown(-174, -9, 20, false),
                Labels.scoreBreakdown(-2, -2, 20, false),
                Labels.scoreBreakdown(11, 11, 20, true),
                Labels.scoreBreakdown(24, 20, 20, true),
                Labels.tutorialScore(10, 20),
                Labels.tutorialScore(24, 20));
        for (String text : labels) {
            for (char c : text.toCharArray()) {
                boolean renderable = (c >= 0x20 && c <= 0x7E) || extras.indexOf(c) >= 0;
                assertTrue(renderable, "unrenderable U+"
                        + Integer.toHexString(c).toUpperCase() + " ('" + c + "') in: " + text);
            }
        }
    }

    @Test
    void aLossBreakdownNamesTheDungeonPenalty() {
        // ScoringTest's case: died at -3 with Q(12) + 9 face-down, scoring -24.
        // The penalty is recoverable from the pair: health - score = 21.
        String line = Labels.scoreBreakdown(-24, -3, 20, false);
        assertTrue(line.contains("-3"), line);
        assertTrue(line.contains("21"), "should name the 21 of monsters left: " + line);
    }

    @Test
    void aLossOnTheLastCardHasNoDungeonPenaltyToName() {
        // Nothing left face-down, so "minus 0" would be noise.
        String line = Labels.scoreBreakdown(-2, -2, 20, false);
        assertTrue(line.contains("-2"), line);
        assertFalse(line.contains("0"), "should not read as a 0 penalty: " + line);
    }

    @Test
    void anOrdinaryWinBreakdownIsJustTheHealthKept() {
        assertEquals("the health you kept", Labels.scoreBreakdown(11, 11, 20, true));
    }

    @Test
    void aWinBreakdownAboveTheCapNamesTheFinalPotion() {
        String line = Labels.scoreBreakdown(24, 20, 20, true);
        assertTrue(line.contains("20"), line);
        assertTrue(line.contains("4"), "should name the 4-potion it finished on: " + line);
    }

    @Test
    void aScoreAboveTheCapReadsAsCapPlusTheFinalPotion() {
        // StandardScoring's other win branch: finish at the cap on a potion and
        // the score is cap + that potion — 20 + 4 here.
        String blurb = Labels.tutorialScore(24, 20);
        assertTrue(blurb.contains("20 + 4"), blurb);
        assertTrue(blurb.contains("negative"), "it should recap the losing score too: " + blurb);
    }
}
