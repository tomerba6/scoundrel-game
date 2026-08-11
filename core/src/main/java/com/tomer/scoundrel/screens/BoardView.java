package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The room of face-up cards: the frames, the sprites in their wells, the
 * printing on them, and everything that happens to one when it is resolved.
 * One class so the developer lab and the real board draw the same thing — the
 * lab is only a verification instrument if what it shows is literally what the
 * game shows.
 *
 * <p>It owns the idle clock, each card's stagger, and the effect currently
 * running. It owns no game state and no rules: callers say which cards are in
 * the room, what just happened to one of them, and where the pointer is.
 *
 * <p>Every effect's timing lives in its own pure class ({@link CardFlight},
 * {@link WeaponKill}, {@link Barehanded}, {@link PotionDrink}); what is here is
 * the sequencing and the draw calls.
 */
final class BoardView {

    /** What is happening to the room right now. */
    private enum Kind { NONE, SWEEP, EQUIP, POTION, SPILL, STRIKE, SLICE }

    /**
     * A deal's clock, with the anchors left at the origin. Every card of a deal
     * aims somewhere different but they all run to the same timing, so anything
     * that only asks <em>when</em> can share one flight.
     */
    private static final CardFlight.Flight DEAL_CLOCK = CardFlight.dealTo(0, 0);
    /** And a slide's, which is the same length but never staggers. */
    private static final CardFlight.Flight SLIDE_CLOCK = CardFlight.slideTo(0, 0);

    private final Theme theme;
    private final Sprites sprites;
    private final CardFrame cardFrame;
    private final CardFace cardFace;
    private final Pips pips;
    private final EffectArt effectArt;
    private final Random random = new Random();

    /** Per-card start offsets, assigned when a card is dealt — never per frame. */
    private final Map<String, Float> idleOffsets = new HashMap<>();
    private List<Card> room = List.of();
    private float elapsed;
    /** The card under the pointer, or null. Only it animates. */
    private Card hovered;

    // --- what is playing ---------------------------------------------------

    private Kind kind = Kind.NONE;
    private float effectElapsed;
    /**
     * How fast the effect's own clock runs. Always 1 except for the blow that
     * kills you, which plays at half speed — the last thing to happen in a run
     * should not go by at the same rate as the forty before it.
     */
    private float effectRate = 1f;
    /** The dungeon sending cards up, which waits for the effect to finish. */
    private boolean dealing;
    private float dealElapsed;
    /** The survivors re-centring, which does not — it runs alongside it. */
    private boolean closing;
    private float closeElapsed;
    /** The card the effect acts on: resolved, so no longer in the room. */
    private Card subject;
    /** The room that was swept away, still to be drawn on its way out. */
    private List<Card> outgoing = List.of();
    /** Where each card sat before the move — the point every flight starts from. */
    private final Map<String, Integer> previousX = new HashMap<>();
    /** And where the swept room sat, which is a separate question once it has left. */
    private final Map<String, Integer> outgoingX = new HashMap<>();
    /** Told when the potion actually pours, so the bar fills with a cause. */
    private Runnable onPour;
    private boolean poured;

    BoardView(Theme theme, Sprites sprites) {
        this.theme = theme;
        this.sprites = sprites;
        this.cardFrame = new CardFrame(theme);
        this.pips = new Pips();
        this.cardFace = new CardFace(theme, pips);
        this.effectArt = new EffectArt(CardArt.CARD_W, CardArt.CARD_H);
    }

    /**
     * The cards on the board now. A card keeps the stagger it was dealt with
     * for as long as it is in the room, so carrying over to the next room does
     * not restart its cycle.
     */
    void setRoom(List<Card> room) {
        this.room = List.copyOf(room);
        for (Card card : this.room) {
            idleOffsets.computeIfAbsent(card.id(), id -> IdleCycle.randomOffset(random));
        }
    }

    List<Card> room() {
        return room;
    }

