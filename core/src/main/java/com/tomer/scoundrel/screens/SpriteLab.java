package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.rules.CardDefinition;
import com.tomer.scoundrel.rules.StandardDeck;

import java.util.ArrayList;
import java.util.List;

/**
 * A developer-only art inspector, opened with F9, closed with Escape and
 * switched between views with Tab. It exists to answer the verify questions in
 * the art conversion — is the art crisp at an integer scale, are all 31 objects
 * on their right ranks, do the idle cycles run, does each effect read — without
 * having to play a real game to reach them.
 *
 * <p>It draws the room through the same {@link BoardView} the game does: an
 * instrument is only worth anything if what it shows is literally what ships.
 * What is here is the triggering — a key per effect, and S to run them at an
 * eighth speed, because a sub-second effect cannot be screenshotted at its own.
 */
public final class SpriteLab extends ScreenAdapter {

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
    private final BoardView board;
    private final BoardHud hud;
    private final PixelViewport viewport;
    private final SpriteBatch batch;

    private final List<Card> deck = new ArrayList<>();
    private View view = View.ROOM;
    /** S slows effects 8x. Sub-second animation cannot be screenshotted at speed. */
    private boolean slowMotion;
    private final GlyphLayout titleLayout = new GlyphLayout();
    private final PixelSurface surface =
            new PixelSurface((int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT);
    /** A hit or a drink landing on the bar. */
    private float damageElapsed = -1f;
    private float healElapsed = -1f;
    /** The death cinematic, and the card that dealt the blow. */
    private float deathElapsed = -1f;
    private int killerSlotX = -1;

    public SpriteLab(ScoundrelGame game, Theme theme, Sprites sprites) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        this.board = new BoardView(theme, sprites);
        this.hud = new BoardHud(theme);
        // One fixed virtual resolution, so the layout numbers are literal and
        // the art is guaranteed to land on whole pixels.
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.batch = new SpriteBatch();
        for (CardDefinition def : new StandardDeck().cards()) {
            deck.add(new Card(def.id(), def.type(), def.value()));
        }
        board.setRoom(room());
    }

    /**
     * The four cards on show — the reference board's room, so a side-by-side
     * against it means something, and so equip and drink have something to act
     * on.
     */
    private List<Card> room() {
        return List.of(cardWithId("10C"), cardWithId("7D"),
                cardWithId("QS"), cardWithId("5H"));
    }

    /** Puts the room back after an effect has taken a card out of it. */
    private void reset() {
        board.skip();
        board.setRoom(room());
        board.beginMove();
    }

