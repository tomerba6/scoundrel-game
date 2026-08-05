package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.rules.CardDefinition;
import com.tomer.scoundrel.rules.StandardDeck;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A developer-only art inspector, opened with F9, closed with Escape and
 * switched between views with Tab. It exists to answer the verify questions in
 * the art conversion — is the art crisp at an integer scale, are all 31
 * objects on their right ranks, do the idle cycles run — without needing a run
 * of the actual game to reach them.
 *
 * <p>Not reachable from any menu, and drawn with a plain batch rather than
 * Scene2D: it is a measuring instrument, not part of the game.
 */
public final class SpriteLab extends ScreenAdapter {

    /** The stage background — dark enough to show colour fringing. */
    private static final Color BACKDROP = Color.valueOf("100c09");

    /** ROOM shows four framed cards; SHEET shows every object by rank. */
    private enum View { ROOM, SHEET }

    private final ScoundrelGame game;
    private final Theme theme;
    private final Sprites sprites;
    private final CardFrame cardFrame;
    private final SliceArt sliceArt;
    private final PixelViewport viewport;
    private final SpriteBatch batch;

    private final List<Card> deck = new ArrayList<>();
    /** Per-card start offsets, assigned once — never recomputed per frame. */
    private final Map<String, Float> idleOffsets = new HashMap<>();
    private View view = View.ROOM;
    /** R toggles the generated outline on, the way a weapon kill flashes it. */
    private boolean showRim;
    /** The card being killed and how far into the effect it is, or null. */
    private Card killing;
    private float killElapsed;
    /** S slows effects 8x. Sub-second animation cannot be screenshotted at speed. */
    private boolean slowMotion;
    /** The card taking a bare-handed exchange, and how far into it. */
    private Card struckBare;
    private float bareElapsed;
    private float elapsed;
    /** The card under the pointer, or null. Only it animates. */
    private Card hovered;