    /**
     * Remembers where everything sits, before the move that is about to change
     * it. Every flight afterwards starts from these positions; a card with no
     * entry is one the dungeon has not dealt yet, and comes from the ticker.
     */
    void beginMove() {
        previousX.clear();
        for (int i = 0; i < room.size(); i++) {
            previousX.put(room.get(i).id(), slotX(i));
        }
    }

    /** Where a card sat before the current move, or null if it was not out. */
    Integer previousSlotX(String cardId) {
        return previousX.get(cardId);
    }

    void update(float delta) {
        elapsed += delta;
        // The room closes on its own clock, alongside whatever is happening to
        // the card that left rather than after it.
        if (closing) {
            closeElapsed += delta;
            if (closeElapsed >= SLIDE_CLOCK.total()) {
                closing = false;
            }
        }
        if (kind != Kind.NONE) {
            effectElapsed += delta * effectRate;
            if (kind == Kind.POTION && !poured && PotionDrink.pouring(effectElapsed)) {
                poured = true;
                if (onPour != null) {
                    onPour.run();
                }
            }
            if (effectElapsed >= effectLength()) {
                kind = Kind.NONE;
                subject = null;
                outgoing = List.of();
            }
        } else if (dealing) {
            dealElapsed += delta;
            if (dealElapsed >= dealLength()) {
                dealing = false;
            }
        }
    }

    private float effectLength() {
        return switch (kind) {
            case SWEEP -> CardFlight.AVOID.totalFor(Math.max(1, outgoing.size()));
            case EQUIP -> CardFlight.EQUIP.total();
            case POTION -> PotionDrink.TOTAL;
            case SPILL -> PotionSpill.TOTAL;
            case STRIKE -> Barehanded.TOTAL;
            case SLICE -> WeaponKill.TOTAL;
            case NONE -> 0f;
        };
    }

    private float dealLength() {
        return DEAL_CLOCK.totalFor(room.size());
    }