    @Override
    public void render(float delta) {
        float step = slowMotion ? delta / 8f : delta;
        if (!handleKeys()) {
            // Escape switched screens, which disposed this one. Anything drawn
            // past that point goes through a freed batch onto freed textures,
            // and takes the JVM down with it rather than throwing.
            return;
        }
        board.update(step);
        advanceBar(step);
        if (deathElapsed >= 0f) {
            deathElapsed += step;
            if (DeathCinematic.finished(deathElapsed)) {
                deathElapsed = -1f;
            }
        }

        // Onto the same 1:1 surface the game uses, for the same reason: the lab
        // is only a verification instrument while it shows literally what ships,
        // and that has to include how the image is scaled to the window.
        surface.begin(new Color((CardArt.BACKDROP << 8) | 0xff));
        batch.setProjectionMatrix(surface.projection());
        batch.begin();

        int shake = deathElapsed >= 0f ? DeathCinematic.shakeX(deathElapsed) : 0;
        batch.getTransformMatrix().translate(shake, 0, 0);
        batch.setTransformMatrix(batch.getTransformMatrix());
        if (view == View.ROOM) {
            drawRoom();
        } else {
            drawSheet();
        }
        batch.getTransformMatrix().translate(-shake, 0, 0);
        batch.setTransformMatrix(batch.getTransformMatrix());
        if (deathElapsed >= 0f) {
            drawDeath();
        }

        theme.pixelSmall.setColor(Theme.BONE);
        theme.pixelSmall.draw(batch,
                "K KILL  B BARE  A AVOID  E EQUIP  P POTION  W WASTED  D HIT  H HEAL  X DEATH  S SLOW",
                40, 48);
        theme.pixelSmall.setColor(Color.WHITE);
        batch.end();
        surface.end();

        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        surface.draw(batch, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        batch.end();
    }

    /**
     * One key per effect, on whatever card the pointer is over. Returns false
     * if the lab has just been left, in which case it no longer exists and the
     * caller must not draw.
     */
    private boolean handleKeys() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showTitle(); // disposes this screen
            return false;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            view = view == View.ROOM ? View.SHEET : View.ROOM;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            slowMotion = !slowMotion;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            damageElapsed = 0f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            healElapsed = 0f;
        }
        Card hovered = board.hovered();
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            List<Card> outgoing = board.room();
            board.beginMove();
            board.setRoom(room());
            board.playSweep(outgoing);
        }
        if (hovered == null) {
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            deathElapsed = 0f;
            killerSlotX = board.slotX(board.room().indexOf(hovered));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            without(hovered, () -> board.playSlice(hovered));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
            without(hovered, () -> board.playStrike(hovered));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) && hovered.type() == CardType.WEAPON) {
            without(hovered, () -> board.playEquip(hovered));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && hovered.type() == CardType.POTION) {
            without(hovered, () -> board.playPotion(hovered, () -> healElapsed = 0f));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.W) && hovered.type() == CardType.POTION) {
            without(hovered, () -> board.playSpill(hovered));
        }
        return true;
    }

    /**
     * Resolves a card the way the game does — it leaves the room, the rest
     * closes up, and the effect plays over the gap it left.
     */
    private void without(Card card, Runnable effect) {
        board.beginMove();
        List<Card> rest = new ArrayList<>(room());
        rest.remove(card);
        board.setRoom(rest);
        effect.run();
    }

    private void advanceBar(float step) {
        if (damageElapsed >= 0f) {
            damageElapsed += step;
            if (HpPulse.damageFinished(HIT_FROM, HIT_TO, damageElapsed)) {
                damageElapsed = -1f;
            }
        }
        if (healElapsed >= 0f) {
            healElapsed += step;
            if (HpPulse.healFinished(HEAL_FROM, HEAL_TO, healElapsed)) {
                healElapsed = -1f;
            }
        }
    }

    /** The reference board, drawn by the same code the game uses. */
    private void drawRoom() {
        board.setHovered(hoveredCard());
        if (!board.isPlaying() && board.room().size() < 4) {
            reset(); // put the resolved card back, ready for the next trigger
        }
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
        hud.drawDepthLine(batch, 27, "01:47");
        hud.drawAvoid(batch, true);
        // The reference's rail and marker: a broadaxe that has taken a 10 and
        // an 8, and an unused draught.
        hud.drawRail(batch, sprites.region(CardSprites.regionName(cardWithId("9D"))),
                CardSprites.displayName(cardWithId("9D")) + " 9",
                List.of(cardWithId("10S"), cardWithId("8C")), "SLAYS < 8");
        hud.drawPotionMarker(batch,
                sprites.region(CardSprites.regionName(cardWithId("5H"))), false);
        board.draw(batch);

        theme.pixelSmall.setColor(Theme.BONE);
        theme.pixelSmall.draw(batch, "ROOM — HOVER TO ANIMATE"
                + (slowMotion ? "   [SLOW 1/8]" : ""), 40, 700);
        theme.pixelSmall.setColor(Color.WHITE);
    }

    private Card hoveredCard() {
        Vector2 point = viewport.unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        return board.cardAt(point.x, point.y);
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

        theme.pixelSmall.setColor(Theme.BONE);
        for (int value = 2; value <= 14; value++) {
            theme.pixelSmall.draw(batch, Labels.rank(value), left + (value - 2) * cell + 24,
                    CardArt.toWorldY(top - 12, 0));
        }
        for (int row = 0; row < 4; row++) {
            int y = top + row * 96;
            theme.pixelSmall.draw(batch, rowLabels[row], 24, CardArt.toWorldY(y + 26, 0));
            for (Card card : deck) {
                if (rowOf(card) != row) {
                    continue;
                }
                int x = left + (card.value() - 2) * cell + 12;
                batch.draw(board.spriteFor(card, false),
                        x, CardArt.toWorldY(y, Sprites.SIZE), Sprites.SIZE, Sprites.SIZE);
            }
        }
        theme.pixelSmall.draw(batch, "SHEET — ALL 44 CARDS, 31 OBJECTS, BY RANK", 40, 700);
        theme.pixelSmall.setColor(Color.WHITE);
    }

    /**
     * The death: a red flare over whatever killed you, then the screen going
     * out by ordered dither with the title growing over it. Drawn last so the
     * pattern covers the whole board.
     */
    private void drawDeath() {
        if (DeathCinematic.flaring(deathElapsed) && killerSlotX >= 0) {
            batch.setColor(0.55f, 0.18f, 0.13f, DeathCinematic.flareStrength(deathElapsed));
            batch.draw(theme.whiteRegion(), killerSlotX,
                    CardArt.toWorldY(CardArt.SLOT_Y, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H);
            batch.setColor(1f, 1f, 1f, 1f);
        }
        int level = DeathCinematic.ditherLevel(deathElapsed);
        if (level > 0) {
            // One tiled draw of a 4x4 pattern. Every pixel is either fully dark
            // or fully clear, so the board thins out rather than dimming.
            batch.draw(board.dither(level, (int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT),
                    0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        }
        if (DeathCinematic.titleShowing(deathElapsed)) {
            // Placed exactly as the game places it — the lab is only a useful
            // instrument while it shows literally what ships.
            // The smallest face in the game, blown up by a whole number. The
            // multiples are the only clean sizes there are, so the smaller the
            // face the more of them fit between "far away" and "in your face".
            BitmapFont font = theme.pixelSmall;
            font.getData().setScale(DeathCinematic.titleZoom(deathElapsed));
            titleLayout.setText(font, "YOU DIED");
            int top = BoardArt.DEATH_TITLE_CENTRE_Y - Math.round(titleLayout.height / 2f);
            font.setColor(Color.valueOf("8c2f22"));
            font.draw(batch, "YOU DIED", 0, CardArt.toWorldY(top, 0),
                    Theme.WORLD_WIDTH, Align.center, false);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
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
        surface.dispose();
        board.dispose();
        batch.dispose();
    }
}
