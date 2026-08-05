package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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

    /** 14 of 20 healing up to 19, so a drink has several segments to grow through. */
    private static final int HEAL_FROM = 148;
    private static final int HEAL_TO = 201;
    /** And 14 down to 6, so a hit has several segments to drain through. */
    private static final int HIT_FROM = 148;
    private static final int HIT_TO = 64;

    /** ROOM shows four framed cards; SHEET shows every object by rank. */
    private enum View { ROOM, SHEET }

    private final ScoundrelGame game;
    private final Theme theme;
    private final Sprites sprites;
    private final CardFrame cardFrame;
    private final EffectArt effectArt;
    private final BoardHud hud;
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
    /** A room being swept to the ticker, or a card carried to the rail. */
    private float avoidElapsed = -1f;
    private Card equipping;
    private float equipElapsed;
    /** A hit or a drink landing on the bar. */
    private float damageElapsed = -1f;
    private float healElapsed = -1f;
    /** A potion being drunk, and the bottle's tilt stages built for it. */
    private Card drinking;
    private float drinkElapsed;
    private Texture[] tilts;
    private float elapsed;
    /** The card under the pointer, or null. Only it animates. */
    private Card hovered;

    public SpriteLab(ScoundrelGame game, Theme theme, Sprites sprites) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        this.cardFrame = new CardFrame(theme);
        this.effectArt = new EffectArt(CardArt.CARD_W, CardArt.CARD_H);
        this.hud = new BoardHud(theme);
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

    /**
     * Builds the bottle's lean stages for this potion. Done here rather than at
     * load because only one potion is ever being drunk, so nine sprites' worth
     * of tilts would sit unused.
     */
    private void startDrink(Card card) {
        endDrink();
        drinking = card;
        drinkElapsed = 0f;
        // The bar waits for the pour. Starting it here would fill it while the
        // bottle was still in the air, which is the whole thing PotionDrink's
        // phase order exists to prevent.
        healElapsed = -1f;
        // The card collapses into a drawn bottle, not into its own sprite --
        // a 64px illustration is unreadable at the size this ends up.
        int n = EffectArt.BOTTLE_SIZE;
        int[] base = effectArt.bottlePixels();
        tilts = new Texture[TiltMask.STAGES + 1];
        for (int stage = 0; stage <= TiltMask.STAGES; stage++) {
            tilts[stage] = Sprites.textureFrom(
                    TiltMask.tilt(base, n, n, stage), n, n);
        }
    }

    private void endDrink() {
        drinking = null;
        if (tilts != null) {
            for (Texture t : tilts) {
                t.dispose();
            }
            tilts = null;
        }
    }

    /**
     * The bottle on its way to the bar and pouring into it. Nothing is drawn at
     * the bar until it has arrived and tipped, so the fill always has a visible
     * cause.
     */
    private void drawDrink(int slotX) {
        int size = EffectArt.BOTTLE_SIZE;
        int fromX = CardArt.spriteLeft(slotX) + (CardArt.SPRITE - size) / 2;
        int fromY = CardArt.spriteTop() + (CardArt.SPRITE - size) / 2;
        float progress = PotionDrink.flightProgress(drinkElapsed);
        int x = Math.round(fromX + (HudArt.BAR_X + 40 - fromX) * progress);
        int y = Math.round(fromY + (HudArt.BAR_Y - 24 - fromY) * progress);
        int drawn = size;   // already the right size; it does not shrink

        batch.draw(new TextureRegion(tilts[PotionDrink.tiltStage(drinkElapsed)]),
                x, CardArt.toWorldY(y, drawn), drawn, drawn);

        // Drops fall from the lip once it is pouring.
        for (int drop = 0; drop < PotionDrink.dropsFallen(drinkElapsed); drop++) {
            int dy = y + drawn - 4 + drop * 6;
            batch.setColor(0.44f, 0.71f, 0.36f, 1f);
            batch.draw(theme.whiteRegion(), x + drawn / 2, CardArt.toWorldY(dy, 4), 4, 4);
            batch.setColor(1f, 1f, 1f, 1f);
        }
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && hovered != null
                && hovered.type() == CardType.POTION) {
            startDrink(hovered);
        }
        if (drinking != null) {
            drinkElapsed += slowMotion ? delta / 8f : delta;
            if (PotionDrink.pouring(drinkElapsed) && healElapsed < 0f) {
                healElapsed = 0f;
            }
            if (PotionDrink.finished(drinkElapsed)) {
                endDrink();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            damageElapsed = 0f;
        }
        if (damageElapsed >= 0f) {
            damageElapsed += slowMotion ? delta / 8f : delta;
            if (HpPulse.damageFinished(HIT_FROM, HIT_TO, damageElapsed)) {
                damageElapsed = -1f;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            healElapsed = 0f;
        }
        if (healElapsed >= 0f) {
            healElapsed += slowMotion ? delta / 8f : delta;
            if (HpPulse.healFinished(HEAL_FROM, HEAL_TO, healElapsed)) {
                healElapsed = -1f;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            avoidElapsed = 0f;
        }
        if (avoidElapsed >= 0f) {
            avoidElapsed += slowMotion ? delta / 8f : delta;
            if (avoidElapsed >= CardFlight.AVOID.totalFor(4)) {
                avoidElapsed = -1f;
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && hovered != null) {
            equipping = hovered;
            equipElapsed = 0f;
        }
        if (equipping != null) {
            equipElapsed += slowMotion ? delta / 8f : delta;
            if (CardFlight.EQUIP.finished(equipElapsed)) {
                equipping = null;
            }
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
        theme.body.draw(batch, "R rim  K kill  B bare  A avoid  E equip  P potion  D hit  H heal  S slow", 40, 48);

        batch.end();
    }

    /** Four framed cards at the board geometry, each with its sprite in the well. */
    private void drawRoom() {
        List<Card> room = List.of(
                cardWithId("10C"),  // the reference board's room, so a
                cardWithId("7D"),   // side-by-side against it means
                cardWithId("QS"),   // something -- and so equip and drink
                cardWithId("5H"));  // have something to act on
        hovered = hoveredIn(room);
        // The reference board: 14 of 20 health, depth 27 of the 44-card deck.
        boolean healing = healElapsed >= 0f;
        boolean hit = damageElapsed >= 0f;
        int fill = HudArt.barFillWidth(14, 20);
        if (healing) {
            fill = HpPulse.healWidth(HEAL_FROM, HEAL_TO, healElapsed);
        } else if (hit) {
            fill = HpPulse.damageWidth(HIT_FROM, HIT_TO, damageElapsed);
        }
        hud.drawHealth(batch,
                healing ? 19 : hit ? 6 : 14, 20,
                healing && HpPulse.numberHealed(HEAL_FROM, HEAL_TO, healElapsed),
                hit && HpPulse.numberBloodied(HIT_FROM, HIT_TO, damageElapsed),
                hit ? HpPulse.barOffset(HIT_FROM, HIT_TO, damageElapsed) : 0, fill);
        hud.drawTicker(batch, 27, 44);
        hud.drawAvoid(batch, true);
        for (int i = 0; i < room.size(); i++) {
            Card card = room.get(i);
            int slotX = CardArt.slotX(i);
            if (card.equals(drinking)) {
                int scale = PotionDrink.cardScale(drinkElapsed);
                if (scale > 0) {
                    int w = CardArt.CARD_W * scale / 100;
                    int h = CardArt.CARD_H * scale / 100;
                    cardFrame.draw(batch, card.type(),
                            slotX + (CardArt.CARD_W - w) / 2,
                            CardArt.SLOT_Y + (CardArt.CARD_H - h) / 2, w, h);
                }
                continue;
            }
            boolean sweeping = avoidElapsed >= 0f
                    && CardFlight.started(CardFlight.AVOID, i, avoidElapsed);
            if (sweeping || card.equals(equipping)) {
                drawFlight(card, slotX, i);
                continue;
            }
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
            // R flashes the bare outline on its own, for inspecting it.
            if (showRim && !beingStruck) {
                TextureRegion rim = sprites.rim(CardSprites.regionName(card));
                if (rim != null) {
                    batch.draw(rim, CardArt.spriteLeft(slotX),
                            CardArt.toWorldY(CardArt.spriteTop() - lift, CardArt.SPRITE),
                            CardArt.SPRITE, CardArt.SPRITE);
                }
            }
            if (bare) {
                drawStars(slotX, lift);
            }
            theme.body.draw(batch, card.id(), slotX + 4, CardArt.toWorldY(CardArt.SLOT_Y - 8, 0));
        }
        // One wash, over the whole board rather than under it, so the blow
        // lands on everything at once instead of lighting the gaps.
        if (struckBare != null) {
            float wash = Barehanded.flashAlpha(bareElapsed);
            if (wash > 0f) {
                batch.setColor(0.95f, 0.81f, 0.48f, wash);
                batch.draw(theme.whiteRegion(), 0, 0,
                        Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
        if (drinking != null) {
            drawDrink(CardArt.slotX(3));
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
     * A card on its way out of the room — swept to the ticker, or carried down
     * to the rail. The whole card shrinks and hops; nothing tweens between one
     * hop and the next.
     */
    private void drawFlight(Card card, int slotX, int index) {
        boolean sweeping = avoidElapsed >= 0f && !card.equals(equipping);
        CardFlight.Flight flight = sweeping ? CardFlight.AVOID : CardFlight.EQUIP;
        float t = sweeping
                ? CardFlight.localTime(flight, index, avoidElapsed)
                : equipElapsed;

        int scale = CardFlight.scale(flight, t);
        int w = CardArt.CARD_W * scale / 100;
        int h = CardArt.CARD_H * scale / 100;
        // The anchors are centres, so the card is placed by its own centre.
        int cx = CardFlight.x(flight, slotX + CardArt.CARD_W / 2, t);
        int cy = CardFlight.y(flight, CardArt.SLOT_Y + CardArt.CARD_H / 2, t);
        int x = cx - w / 2;
        int y = cy - h / 2;

        cardFrame.draw(batch, card.type(), x, y, w, h);
        int sprite = CardArt.SPRITE * scale / 100;
        batch.draw(current(card, false),
                x + (w - sprite) / 2,
                CardArt.toWorldY(y + Math.round((CardArt.spriteTop() - CardArt.SLOT_Y)
                        * scale / 100f), sprite),
                sprite, sprite);
    }

    /**
     * The two bursts a bare-handed blow throws, each growing through three
     * discrete sizes as it fades. Drawn over the card, centred on their own
     * offsets from its centre.
     */
    private void drawStars(int slotX, int lift) {
        for (int hit = 0; hit < Barehanded.hits(); hit++) {
            int size = Barehanded.starSize(hit, bareElapsed);
            if (size == 0) {
                continue;
            }
            int cx = slotX + CardArt.CARD_W / 2 + Barehanded.starOffsetX(hit);
            int cy = CardArt.SLOT_Y + CardArt.CARD_H / 2 + Barehanded.starOffsetY(hit) - lift;
            batch.setColor(1f, 1f, 1f, Barehanded.starAlpha(hit, bareElapsed));
            batch.draw(effectArt.star(), cx - size / 2,
                    CardArt.toWorldY(cy - size / 2, size), size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
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
            batch.draw(effectArt.upper(), slotX + WeaponKill.upperDx(killElapsed),
                    CardArt.toWorldY(top + WeaponKill.upperDy(killElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.draw(effectArt.lower(), slotX + WeaponKill.lowerDx(killElapsed),
                    CardArt.toWorldY(top + WeaponKill.lowerDy(killElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            // Between the lift and the halves parting the card is whole but
            // already raised, so the blow reads before the cut does.
            int lift = WeaponKill.cardLift(killElapsed);
            batch.draw(effectArt.upper(), slotX,
                    CardArt.toWorldY(top - lift, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.draw(effectArt.lower(), slotX,
                    CardArt.toWorldY(top - lift, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
        }
        if (WeaponKill.slashShowing(killElapsed)) {
            batch.draw(effectArt.bar(), slotX + WeaponKill.slashOffset(killElapsed),
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
        endDrink();
        effectArt.dispose();
        batch.dispose();
    }
}