    /**
     * How many cards are still on their way up out of the dungeon. The engine
     * gave them up the moment the move was applied, but on screen they are
     * between the ticks and the table — so the depth ticker still counts them,
     * and a tick goes out as its card lands rather than all four at once before
     * anything has moved.
     *
     * <p>A card that was already on the board is sliding, not rising: it left
     * the dungeon rooms ago and must not be counted again.
     */
    int rising() {
        if (!dealing) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < room.size(); i++) {
            if (previousX.containsKey(room.get(i).id())) {
                continue;
            }
            if (!CardFlight.landed(DEAL_CLOCK, i, dealElapsed)) {
                count++;
            }
        }
        return count;
    }

    /**
     * And how many of an avoided room have not reached the dungeon yet. They
     * have left the table but not arrived, so the ticker does not count them
     * either — which is what makes the strip grow as the room goes in and shrink
     * again as the next one comes out.
     */
    int sweeping() {
        int count = 0;
        for (int i = 0; i < outgoing.size(); i++) {
            if (!CardFlight.landed(CardFlight.AVOID, i, effectElapsed)) {
                count++;
            }
        }
        return count;
    }

    boolean isPlaying() {
        return kind != Kind.NONE || dealing || closing;
    }

    /**
     * Whether the rail is still running ahead of the board. The engine settles
     * the whole move the instant the card is pressed, so both of the rail's
     * halves arrive early: a weapon sits in its well while its own card is
     * still hopping down towards it, and a slain monster is stacked on it —
     * chip, and a dulled threshold plate — while the creature is still being
     * cleaved. Until what you can see agrees, the rail reports what was there.
     */
    boolean railAhead() {
        return kind == Kind.EQUIP || kind == Kind.SLICE;
    }

    /** Ends whatever is playing at once. The state underneath is already final. */
    void skip() {
        if (kind == Kind.POTION && !poured && onPour != null) {
            poured = true;
            onPour.run(); // the heal must still land, even skipped
        }
        kind = Kind.NONE;
        subject = null;
        outgoing = List.of();
        dealing = false;
        closing = false;
    }

    // --- starting an effect ------------------------------------------------

    /**
     * A fresh run: nothing was on the board, so the whole opening room comes up
     * out of the dungeon rather than appearing already dealt.
     */
    void dealFresh(List<Card> room) {
        previousX.clear();
        outgoingX.clear();
        kind = Kind.NONE;
        subject = null;
        outgoing = List.of();
        closing = false;
        setRoom(room);
        playDeal();
    }

    /**
     * The new room flies in; anything already out slides to its new slot. The
     * two run on separate clocks: the slide starts now, alongside the effect,
     * and the deal waits until {@link #update} says the effect is over.
     */
    void playDeal() {
        closing = !previousX.isEmpty();
        closeElapsed = 0f;
        dealing = hasUndealtCards();
        dealElapsed = 0f;
    }

    /** Whether the dungeon actually owes the room anything, or it merely shrank. */
    private boolean hasUndealtCards() {
        for (Card card : room) {
            if (!previousX.containsKey(card.id())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The whole room sweeps into the dungeon, then the next one deals in. An
     * avoided room goes to the bottom of the deck, so every card of the next
     * one comes back up out of the dungeon — including, at the shallow end, a
     * card that was just swept away. Handing the outgoing positions to their
     * own map is what says that: nothing carries over.
     */
    void playSweep(List<Card> avoided) {
        start(Kind.SWEEP, null);
        outgoing = List.copyOf(avoided);
        outgoingX.clear();
        outgoingX.putAll(previousX);
        previousX.clear();
        playDeal();
    }

    void playEquip(Card weapon) {
        start(Kind.EQUIP, weapon);
        playDeal();
    }

    /** The potion collapses, flies to the bar, and pours — then the room refills. */
    void playPotion(Card potion, Runnable onPour) {
        start(Kind.POTION, potion);
        this.onPour = onPour;
        this.poured = false;
        playDeal();
    }

    /**
     * A wasted potion: the same card, the same bottle, but drained and tipped
     * out where it stood. It never reaches the bar, because nothing reaches
     * you — that is the whole message of the effect.
     */
    void playSpill(Card potion) {
        start(Kind.SPILL, potion);
        playDeal();
    }

    void playStrike(Card monster) {
        playStrike(monster, false);
    }

    void playSlice(Card monster) {
        playSlice(monster, false);
    }

    /**
     * A blow, and whether it is the one that kills you. A fatal blow runs at
     * half speed and the dungeon sends nothing up after it — the room closes
     * over the gap, but you are not being dealt another card, because you are
     * not playing on.
     */
    void playStrike(Card monster, boolean fatal) {
        start(Kind.STRIKE, monster, fatal);
    }

    void playSlice(Card monster, boolean fatal) {
        start(Kind.SLICE, monster, fatal);
    }

    private void start(Kind kind, Card subject) {
        start(kind, subject, 1f);
        playDeal();
    }

    private void start(Kind kind, Card subject, boolean fatal) {
        start(kind, subject, fatal ? 0.5f : 1f);
        if (fatal) {
            closeOnly();
        } else {
            playDeal();
        }
    }

    private void start(Kind kind, Card subject, float rate) {
        this.kind = kind;
        this.subject = subject;
        this.effectElapsed = 0f;
        this.effectRate = rate;
        this.onPour = null;
    }

    /** The room closes, but nothing comes up to replace what left. */
    private void closeOnly() {
        closing = !previousX.isEmpty();
        closeElapsed = 0f;
        dealing = false;
        dealElapsed = 0f;
    }

    // --- input -------------------------------------------------------------

    /** The card under a point in world coordinates, or null. */
    Card cardAt(float worldX, float worldY) {
        List<CardHitRegions.CardRect> rects = new ArrayList<>();
        for (int i = 0; i < room.size(); i++) {
            rects.add(new CardHitRegions.CardRect(room.get(i), slotX(i),
                    CardArt.toWorldY(CardArt.SLOT_Y, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H));
        }
        return CardHitRegions.cardAt(rects, worldX, worldY);
    }

    void setHovered(Card card) {
        this.hovered = card;
    }

    Card hovered() {
        return hovered;
    }

    /**
     * Where the i-th card of the current room sits. A short room is centred on
     * the same middle a full one is, so the last card of a room does not sit
     * off to one side while the next deals in around it.
     */
    int slotX(int index) {
        return slotX(index, room.size());
    }

    static int slotX(int index, int cards) {
        int span = cards * CardArt.CARD_W + Math.max(0, cards - 1) * gap();
        int left = Math.round((Theme.WORLD_WIDTH - span) / 2f);
        return left + index * (CardArt.CARD_W + gap());
    }

    private static int gap() {
        return CardArt.slotX(1) - (CardArt.slotX(0) + CardArt.CARD_W);
    }

    // --- drawing -----------------------------------------------------------

    void draw(Batch batch) {
        for (Card card : outgoing) {
            drawSweeping(batch, card);
        }
        for (int i = 0; i < room.size(); i++) {
            drawRoomCard(batch, room.get(i), i);
        }
        if (subject != null) {
            drawSubject(batch);
        }
        // The bare-handed flash goes over the whole board rather than under it,
        // so the blow lands on everything at once instead of lighting the gaps.
        if (kind == Kind.STRIKE) {
            float wash = Barehanded.flashAlpha(effectElapsed);
            if (wash > 0f) {
                batch.setColor(0.95f, 0.81f, 0.48f, wash);
                batch.draw(theme.whiteRegion(), 0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
                batch.setColor(1f, 1f, 1f, 1f);
            }
        }
    }

    /**
     * A card of the current room. While an effect plays it waits where it was;
     * one the dungeon has not dealt yet is not drawn at all, rather than
     * appearing before its flight.
     */
    private void drawRoomCard(Batch batch, Card card, int index) {
        Integer from = previousX.get(card.id());
        // Flights are specified between centres, so the slot's left edge is not
        // the anchor — landing a card on it puts it half a card too far left.
        int toX = slotX(index) + CardArt.CARD_W / 2;
        int toY = CardArt.SLOT_Y + CardArt.CARD_H / 2;
        switch (RoomMotion.of(from != null, kind != Kind.NONE, closing, dealing)) {
            case RESTING -> drawCard(batch, card, slotX(index), CardArt.SLOT_Y);
            case HIDDEN -> { }
            case SLIDING -> drawFlying(batch, card, CardFlight.slideTo(toX, toY),
                    from, CardArt.SLOT_Y, closeElapsed);
            case DEALING -> {
                CardFlight.Flight deal = CardFlight.dealTo(toX, toY);
                float t = CardFlight.localTime(deal, index, dealElapsed);
                if (t >= 0f) { // otherwise it is still waiting its turn
                    drawFlying(batch, card, deal, CardFlight.TICKER_X - CardArt.CARD_W / 2,
                            CardFlight.TICKER_Y - CardArt.CARD_H / 2, t);
                }
            }
        }
    }

    /** A card of the avoided room, hopping up into the dungeon. */
    private void drawSweeping(Batch batch, Card card) {
        Integer from = outgoingX.get(card.id());
        if (from == null) {
            return;
        }
        int index = outgoing.indexOf(card);
        float t = CardFlight.localTime(CardFlight.AVOID, index, effectElapsed);
        if (t < 0f) {
            drawCard(batch, card, from, CardArt.SLOT_Y);
            return;
        }
        drawFlying(batch, card, CardFlight.AVOID, from, CardArt.SLOT_Y, t);
    }

    /** Whatever is happening to the card that was just resolved. */
    private void drawSubject(Batch batch) {
        Integer from = previousX.get(subject.id());
        int slot = from != null ? from : slotX(0);
        switch (kind) {
            case EQUIP -> drawFlying(batch, subject, CardFlight.EQUIP,
                    slot, CardArt.SLOT_Y, effectElapsed);
            case POTION -> drawDrink(batch, slot);
            case SPILL -> drawSpill(batch, slot);
            case STRIKE -> drawStruck(batch, slot, true);
            case SLICE -> drawStruck(batch, slot, false);
            default -> { }
        }
    }

    /**
     * One card, whole, at a design-space position — the ordinary resting state.
     *
     * <p>The frame and the printing are drawn where they belong and the sprite
     * alone takes the {@link SpriteBob} offset, so the art breathes inside its
     * window rather than the card wobbling. A card mid-flight or mid-effect does
     * not come through here: those own their motion, and a bob underneath would
     * fight it.
     */
    void drawCard(Batch batch, Card card, int slotX, int slotY) {
        cardFrame.draw(batch, card.type(), slotX, slotY);
        int spriteTop = slotY + (CardArt.spriteTop() - CardArt.SLOT_Y) + SpriteBob.offsetAt(elapsed);
        batch.draw(spriteFor(card, card.equals(hovered)), CardArt.spriteLeft(slotX),
                CardArt.toWorldY(spriteTop, CardArt.SPRITE),
                CardArt.SPRITE, CardArt.SPRITE);
        cardFace.draw(batch, card, slotX, slotY);
    }

    /**
     * A card mid-flight: the whole card shrinks and hops, and nothing tweens
     * between one hop and the next. The face is left off — at 28% the printing
     * is unreadable, and drawing it costs three text layouts a frame per card.
     */
    private void drawFlying(Batch batch, Card card, CardFlight.Flight flight,
                            int fromX, int fromY, float t) {
        int scale = CardFlight.scale(flight, t);
        int w = CardArt.CARD_W * scale / 100;
        int h = CardArt.CARD_H * scale / 100;
        // The anchors are centres, so the card is placed by its own centre.
        int cx = CardFlight.x(flight, fromX + CardArt.CARD_W / 2, t);
        int cy = CardFlight.y(flight, fromY + CardArt.CARD_H / 2, t);
        int x = cx - w / 2;
        int y = cy - h / 2;

        cardFrame.draw(batch, card.type(), x, y, w, h);
        int sprite = CardArt.SPRITE * scale / 100;
        batch.draw(spriteFor(card, false), x + (w - sprite) / 2,
                CardArt.toWorldY(y + Math.round((CardArt.spriteTop() - CardArt.SLOT_Y)
                        * scale / 100f), sprite),
                sprite, sprite);
        if (scale == 100) {
            cardFace.draw(batch, card, x, y);
        }
    }

    /**
     * The card taking a blow. A creature reads the same however it is being
     * killed — it holds its struck frame — and only what happens next differs:
     * a bare-handed exchange throws stars, a weapon cleaves the card in two.
     */
    private void drawStruck(Batch batch, int slotX, boolean barehanded) {
        if (!barehanded && WeaponKill.cardCut(effectElapsed)) {
            drawCleaved(batch, slotX);
            return;
        }
        int lift = barehanded ? 0 : WeaponKill.cardLift(effectElapsed);
        int shakeX = barehanded ? Barehanded.shakeX(effectElapsed) : 0;
        int shakeY = barehanded ? Barehanded.shakeY(effectElapsed) : 0;
        int x = slotX + shakeX;
        int top = CardArt.SLOT_Y - lift + shakeY;

        cardFrame.draw(batch, subject.type(), x, top);
        boolean hurting = barehanded
                ? Barehanded.hurtShowing(effectElapsed)
                : WeaponKill.rimShowing(effectElapsed);
        TextureRegion body = hurting && subject.type() == CardType.MONSTER
                ? sprites.hurt(CardSprites.regionName(subject))
                : spriteFor(subject, false);
        batch.draw(body, CardArt.spriteLeft(x),
                CardArt.toWorldY(top + (CardArt.spriteTop() - CardArt.SLOT_Y), CardArt.SPRITE),
                CardArt.SPRITE, CardArt.SPRITE);
        cardFace.draw(batch, subject, x, top);
        if (barehanded) {
            drawStars(batch, x, CardArt.SLOT_Y - top);
        }
    }

    /**
     * The two bursts a bare-handed blow throws, each growing through three
     * discrete sizes as it fades.
     */
    private void drawStars(Batch batch, int slotX, int lift) {
        for (int hit = 0; hit < Barehanded.hits(); hit++) {
            int size = Barehanded.starSize(hit, effectElapsed);
            if (size == 0) {
                continue;
            }
            int cx = slotX + CardArt.CARD_W / 2 + Barehanded.starOffsetX(hit);
            int cy = CardArt.SLOT_Y + CardArt.CARD_H / 2 + Barehanded.starOffsetY(hit) - lift;
            batch.setColor(1f, 1f, 1f, Barehanded.starAlpha(hit, effectElapsed));
            batch.draw(effectArt.star(), cx - size / 2,
                    CardArt.toWorldY(cy - size / 2, size), size, size);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * The card after the blade lands. Nothing here is drawn before
     * {@code cardCut} is true — the halves do not exist during the flash rather
     * than existing transparently, which is what keeps the creature visible
     * through it.
     */
    private void drawCleaved(Batch batch, int slotX) {
        int top = CardArt.SLOT_Y;
        if (WeaponKill.halvesShowing(effectElapsed)) {
            batch.setColor(1f, 1f, 1f, WeaponKill.halfAlpha(effectElapsed));
            batch.draw(effectArt.upper(), slotX + WeaponKill.upperDx(effectElapsed),
                    CardArt.toWorldY(top + WeaponKill.upperDy(effectElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.draw(effectArt.lower(), slotX + WeaponKill.lowerDx(effectElapsed),
                    CardArt.toWorldY(top + WeaponKill.lowerDy(effectElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.setColor(1f, 1f, 1f, 1f);
        } else {
            // Between the lift and the halves parting the card is whole but
            // already raised, so the blow reads before the cut does.
            int lift = WeaponKill.cardLift(effectElapsed);
            batch.draw(effectArt.upper(), slotX,
                    CardArt.toWorldY(top - lift, CardArt.CARD_H), CardArt.CARD_W, CardArt.CARD_H);
            batch.draw(effectArt.lower(), slotX,
                    CardArt.toWorldY(top - lift, CardArt.CARD_H), CardArt.CARD_W, CardArt.CARD_H);
        }
        if (WeaponKill.slashShowing(effectElapsed)) {
            batch.draw(effectArt.bar(), slotX + WeaponKill.slashOffset(effectElapsed),
                    CardArt.toWorldY(top - WeaponKill.slashOffset(effectElapsed), CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
        }
    }

    /**
     * The potion: its card collapses into a bottle that flies to the health bar
     * and tips over it. Nothing is drawn at the bar until it has arrived, so
     * the fill always has a visible cause.
     */
    private void drawDrink(Batch batch, int slotX) {
        int scale = PotionDrink.cardScale(effectElapsed);
        if (scale > 0) {
            int w = CardArt.CARD_W * scale / 100;
            int h = CardArt.CARD_H * scale / 100;
            cardFrame.draw(batch, subject.type(), slotX + (CardArt.CARD_W - w) / 2,
                    CardArt.SLOT_Y + (CardArt.CARD_H - h) / 2, w, h);
            return;
        }
        int size = EffectArt.BOTTLE_SIZE;
        int fromX = CardArt.spriteLeft(slotX) + (CardArt.SPRITE - size) / 2;
        int fromY = CardArt.spriteTop() + (CardArt.SPRITE - size) / 2;
        float progress = PotionDrink.flightProgress(effectElapsed);
        int x = Math.round(fromX + (HudArt.BAR_X + 40 - fromX) * progress);
        int y = Math.round(fromY + (HudArt.BAR_Y - 24 - fromY) * progress);

        // Rotated about its own centre. Safe because the texture is nearest
        // filtered: each pixel point-samples one texel, so the turn cannot
        // blend two ramp steps into a colour that is not in the palette.
        batch.draw(effectArt.bottle(), x, CardArt.toWorldY(y, size),
                size / 2f, size / 2f, size, size, 1f, 1f,
                PotionDrink.tiltDegrees(effectElapsed));

        for (int drop = 0; drop < PotionDrink.dropsFallen(effectElapsed); drop++) {
            int dy = y + size - 4 + drop * 6;
            // From the palette constant, not hand-typed floats — eyeballing the
            // components put the drops one step off the ramp in every channel.
            batch.setColor(((HudArt.FILL_HEAL >>> 16) & 0xff) / 255f,
                    ((HudArt.FILL_HEAL >>> 8) & 0xff) / 255f,
                    (HudArt.FILL_HEAL & 0xff) / 255f, 1f);
            batch.draw(theme.whiteRegion(), x + size / 2, CardArt.toWorldY(dy, 4), 4, 4);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * A potion that heals nothing: the card folds into the same bottle, drained
     * to bone, and it tips over and dribbles where it stood. Nothing travels —
     * the bar is never involved, because nothing ever reaches it.
     */
    private void drawSpill(Batch batch, int slotX) {
        int scale = PotionSpill.cardScale(effectElapsed);
        if (scale > 0) {
            int w = CardArt.CARD_W * scale / 100;
            int h = CardArt.CARD_H * scale / 100;
            cardFrame.draw(batch, subject.type(), slotX + (CardArt.CARD_W - w) / 2,
                    CardArt.SLOT_Y + (CardArt.CARD_H - h) / 2, w, h);
            return;
        }
        int size = EffectArt.BOTTLE_SIZE;
        int x = CardArt.spriteLeft(slotX) + (CardArt.SPRITE - size) / 2;
        int y = CardArt.spriteTop() + (CardArt.SPRITE - size) / 2 + PotionSpill.slump(effectElapsed);

        // Rotated about its own centre, which is safe only because the texture
        // is nearest filtered — see PotionDrink for why.
        batch.draw(effectArt.spentBottle(), x, CardArt.toWorldY(y, size),
                size / 2f, size / 2f, size, size, 1f, 1f,
                PotionSpill.tiltDegrees(effectElapsed));

        // The liquid, drained the same way the glass was, running out to the
        // side the bottle has gone over rather than falling straight down.
        int spilt = Ramps.drain(HudArt.FILL_HEAL);
        for (int drop = 0; drop < PotionSpill.dropsFallen(effectElapsed); drop++) {
            batch.setColor(((spilt >>> 16) & 0xff) / 255f, ((spilt >>> 8) & 0xff) / 255f,
                    (spilt & 0xff) / 255f, 1f);
            batch.draw(theme.whiteRegion(), x - 8 - drop * 6,
                    CardArt.toWorldY(y + size - 6 + drop * 4, 4), 4, 4);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * The region to draw for a card right now. Only creatures have idle frames
     * — none were drawn for weapons or potions — so everything else is its
     * static base sprite, which is frame 1 of a cycle anyway.
     */
    TextureRegion spriteFor(Card card, boolean animating) {
        if (card.type() != CardType.MONSTER) {
            return sprites.region(CardSprites.regionName(card));
        }
        Array<TextureRegion> frames = sprites.frames(CardSprites.idleStem(card));
        float offset = idleOffsets.getOrDefault(card.id(), 0f);
        return frames.get(IdleCycle.frameIndex(elapsed, offset, frames.size, animating));
    }

    /** The screen-death pattern, which lives with the other generated shapes. */
    TextureRegion dither(int level, int width, int height) {
        return effectArt.ditherAt(level, width, height);
    }

    void dispose() {
        pips.dispose();
        effectArt.dispose();
    }
}
