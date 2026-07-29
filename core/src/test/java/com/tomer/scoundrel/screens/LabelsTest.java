package com.tomer.scoundrel.screens;

import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.model.EquippedWeapon;
import com.tomer.scoundrel.rules.Move;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void aScoreAboveTheCapReadsAsCapPlusTheFinalPotion() {
        // StandardScoring's other win branch: finish at the cap on a potion and
        // the score is cap + that potion — 20 + 4 here.
        String blurb = Labels.tutorialScore(24, 20);
        assertTrue(blurb.contains("20 + 4"), blurb);
        assertTrue(blurb.contains("negative"), "it should recap the losing score too: " + blurb);
    }
}
