package com.tomer.scoundrel.screens;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tomer.scoundrel.model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plays purely cosmetic move animations over the already-rebuilt board. The
 * state underneath is final before any motion starts, so skipping is always
 * safe: {@link #finish()} just discards the flight proxies and reveals the
 * real tiles. While a choreography plays, a fullscreen gate covers the board;
 * a click on it settles the board immediately AND is handed to the
 * {@link SkipListener}, so the same click still resolves the card it landed
 * on — no click is ever wasted.
 */
final class Choreographer {

    /** Told where a skip-click landed, once the board is settled. */
    @FunctionalInterface
    interface SkipListener {
        void skippedAt(float stageX, float stageY);
    }

    private final Stage stage;
    private final Theme theme;
    private final SkipListener skipListener;
    private final Group flightLayer = new Group();
    private final Actor gate = new Actor();
    private final List<Actor> hiddenTiles = new ArrayList<>();
    private boolean playing;

    Choreographer(Stage stage, Theme theme, SkipListener skipListener) {
        this.stage = stage;
        this.theme = theme;
        this.skipListener = skipListener;
        flightLayer.setTouchable(Touchable.disabled);
        // Press, not click, and the press coordinates: the mouse may already be
        // travelling by the time the button comes back up.
        gate.addListener(Widgets.pressListenerAt((stageX, stageY) -> {
            finish(); // reveals the real tiles before we act on them
            skipListener.skippedAt(stageX, stageY);
        }));
    }

    boolean isPlaying() {
        return playing;
    }

    /**
     * Deal-in: the real room tiles (already laid out) are hidden and proxies
     * fly to them — from their previous slot for the carryover card, from the
     * dungeon (the depth ticker) for freshly dealt ones. {@code roomTiles}
     * must iterate in room order for the stagger to read left-to-right.
     */
    void playDealIn(Map<Card, Table> roomTiles, Map<String, Vector2> previousSlots, Vector2 dungeonSource) {
        begin();
        float total = spawnDealProxies(roomTiles, previousSlots, dungeonSource, 0f);
        flightLayer.addAction(Actions.delay(total, Actions.run(this::finish)));
    }

    /**
     * Avoid: the outgoing room sweeps up into the dungeon (the depth ticker),
     * then the fresh room deals in — everything from the ticker, including
     * any cards the shallow end-of-dungeon deals right back out.
     */
    void playAvoid(List<Card> avoidedCards, Map<String, Vector2> previousSlots,
                   Map<Card, Table> roomTiles, Vector2 dungeonSource) {
        begin();
        for (Card card : avoidedCards) {
            Vector2 from = previousSlots.get(card.id());
            if (from == null) {
                continue;
            }
            Table proxy = buildProxy(card);
            proxy.setPosition(from.x, from.y);
            proxy.addAction(Actions.parallel(
                    Actions.moveTo(dungeonSource.x - Theme.CARD_WIDTH / 2f,
                            dungeonSource.y - Theme.CARD_HEIGHT / 2f,
                            Theme.SWEEP_DURATION, Interpolation.pow2In),
                    Actions.scaleTo(0.15f, 0.15f, Theme.SWEEP_DURATION, Interpolation.pow2In),
                    Actions.delay(Theme.SWEEP_DURATION * 0.4f,
                            Actions.fadeOut(Theme.SWEEP_DURATION * 0.6f))));
            flightLayer.addActor(proxy);
        }
        float total = spawnDealProxies(roomTiles, Map.of(), dungeonSource, Theme.SWEEP_DURATION);
        flightLayer.addAction(Actions.delay(total, Actions.run(this::finish)));
    }

    /**
     * Bare-handed kill: the monster's tile shudders under two impact flares in
     * its old slot, then — if the room refilled — the fresh cards deal in once
     * the last blow has landed. One gate covers the whole beat.
     */
    void playBarehanded(Card monster, Vector2 slot, Map<Card, Table> roomTiles,
                        Map<String, Vector2> previousSlots, Vector2 dungeonSource, boolean dealAfter) {
        begin();
        float strike = spawnStrike(monster, slot);
        float total = dealAfter
                ? spawnDealProxies(roomTiles, previousSlots, dungeonSource, strike)
                : strike;
        flightLayer.addAction(Actions.delay(total, Actions.run(this::finish)));
    }

    /** The struck tile shakes and two flares land in turn; returns the strike's length. */
    private float spawnStrike(Card monster, Vector2 slot) {
        float strike = Motion.strikeWindow(
                Theme.STRIKE_HITS, Theme.STRIKE_HIT_STAGGER, Theme.STRIKE_HIT_DURATION);
        Table proxy = buildProxy(monster);
        proxy.setPosition(slot.x, slot.y);
        proxy.addAction(Actions.sequence(
                Actions.moveBy(8, 0, 0.04f), Actions.moveBy(-16, 0, 0.06f),
                Actions.moveBy(12, 0, 0.05f), Actions.moveBy(-4, 0, 0.04f)));
        proxy.addAction(Actions.sequence(
                Actions.delay(strike * 0.6f),
                Actions.fadeOut(strike * 0.4f, Interpolation.pow2In)));
        flightLayer.addActor(proxy);

        float centreX = slot.x + Theme.CARD_WIDTH / 2f;
        float centreY = slot.y + Theme.CARD_HEIGHT / 2f;
        for (int i = 0; i < Theme.STRIKE_HITS; i++) {
            float offsetX = (i % 2 == 0) ? -18f : 20f; // the blows land a little apart
            float offsetY = (i % 2 == 0) ? 22f : -20f;
            flightLayer.addActor(spawnFlare(centreX + offsetX, centreY + offsetY,
                    i * Theme.STRIKE_HIT_STAGGER));
        }
        return strike;
    }

    /**
     * Equip: the weapon card flies from its slot into the trophy rail, shrinking
     * and cross-fading into an axe as it goes; the real rail mini stays hidden
     * until it lands. If the room refilled, the fresh cards deal in behind it.
     */
    void playEquip(Card weapon, Vector2 fromSlot, Actor railMini, Map<Card, Table> roomTiles,
                   Map<String, Vector2> previousSlots, Vector2 dungeonSource, boolean dealAfter) {
        begin();
        Vector2 to = railMini.localToStageCoordinates(new Vector2(0, 0));
        railMini.setVisible(false);
        hiddenTiles.add(railMini);
        float fly = spawnEquipFlight(weapon, fromSlot,
                to.x + railMini.getWidth() / 2f, to.y + railMini.getHeight() / 2f,
                railMini.getHeight());
        float total = dealAfter
                ? spawnDealProxies(roomTiles, previousSlots, dungeonSource, fly)
                : fly;
        flightLayer.addAction(Actions.delay(total, Actions.run(this::finish)));
    }

    /** The card shrinks and fades into a battleaxe that lands {@code destAxeSize} tall at the slot. */
    private float spawnEquipFlight(Card weapon, Vector2 fromSlot,
                                   float destCentreX, float destCentreY, float destAxeSize) {
        float fly = Theme.EQUIP_FLIGHT;
        Group flyer = new Group();
        flyer.setSize(Theme.CARD_WIDTH, Theme.CARD_HEIGHT);
        flyer.setTransform(true);
        flyer.setOrigin(Theme.CARD_WIDTH / 2f, Theme.CARD_HEIGHT / 2f);
        flyer.setPosition(fromSlot.x, fromSlot.y);

        Table card = CardTiles.build(theme, weapon);
        card.setSize(Theme.CARD_WIDTH, Theme.CARD_HEIGHT);
        card.addAction(Actions.fadeOut(fly * 0.55f));
        flyer.addActor(card);

        // A battleaxe that fills the card, cross-fading in as the card fades out.
        float axeSize = Theme.CARD_WIDTH * 0.7f;
        Image axeIcon = new Image(theme.axeRegion());
        axeIcon.setColor(Theme.IRON);
        axeIcon.setSize(axeSize, axeSize);
        axeIcon.setPosition((Theme.CARD_WIDTH - axeSize) / 2f, (Theme.CARD_HEIGHT - axeSize) / 2f);
        axeIcon.getColor().a = 0f;
        axeIcon.addAction(Actions.sequence(
                Actions.delay(fly * 0.2f), Actions.alpha(1f, fly * 0.4f)));
        flyer.addActor(axeIcon);

        // Scale so the in-group axe ends exactly the rail axe's size — no pop on landing.
        float finalScale = destAxeSize / axeSize;
        flyer.addAction(Actions.parallel(
                Actions.moveTo(destCentreX - Theme.CARD_WIDTH / 2f,
                        destCentreY - Theme.CARD_HEIGHT / 2f, fly, Interpolation.pow2In),
                Actions.scaleTo(finalScale, finalScale, fly, Interpolation.pow2In)));
        flightLayer.addActor(flyer);
        return fly;
    }

    /** A bone flare that punches in and fades, scaling up around its centre. */
    private Group spawnFlare(float centreX, float centreY, float delay) {
        float d = 92f;
        Image star = new Image(theme.burstRegion());
        star.setSize(d, d);
        star.setColor(Theme.BONE);
        Group flare = new Group();
        flare.setSize(d, d);
        flare.setTransform(true);
        flare.setOrigin(d / 2f, d / 2f);
        flare.setPosition(centreX - d / 2f, centreY - d / 2f);
        flare.addActor(star);
        flare.setScale(0.4f);
        flare.getColor().a = 0f;
        flare.addAction(Actions.delay(delay, Actions.parallel(
                Actions.sequence(
                        Actions.alpha(1f, Theme.STRIKE_HIT_DURATION * 0.3f),
                        Actions.alpha(0f, Theme.STRIKE_HIT_DURATION * 0.7f)),
                Actions.scaleTo(1.3f, 1.3f, Theme.STRIKE_HIT_DURATION, Interpolation.pow2Out))));
        return flare;
    }

    private void begin() {
        finish();
        playing = true;
        gate.setBounds(0, 0, stage.getWidth(), stage.getHeight());
        stage.addActor(flightLayer);
        stage.addActor(gate);
    }

    /**
     * Hides the real tiles and spawns their flight proxies; deal actions
     * start after {@code baseDelay}. Returns the time until the last proxy
     * lands. Proxies waiting on a delay sit invisible at the ticker.
     */
    private float spawnDealProxies(Map<Card, Table> roomTiles, Map<String, Vector2> previousSlots,
                                   Vector2 dungeonSource, float baseDelay) {
        int slot = 0;
        for (Map.Entry<Card, Table> entry : roomTiles.entrySet()) {
            Card card = entry.getKey();
            Table tile = entry.getValue();
            Vector2 destination = tile.localToStageCoordinates(new Vector2(0, 0));
            Vector2 from = previousSlots.get(card.id());
            float delay = baseDelay + slot * Theme.DEAL_STAGGER;

            tile.setVisible(false);
            hiddenTiles.add(tile);

            Table proxy = buildProxy(card);
            if (from != null) {
                // The carryover card slides from where it just was.
                proxy.setPosition(from.x, from.y);
                proxy.addAction(Actions.delay(delay, Actions.moveTo(
                        destination.x, destination.y, Theme.DEAL_DURATION, Interpolation.pow2Out)));
            } else {
                // Fresh cards emerge from the dungeon — the depth ticker.
                proxy.setPosition(dungeonSource.x - Theme.CARD_WIDTH / 2f,
                        dungeonSource.y - Theme.CARD_HEIGHT / 2f);
                proxy.setScale(0.2f);
                proxy.getColor().a = 0f;
                proxy.addAction(Actions.delay(delay, Actions.parallel(
                        Actions.moveTo(destination.x, destination.y, Theme.DEAL_DURATION, Interpolation.pow2Out),
                        Actions.scaleTo(1f, 1f, Theme.DEAL_DURATION, Interpolation.pow2Out),
                        Actions.fadeIn(Theme.DEAL_DURATION * 0.6f))));
            }
            flightLayer.addActor(proxy);
            slot++;
        }
        return Motion.dealWindow(roomTiles.size(), baseDelay,
                Theme.DEAL_STAGGER, Theme.DEAL_DURATION);
    }

    private Table buildProxy(Card card) {
        Table proxy = CardTiles.build(theme, card);
        proxy.setSize(Theme.CARD_WIDTH, Theme.CARD_HEIGHT);
        proxy.setTransform(true);
        proxy.setOrigin(Theme.CARD_WIDTH / 2f, Theme.CARD_HEIGHT / 2f);
        return proxy;
    }

    /**
     * Ends any running choreography immediately — natural completion and a
     * skip click both land here. The board underneath is already final.
     */
    void finish() {
        if (!playing) {
            return;
        }
        playing = false;
        flightLayer.clearActions();
        flightLayer.clearChildren();
        flightLayer.remove();
        gate.remove();
        for (Actor tile : hiddenTiles) {
            tile.setVisible(true);
        }
        hiddenTiles.clear();
    }
}
