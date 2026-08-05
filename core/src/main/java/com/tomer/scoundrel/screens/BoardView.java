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
 * The room of face-up cards: the frames, the sprites in their wells, and the
 * printing on them. One class so the developer lab and the real board are
 * drawing the same thing — the lab is only a verification instrument if what it
 * shows is literally what the game shows.
 *
 * <p>It owns the idle clock and each card's stagger, and nothing else: no game
 * state, no rules, no input. Callers say which cards are in the room and where
 * the pointer is; this draws them.
 */
final class BoardView {

    private final Sprites sprites;
    private final CardFrame cardFrame;
    private final CardFace cardFace;
    private final Pips pips;
    private final Random random = new Random();

    /** Per-card start offsets, assigned when a card is dealt — never per frame. */
    private final Map<String, Float> idleOffsets = new HashMap<>();
    private List<Card> room = List.of();
    private float elapsed;
    /** The card under the pointer, or null. Only it animates. */
    private Card hovered;

    BoardView(Theme theme, Sprites sprites) {
        this.sprites = sprites;
        this.cardFrame = new CardFrame(theme);
        this.pips = new Pips();
        this.cardFace = new CardFace(theme, pips);
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

    void update(float delta) {
        elapsed += delta;
    }

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

    void draw(Batch batch) {
        for (int i = 0; i < room.size(); i++) {
            drawCard(batch, room.get(i), slotX(i), CardArt.SLOT_Y);
        }
    }

    /** One card, whole, at a design-space position — the ordinary resting state. */
    void drawCard(Batch batch, Card card, int slotX, int slotY) {
        cardFrame.draw(batch, card.type(), slotX, slotY);
        batch.draw(spriteFor(card, card.equals(hovered)), CardArt.spriteLeft(slotX),
                CardArt.toWorldY(slotY + (CardArt.spriteTop() - CardArt.SLOT_Y), CardArt.SPRITE),
                CardArt.SPRITE, CardArt.SPRITE);
        cardFace.draw(batch, card, slotX, slotY);
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

    void dispose() {
        pips.dispose();
    }
}