    public SpriteLab(ScoundrelGame game, Theme theme, Sprites sprites) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        this.cardFrame = new CardFrame(theme);
        this.sliceArt = new SliceArt(CardArt.CARD_W, CardArt.CARD_H);
        // One fixed virtual resolution, so the layout numbers are literal and
        // the art is guaranteed to land on whole pixels.
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.batch = new SpriteBatch();
        Random random = new Random();
        for (CardDefinition def : new StandardDeck().cards()) {
            Card card = new Card(def.id(), def.type(), def.value());
            deck.add(card);
            idleOffsets.put(card.id(), IdleCycle.randomOffset(random));
        }
    }

    /**
     * The region to draw for a card right now. Only creatures have idle frames
     * (none were drawn for weapons or potions), so everything else is its
     * static base sprite.
     */
    private TextureRegion current(Card card, boolean animating) {
        if (card.type() != CardType.MONSTER) {
            return sprites.region(CardSprites.regionName(card));
        }
        Array<TextureRegion> frames = sprites.frames(CardSprites.idleStem(card));
        int index = IdleCycle.frameIndex(
                elapsed, idleOffsets.get(card.id()), frames.size, animating);
        return frames.get(index);
    }

    /** The room card under the pointer, reusing the tested hit-test. */
    private Card hoveredIn(List<Card> room) {
        Vector2 point = viewport.unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        List<CardHitRegions.CardRect> rects = new ArrayList<>();
        for (int i = 0; i < room.size(); i++) {
            rects.add(new CardHitRegions.CardRect(room.get(i), CardArt.slotX(i),
                    CardArt.toWorldY(CardArt.SLOT_Y, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H));
        }
        return CardHitRegions.cardAt(rects, point.x, point.y);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showTitle();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B) && hovered != null) {
            struckBare = hovered;
            bareElapsed = 0f;
        }
        if (struckBare != null) {
            bareElapsed += slowMotion ? delta / 8f : delta;
            if (Barehanded.finished(bareElapsed)) {
                struckBare = null;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K) && hovered != null) {
            killing = hovered;
            killElapsed = 0f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            slowMotion = !slowMotion;
        }
        if (killing != null) {
            killElapsed += slowMotion ? delta / 8f : delta;
            if (WeaponKill.finished(killElapsed)) {
                killing = null;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            showRim = !showRim;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            view = view == View.ROOM ? View.SHEET : View.ROOM;
        }
        elapsed += delta;
        ScreenUtils.clear(BACKDROP);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        theme.body.setColor(Theme.BONE);
        if (view == View.ROOM) {
            drawRoom();
        } else {
            drawSheet();
        }
        theme.body.draw(batch, "Tab: view   R: rim   K: kill   B: barehanded   S: slow-mo   Esc: leave", 40, 48);

        batch.end();
    }

    /** Four framed cards at the board geometry, each with its sprite in the well. */
    private void drawRoom() {
        List<Card> room = List.of(
                cardWithId("2C"),   // four creatures, so the idle stagger
                cardWithId("7C"),   // is visible: at any instant they
                cardWithId("10C"),  // should be on different frames
                cardWithId("QC"));
        hovered = hoveredIn(room);
        for (int i = 0; i < room.size(); i++) {
            Card card = room.get(i);
            int slotX = CardArt.slotX(i);
            boolean struck = card.equals(killing);
            if (struck && WeaponKill.cardCut(killElapsed)) {
                drawCleaved(slotX);
                continue;
            }
            boolean bare = card.equals(struckBare);
            int lift = struck ? WeaponKill.cardLift(killElapsed) : 0;
            int shakeX = bare ? Barehanded.shakeX(bareElapsed) : 0;
            int shakeY = bare ? Barehanded.shakeY(bareElapsed) : 0;
            slotX += shakeX;
            lift -= shakeY;
            cardFrame.draw(batch, card.type(), slotX, CardArt.SLOT_Y - lift);
            // A creature reads the same however it is being killed: it holds
            // its struck frame, and only what happens next differs. The hurt
            // frame already carries the outline, so this is one draw.
            boolean beingStruck = (bare && Barehanded.hurtShowing(bareElapsed))
                    || (struck && WeaponKill.rimShowing(killElapsed));
            TextureRegion body = beingStruck
                    ? sprites.hurt(CardSprites.regionName(card))
                    : current(card, card.equals(hovered));
            batch.draw(body, CardArt.spriteLeft(slotX),
                    CardArt.toWorldY(CardArt.spriteTop() - lift, CardArt.SPRITE),
                    CardArt.SPRITE, CardArt.SPRITE);
            if (bare && Barehanded.flashShowing(bareElapsed)) {
                // A short bone wash under the blow, over the well only.
                batch.setColor(0.91f, 0.87f, 0.78f, 0.35f);
                batch.draw(theme.whiteRegion(), CardArt.wellLeft(slotX),
                        CardArt.toWorldY(CardArt.wellTop() - lift, CardArt.WELL_H),
                        CardArt.wellWidth(), CardArt.WELL_H);
                batch.setColor(1f, 1f, 1f, 1f);
            }
            // R flashes the bare outline on its own, for inspecting it.
            if (showRim && !beingStruck) {
                TextureRegion rim = sprites.rim(CardSprites.regionName(card));
                if (rim != null) {
                    batch.draw(rim, CardArt.spriteLeft(slotX),
                            CardArt.toWorldY(CardArt.spriteTop() - lift, CardArt.SPRITE),
                            CardArt.SPRITE, CardArt.SPRITE);
                }
            }
            theme.body.draw(batch, card.id(), slotX + 4, CardArt.toWorldY(CardArt.SLOT_Y - 8, 0));
        }
        theme.body.draw(batch, "ROOM — hover to animate, K to cleave, B to strike"
                + (slowMotion ? "   [SLOW 1/8]" : ""), 40, 700);
    }

    /**
     * Every object the deck can deal, laid out by rank so a missing or doubled
     * sprite is obvious at a glance: clubs and spades creatures on the top two
     * rows, then the nine weapons and nine potions.
     */
    private void drawSheet() {
        int cell = 88;
        int left = (int) (Theme.WORLD_WIDTH - 13 * cell) / 2;
        int top = 132;
        String[] rowLabels = {"CLUBS", "SPADES", "WEAPON", "POTION"};

        for (int value = 2; value <= 14; value++) {
            String rank = switch (value) {
                case 11 -> "J";
                case 12 -> "Q";
                case 13 -> "K";
                case 14 -> "A";
                default -> String.valueOf(value);
            };
            theme.body.draw(batch, rank, left + (value - 2) * cell + 24,
                    CardArt.toWorldY(top - 12, 0));
        }

        for (int row = 0; row < 4; row++) {
            int y = top + row * 96;
            theme.body.draw(batch, rowLabels[row], 24, CardArt.toWorldY(y + 26, 0));
            for (Card card : deck) {
                if (rowOf(card) != row) {
                    continue;
                }
                int x = left + (card.value() - 2) * cell + 12;
                batch.draw(current(card, false),
                        x, CardArt.toWorldY(y, Sprites.SIZE), Sprites.SIZE, Sprites.SIZE);
            }
        }
        theme.body.draw(batch, "SHEET — all 44 cards, 31 objects, by rank", 40, 700);
    }

    /**
     * The card after the blade lands. Nothing here is drawn before
     * {@code cardCut} is true — the halves do not exist during the flash
     * rather than existing transparently, which is what keeps the
     * creature visible through it.
     */
    private void drawCleaved(int slotX) {
        int top = CardArt.SLOT_Y;
        if (WeaponKill.halvesShowing(killElapsed)) {
            float alpha = WeaponKill.halfAlpha(killElapsed);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(sliceArt.upper(), slotX + WeaponKill.upperDx(killElapsed),
                    CardArt.toWorldY(top + WeaponKill.upperDy(killElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.draw(sliceArt.lower(), slotX + WeaponKill.lowerDx(killElapsed),
                    CardArt.toWorldY(top + WeaponKill.lowerDy(killElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            // Between the lift and the halves parting the card is whole but
            // already raised, so the blow reads before the cut does.
            int lift = WeaponKill.cardLift(killElapsed);
            batch.draw(sliceArt.upper(), slotX,
                    CardArt.toWorldY(top - lift, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.draw(sliceArt.lower(), slotX,
                    CardArt.toWorldY(top - lift, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
        }
        if (WeaponKill.slashShowing(killElapsed)) {
            batch.draw(sliceArt.bar(), slotX + WeaponKill.slashOffset(killElapsed),
                    CardArt.toWorldY(top - WeaponKill.slashOffset(killElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
        }
    }

    /** Clubs 0, spades 1, weapons 2, potions 3. */
    private int rowOf(Card card) {
        if (card.type() == CardType.MONSTER) {
            return card.id().endsWith("C") ? 0 : 1;
        }
        return card.type() == CardType.WEAPON ? 2 : 3;
    }

    private Card cardWithId(String id) {
        return deck.stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("no card " + id));
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        sliceArt.dispose();
        batch.dispose();
    }
}
