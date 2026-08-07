package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.achievements.Achievement;
import com.tomer.scoundrel.achievements.AchievementContext;
import com.tomer.scoundrel.achievements.AchievementService;
import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.achievements.AchievementTracker;
import com.tomer.scoundrel.achievements.Achievements;
import com.tomer.scoundrel.achievements.RunSummary;
import com.tomer.scoundrel.achievements.UnlockedAchievement;
import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;
import com.tomer.scoundrel.model.EquippedWeapon;
import com.tomer.scoundrel.model.GameState;
import com.tomer.scoundrel.model.Status;
import com.tomer.scoundrel.rules.GameEvent;
import com.tomer.scoundrel.rules.GameMode;
import com.tomer.scoundrel.rules.Move;
import com.tomer.scoundrel.rules.MoveResult;
import com.tomer.scoundrel.rules.Ruleset;
import com.tomer.scoundrel.rules.ScoundrelEngine;
import com.tomer.scoundrel.runs.HighScores;
import com.tomer.scoundrel.runs.RunLog;
import com.tomer.scoundrel.runs.RunRecord;
import com.tomer.scoundrel.runs.RunRecorder;
import com.tomer.scoundrel.tutorial.TutorialGuide;
import com.tomer.scoundrel.tutorial.TutorialScript;
import com.tomer.scoundrel.tutorial.TutorialStep;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Random;

import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;
import static com.tomer.scoundrel.screens.Widgets.mutedButton;
import static com.tomer.scoundrel.screens.Widgets.torchButton;

/**
 * The one in-game screen: draws the current GameState and translates clicks
 * into engine moves. Contains no rule logic — everything it knows about the
 * game comes from the state, the ruleset, and legalMoves().
 *
 * <p>The board itself is drawn in immediate mode, straight onto a batch at the
 * design resolution, because the art is specified as pixels at fixed positions
 * and a layout engine's job is to compute positions. The overlays that still
 * have buttons — the end screen, the move chooser, the tutorial callout — remain
 * Scene2D on a stage drawn over the top, until the screens pass converts them.
 */
public final class GameScreen extends ScreenAdapter {

    /** The stage background the art specifies, behind the torchlit glow. */
    private static final Color BACKDROP = new Color(
            (CardArt.BACKDROP << 8) | 0xff);
    /** Dried blood, the one colour YOU DIED is ever set in. */
    private static final Color DEATH_TITLE = new Color((0x8c2f22 << 8) | 0xff);
    private static final String DEATH_TITLE_TEXT = "YOU DIED";

    private final ScoundrelGame game;
    private final Theme theme;
    private final GameMode mode;
    private final Ruleset rules;
    private final ScoundrelEngine engine;
    private final RunLog runLog;
    private final AchievementStore achievements;
    private final TutorialGuide tutorial; // null unless this is the guided tutorial

    private final PixelViewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    /**
     * The board is drawn onto this at 1:1 and scaled to the window once, so
     * every element is resampled together rather than each draw rounding on its
     * own — see {@link PixelSurface}.
     */
    private final PixelSurface surface =
            new PixelSurface((int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT);
    private final Backdrop backdrop;
    private final BoardView board;
    private final BoardHud hud;
    private final Sprites sprites;
    private final Feed feed = new Feed();
    /** Measures the death title, so it can be placed by its centre. */
    private final GlyphLayout titleLayout = new GlyphLayout();
    /** Overlays only — everything with a button on it. */
    private final Stage stage;

    private GameState state;
    private RunRecorder recorder;
    private long finalRunSeconds;
    private AchievementTracker tracker;
    private String endBestLine;
    private List<Achievement> newlyUnlocked = List.of();
    private Actor endOverlay;
    private Actor tutorialLayer;
    /** The open move chooser: the moves offered, and the card they are about. */
    private List<Move> chooserMoves = List.of();
    private Card chooserCard;
    private int chooserPlateW;
    /** The last potion drunk, which is what the marker shows once one has been. */
    private Card lastPotion;
    /** What was in the rail before this move, for as long as the new one is in the air. */
    private EquippedWeapon weaponBeforeMove;

    // --- what the health bar is doing, and the death ---

    /** What the health bar is doing, what it is doing it between, and since when. */
    private HealthReadout.Phase barPhase = HealthReadout.Phase.REST;
    private HealthReadout.Change barChange = HealthReadout.Change.NONE;
    private float barElapsed;
    /** The death cinematic's clock and the slot the fatal blow landed in. */
    private float deathElapsed = -1f;
    private int killerSlotX = -1;
    /** Withheld until the cinematic ends, so the score does not pre-empt it. */
    private boolean endPending;

    /** A normal run in the given mode, recorded to the run log. */
    public GameScreen(ScoundrelGame game, Theme theme, Sprites sprites, RunLog runLog,
                      AchievementStore achievements, GameMode mode) {
        this(game, theme, sprites, runLog, achievements, mode, null);
    }

    /** The guided tutorial: a scripted deck with narration, never recorded. */
    public GameScreen(ScoundrelGame game, Theme theme, Sprites sprites,
                      GameMode mode, TutorialGuide tutorial) {
        this(game, theme, sprites, null, null, mode, tutorial);
    }

    private GameScreen(ScoundrelGame game, Theme theme, Sprites sprites, RunLog runLog,
                       AchievementStore achievements, GameMode mode, TutorialGuide tutorial) {
        this.game = game;
        this.theme = theme;
        this.sprites = sprites;
        this.runLog = runLog;
        this.achievements = achievements;
        this.mode = mode;
        this.tutorial = tutorial;
        this.rules = mode.ruleset();
        this.engine = new ScoundrelEngine(rules);
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.backdrop = new Backdrop(theme);
        this.board = new BoardView(theme, sprites);
        this.hud = new BoardHud(theme);
        this.stage = new Stage(new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT));
        startRun();
        board.dealFresh(state.room());
        syncBoard();
    }

    @Override
    public void show() {
        // The stage takes presses first, so an overlay swallows what lands on
        // it; anything it does not want falls through to the board.
        InputMultiplexer input = new InputMultiplexer(stage, new BoardInput());
        Gdx.input.setInputProcessor(input);
    }

    /** Clicks on the board itself: a card, or the Avoid plate. */
    private final class BoardInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            // A click through a cinematic settles it. The state underneath is
            // already final, so skipping is always safe — and the same click
            // still resolves whatever it landed on, so none is ever spent
            // purely on dismissing motion.
            if (deathElapsed >= 0f) {
                settleEnd();
                return true;
            }
            Vector2 point = viewport.unproject(new Vector2(screenX, screenY));
            // An open chooser takes the press first. One that lands on a plate
            // resolves it; one that lands anywhere else dismisses the chooser
            // AND still resolves whatever card it hit, so a press is never
            // spent merely closing the popup.
            if (chooserCard != null) {
                int picked = ChooserArt.indexAt(chooserSlotX(), chooserPlateW,
                        chooserMoves.size(), point.x, point.y);
                List<Move> offered = chooserMoves;
                closeChooser();
                if (picked >= 0) {
                    applyMove(offered.get(picked));
                    return true;
                }
            }
            if (board.isPlaying()) {
                board.skip();
            }
            if (state.status() != Status.IN_PROGRESS) {
                return false;
            }
            if (HudArt.avoidContains(point.x, point.y) && avoidAllowed()) {
                applyMove(new Move.AvoidRoom());
                return true;
            }
            Card card = board.cardAt(point.x, point.y);
            if (card != null) {
                onCardClicked(card);
                return true;
            }
            return false;
        }
    }

    @Override
    public void render(float delta) {
        advance(delta);

        // Everything at 1:1 on the surface's own grid first.
        surface.begin(BACKDROP);
        batch.setProjectionMatrix(surface.projection());
        batch.begin();
        // The death shakes the board but not the dark it happens in, so the
        // backdrop is drawn before the jolt is applied.
        backdrop.render(batch,
                deathElapsed >= 0f ? DeathCinematic.torchLight(deathElapsed) : 1f);
        int shake = deathElapsed >= 0f ? DeathCinematic.shakeX(deathElapsed) : 0;
        batch.getTransformMatrix().translate(shake, 0, 0);
        batch.setTransformMatrix(batch.getTransformMatrix());
        drawHud();
        board.draw(batch);
        drawChooser();
        drawFeed();
        batch.getTransformMatrix().translate(-shake, 0, 0);
        batch.setTransformMatrix(batch.getTransformMatrix());
        if (deathElapsed >= 0f) {
            drawDeath();
        }
        batch.end();
        surface.end();

        // Then that one image to the window, in one scale.
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        batch.draw(surface.region(), 0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        batch.end();

        // The overlays are still Scene2D in the vector faces, which supersample
        // and so want the window's own resolution rather than the surface's.
        stage.act(delta);
        stage.draw();
    }

    /** Moves every clock on: the board, the bar, the feed and the death. */
    private void advance(float delta) {
        backdrop.advance(delta);
        board.update(delta);
        feed.update(delta);
        board.setHovered(hoveredCard());
        advanceBar(delta);
        if (deathElapsed >= 0f) {
            deathElapsed += delta;
            if (DeathCinematic.finished(deathElapsed)) {
                deathElapsed = -1f;
                settleEnd();
            }
        }
    }

    /** The card under the pointer, so only it animates. */
    private Card hoveredCard() {
        if (state.status() != Status.IN_PROGRESS || endOverlay != null) {
            return null;
        }
        Vector2 point = viewport.unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        return board.cardAt(point.x, point.y);
    }

    private void drawHud() {
        drawHealth();
        drawDepthGauge();
        hud.drawAvoid(batch, avoidAllowed());
        drawRail();
        hud.drawPotionMarker(batch, potionMarkerRegion(),
                state.potionsUsedThisRoom() >= rules.potionsPerTurn());
    }

    /** The ticks and the line under them — the one gauge of how far you got. */
    private void drawDepthGauge() {
        int depth = shownDepth();
        hud.drawTicker(batch, depth, rules.deck().cards().size());
        hud.drawDepthLine(batch, depth,
                tutorial == null ? ClockText.format(currentRunSeconds()) : null);
    }

    /**
     * How deep the dungeon looks, which is not always how deep it is. The engine
     * settles the whole move at once, so its count drops before a single card
     * has moved; on screen a card is only back in the deck once it has flown
     * there, and only out of it once it has landed on the table.
     *
     * <p>Both corrections are needed and they run in opposite directions, which
     * is what makes an avoided room read properly: the strip grows by four as
     * the old room goes in, then shrinks by four as the new one comes out, and
     * ends exactly where the engine says it should.
     */
    private int shownDepth() {
        return state.dungeon().size() + board.rising() - board.sweeping();
    }

    /**
     * The bar's clock. Only a change runs one — a bar at rest, or one holding
     * for a bottle that has not landed, has nothing to advance.
     */
    private void advanceBar(float delta) {
        if (barPhase != HealthReadout.Phase.HEALING
                && barPhase != HealthReadout.Phase.BLEEDING) {
            return;
        }
        barElapsed += delta;
        boolean over = barPhase == HealthReadout.Phase.HEALING
                ? HpPulse.healFinished(barChange.fromWidth(), barChange.toWidth(), barElapsed)
                : HpPulse.damageFinished(barChange.fromWidth(), barChange.toWidth(), barElapsed);
        if (over) {
            barPhase = HealthReadout.Phase.REST;
        }
    }

    /** The bar, mid-change, holding, or at rest — the decision is out in {@link HealthReadout}. */
    private void drawHealth() {
        HealthReadout readout = HealthReadout.of(
                barPhase, state.health(), rules.healthCap(), barChange, barElapsed);
        hud.drawHealth(batch, readout.number(), rules.healthCap(),
                readout.healing(), readout.bleeding(), readout.offsetX(), readout.fill());
    }

    /**
     * Dying: a red flare over whatever killed you, the board shaking (applied
     * by the caller), and the screen going out by ordered dither with YOU DIED
     * growing over it. Pattern, never alpha — the board thins out rather than
     * dimming, so it reads as the game failing and not as a dialog covering it.
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
            batch.draw(board.dither(level, (int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT),
                    0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        }
        // Over the dither, not under it: the dark takes the whole board and
        // leaves the gauge that says how close you got, until that goes too.
        if (DeathCinematic.tickerShowing(deathElapsed)) {
            drawDepthGauge();
        }
        if (DeathCinematic.titleShowing(deathElapsed)) {
            // Whole multiples only, and placed by its own centre so it grows
            // outward from a fixed line instead of downward from a fixed top.
            // The smallest face in the game, blown up by a whole number. The
            // multiples are the only clean sizes there are, so the smaller the
            // face the more of them fit between "far away" and "in your face".
            BitmapFont font = theme.pixelSmall;
            font.getData().setScale(DeathCinematic.titleZoom(deathElapsed));
            titleLayout.setText(font, DEATH_TITLE_TEXT);
            int top = BoardArt.DEATH_TITLE_CENTRE_Y - Math.round(titleLayout.height / 2f);
            font.setColor(DEATH_TITLE);
            font.draw(batch, DEATH_TITLE_TEXT, 0, CardArt.toWorldY(top, 0),
                    Theme.WORLD_WIDTH, Align.center, false);
            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
        }
    }

    /** The cinematic is over (or was clicked through): show the score. */
    private void settleEnd() {
        deathElapsed = -1f;
        if (endPending) {
            endPending = false;
            syncBoard();
        }
    }

    /**
     * Equipped weapon, its slain stack, and how much bite it has left — as the
     * board shows it, not as the engine has already settled it. A weapon still
     * hopping down to the well has not arrived, and a monster still being
     * cleaved has not died, so neither the icon nor the chip beside it may
     * appear yet.
     */
    private void drawRail() {
        EquippedWeapon weapon = board.railAhead() ? weaponBeforeMove : state.weapon();
        if (weapon == null) {
            hud.drawRail(batch, null, "BAREHANDED", null, null);
            return;
        }
        Card held = weapon.weapon();
        hud.drawRail(batch, sprites.region(CardSprites.regionName(held)),
                CardSprites.displayName(held) + " " + held.value(),
                weapon.slain(),
                Labels.weaponThreshold(weapon).toUpperCase(Locale.ROOT));
    }

    /**
     * The marker shows the potion you drank once you have drunk one, and a
     * plain draught while the room's is still going. Never dimmed: an alpha
     * over the dark board would make colours that are on no ramp.
     */
    private TextureRegion potionMarkerRegion() {
        Card shown = lastPotion != null && state.potionsUsedThisRoom() > 0
                ? lastPotion
                : new Card("5H", CardType.POTION, 5);
        return sprites.region(CardSprites.regionName(shown));
    }

    private void drawFeed() {
        for (int i = 0; i < feed.size(); i++) {
            hud.drawFeedLine(batch, feed.textAt(i), i, feed.alphaAt(i));
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return; // minimized window
        }
        viewport.update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        board.dispose();
        surface.dispose();
        batch.dispose();
    }

    /**
     * Pushes the current state onto the board and rebuilds whichever overlay
     * belongs on top of it. Called after every move; the board itself is drawn
     * from the state each frame, so there is nothing else to rebuild.
     */
    private void syncBoard() {
        board.setRoom(state.room());
        // The room it was anchored to has just changed under it.
        closeChooser();
        if (endOverlay != null) {
            endOverlay.remove();
            endOverlay = null;
        }
        if (tutorialLayer != null) {
            tutorialLayer.remove();
            tutorialLayer = null;
        }
        if (state.status() != Status.IN_PROGRESS) {
            endOverlay = tutorial != null ? buildTutorialComplete() : buildEndOverlay();
            stage.addActor(endOverlay);
        } else if (tutorial != null && !tutorial.isComplete()) {
            tutorialLayer = buildTutorialLayer();
            stage.addActor(tutorialLayer);
        }
    }

    /** Whether Avoid is live: the tutorial gates it, otherwise the rules do. */
    private boolean avoidAllowed() {
        return tutorial != null
                ? tutorialExpects(new Move.AvoidRoom())
                : engine.legalMoves(state).contains(new Move.AvoidRoom());
    }

    /** Dim screen with the outcome, the score, and the way back in. */
    private Actor buildEndOverlay() {
        boolean won = state.status() == Status.WON;
        Table overlay = new Table();
        overlay.setFillParent(true);
        // A Table is childrenOnly by default, which would let presses fall
        // straight through to the dead board behind it.
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(theme.solid(dim(Theme.SOOT, 0.85f)));
        overlay.add(label(won ? "DUNGEON CLEARED" : "DEFEATED",
                theme.title, won ? Theme.TORCHLIGHT : Theme.DRIED_BLOOD)).padBottom(4);
        overlay.row();
        overlay.add(buildEndPanel());
        return overlay;
    }

    /** The score, best line, unlocked banner, and the four navigation buttons. */
    private Table buildEndPanel() {
        Table panel = new Table();
        panel.add(label("score " + state.score(), theme.display, Theme.BONE)).padBottom(2);
        panel.row();
        // Where that number came from — a death score charges you for monsters
        // still face-down, which is otherwise unexplained on screen.
        panel.add(label(Labels.scoreBreakdown(state.score(), state.health(),
                        rules.healthCap(), state.status() == Status.WON),
                theme.body, dim(Theme.BONE, 0.55f))).padBottom(6);
        panel.row();
        panel.add(label("time " + ClockText.format(finalRunSeconds), theme.body, dim(Theme.BONE, 0.7f)))
                .padBottom(8);
        panel.row();
        if (endBestLine != null) {
            Color bestColor = endBestLine.equals("New best!")
                    ? Theme.TORCHLIGHT : dim(Theme.BONE, 0.6f);
            panel.add(label(endBestLine, theme.bodyBold, bestColor)).padBottom(24);
            panel.row();
        }
        if (!newlyUnlocked.isEmpty()) {
            panel.add(unlockedBanner()).padBottom(24);
            panel.row();
        }
        panel.add(navButton("New game", this::startNewGame)).padBottom(10);
        panel.row();
        panel.add(navButton("Main menu", game::showTitle)).padBottom(10);
        panel.row();
        panel.add(navButton("Trophies", game::showTrophies)).padBottom(10);
        panel.row();
        panel.add(navButton("Records", game::showRecords));
        return panel;
    }

    private TextButton navButton(String text, Runnable action) {
        TextButton button = torchButton(theme, text);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    /**
     * The achievements this run just earned, listed under a torchlight heading.
     * A hidden achievement is revealed here the moment it is earned — hiding
     * only ever applies to the not-yet-earned on the trophies screen.
     */
    private Actor unlockedBanner() {
        Table banner = new Table();
        banner.add(label(newlyUnlocked.size() == 1 ? "ACHIEVEMENT UNLOCKED" : "ACHIEVEMENTS UNLOCKED",
                theme.small, Theme.TORCHLIGHT)).padBottom(6);
        for (Achievement earned : newlyUnlocked) {
            banner.row();
            banner.add(label(earned.title(), theme.bodyBold, Theme.BONE)).padBottom(2);
        }
        return banner;
    }

    private void startNewGame() {
        startRun();
        feed.clear();
        board.dealFresh(state.room());
        syncBoard();
    }

    // --- the guided tutorial's overlay ---

    /** Glow on the current step's target, a callout of its narration, and Skip. */
    private Actor buildTutorialLayer() {
        Group layer = new Group();
        layer.setBounds(0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        // Only the callout's buttons and Skip are hit targets; a full-bleed Group
        // otherwise swallows every click before it reaches the card underneath.
        layer.setTouchable(Touchable.childrenOnly);
        TutorialStep step = tutorial.current();

        float[] target = tutorialTarget(step);
        Vector2 targetCentre = null;
        float targetHeight = 0;
        if (target != null) {
            targetCentre = new Vector2(target[0] + target[2] / 2f, target[1] + target[3] / 2f);
            targetHeight = target[3];
            layer.addActor(frameHighlight(target[0], target[1], target[2], target[3]));
        }

        Table callout = buildCallout(step);
        callout.pack();
        positionCallout(callout, targetCentre, targetHeight);
        layer.addActor(callout);

        TextButton skip = mutedButton(theme, "Skip tutorial");
        skip.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTitle();
            }
        });
        skip.pack();
        // Bottom-right, but lifted clear of the potion marker in the bottom strip.
        skip.setPosition(Theme.WORLD_WIDTH - skip.getWidth() - 20, 100);
        layer.addActor(skip);
        return layer;
    }

    /**
     * The board rectangle the current step points at — a room card, the Avoid
     * plate, or none for an explanation beat. World coordinates, {@code
     * {x, y, w, h}}, because the board has no actors to ask any more.
     */
    private float[] tutorialTarget(TutorialStep step) {
        if (step.expectedMove() instanceof Move.CardMove cardMove) {
            int index = state.room().indexOf(cardMove.targetCard());
            if (index < 0) {
                return null;
            }
            return new float[] {board.slotX(index),
                    CardArt.toWorldY(CardArt.SLOT_Y, CardArt.CARD_H),
                    CardArt.CARD_W, CardArt.CARD_H};
        }
        if (step.expectedMove() instanceof Move.AvoidRoom) {
            return new float[] {HudArt.AVOID_X,
                    CardArt.toWorldY(HudArt.AVOID_Y, HudArt.AVOID_H),
                    HudArt.AVOID_W, HudArt.AVOID_H};
        }
        return null; // an explanation beat with no single focus
    }

    /**
     * A crisp outline hugging the target's frame, gently pulsing. Bone, not
     * torchlight, so it stands out against both the dark cards and the lit Avoid
     * button (a torchlight ring would vanish into the button's own colour).
     */
    private Actor frameHighlight(float x, float y, float w, float h) {
        float gap = 3f;        // clearance between the target's edge and the outline
        float thickness = 4f;
        float fx = x - gap - thickness;
        float fy = y - gap - thickness;
        float fw = w + 2 * (gap + thickness);
        float fh = h + 2 * (gap + thickness);
        Group frame = new Group();
        frame.setTouchable(Touchable.disabled);
        frame.addActor(bar(fx, fy, fw, thickness));                  // bottom
        frame.addActor(bar(fx, fy + fh - thickness, fw, thickness)); // top
        frame.addActor(bar(fx, fy, thickness, fh));                  // left
        frame.addActor(bar(fx + fw - thickness, fy, thickness, fh)); // right
        frame.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(1f, 0.55f), Actions.alpha(0.45f, 0.55f))));
        return frame;
    }

    private Image bar(float x, float y, float w, float h) {
        Image bar = new Image(theme.solid(Theme.BONE));
        bar.setBounds(x, y, w, h);
        return bar;
    }

    private Table buildCallout(TutorialStep step) {
        Table callout = new Table();
        callout.setBackground(theme.solid(Theme.STONE));
        callout.pad(14, 18, 14, 18);
        Label text = label(step.narration(), theme.body, Theme.BONE);
        text.setWrap(true);
        callout.add(text).width(320);
        if (!step.isAction()) {
            callout.row();
            TextButton next = torchButton(theme, "Next");
            next.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    tutorial.next();
                    syncBoard();
                }
            });
            callout.add(next).right().padTop(12);
        }
        return callout;
    }

    /** Above the target if it fits, else below; centred when there is no target. */
    private void positionCallout(Table callout, Vector2 targetCentre, float targetHeight) {
        float w = callout.getWidth();
        float h = callout.getHeight();
        if (targetCentre == null) {
            callout.setPosition((Theme.WORLD_WIDTH - w) / 2f, (Theme.WORLD_HEIGHT - h) / 2f);
            return;
        }
        float x = clamp(targetCentre.x - w / 2f, 16, Theme.WORLD_WIDTH - w - 16);
        float above = targetCentre.y + targetHeight / 2f + 18;
        float y = above + h <= Theme.WORLD_HEIGHT - 16
                ? above
                : targetCentre.y - targetHeight / 2f - h - 18;
        callout.setPosition(x, y);
    }

    private static float clamp(float value, float lo, float hi) {
        return Math.max(lo, Math.min(hi, value));
    }

    /**
     * The tutorial's end: nothing is recorded, but the run's real score is shown
     * and read back as the rule that produced it — scoring is the rule players
     * find most confusing, so the last thing they see is a worked example.
     */
    private Actor buildTutorialComplete() {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(theme.solid(dim(Theme.SOOT, 0.85f)));
        overlay.add(label("TUTORIAL COMPLETE", theme.title, Theme.TORCHLIGHT)).padBottom(10);
        overlay.row();
        overlay.add(label("score " + state.score(), theme.display, Theme.BONE)).padBottom(6);
        overlay.row();
        Label blurb = label(
                Labels.tutorialScore(state.score(), rules.healthCap())
                        + " Good luck down there.",
                theme.body, Theme.BONE);
        blurb.setWrap(true);
        blurb.setAlignment(com.badlogic.gdx.utils.Align.center);
        overlay.add(blurb).width(560).padBottom(26);
        overlay.row();
        overlay.add(navButton("Play for real", game::showModeSelect)).padBottom(10);
        overlay.row();
        overlay.add(navButton("Main menu", game::showTitle));
        return overlay;
    }

    /** True when the tutorial's current step expects exactly this move. */
    private boolean tutorialExpects(Move move) {
        return tutorial != null && !tutorial.isComplete()
                && move.equals(tutorial.current().expectedMove());
    }

    /** Seconds shown by the timer: live while a run is in progress, frozen after. */
    private long currentRunSeconds() {
        if (state.status() != Status.IN_PROGRESS) {
            return finalRunSeconds;
        }
        return recorder != null ? recorder.elapsedSeconds() : 0;
    }

    // --- resolving a card ---

    /** One legal move plays immediately; two or more open the chooser. */
    private void onCardClicked(Card card) {
        if (tutorial != null) {
            // In the tutorial only the highlighted card responds, and it makes
            // exactly the scripted move — no chooser to wander into.
            if (!tutorial.isComplete()
                    && tutorial.current().expectedMove() instanceof Move.CardMove cm
                    && cm.targetCard().equals(card)) {
                applyMove(cm);
            }
            return;
        }
        List<Move> moves = engine.legalMoves(state).stream()
                .filter(m -> m instanceof Move.CardMove cm && cm.targetCard().equals(card))
                .toList();
        if (moves.size() == 1) {
            applyMove(moves.get(0));
        } else if (moves.size() > 1) {
            showChooser(moves, card);
        }
    }

    /**
     * A stack of the board's own gold plates over the pressed card, one per
     * legal move. Every plate takes the widest label's width, measured once
     * here rather than per frame — a ragged stack reads as two unrelated
     * buttons instead of as a choice between two things.
     */
    private void showChooser(List<Move> moves, Card card) {
        chooserMoves = List.copyOf(moves);
        chooserCard = card;
        int widest = 0;
        for (Move move : chooserMoves) {
            widest = Math.max(widest, hud.labelWidth(chooserLabel(move)));
        }
        chooserPlateW = ChooserArt.plateW(widest);
    }

    /** Uppercase, like every other label the pixel board sets. */
    private static String chooserLabel(Move move) {
        return Labels.move(move).toUpperCase(Locale.ROOT);
    }

    /** The slot the chooser is anchored to, following its card if the room moves. */
    private int chooserSlotX() {
        int index = state.room().indexOf(chooserCard);
        return board.slotX(Math.max(0, index));
    }

    private void drawChooser() {
        if (chooserCard == null) {
            return;
        }
        int x = chooserSlotX();
        for (int i = 0; i < chooserMoves.size(); i++) {
            hud.drawPlate(batch, ChooserArt.plateX(x, chooserPlateW),
                    ChooserArt.plateY(i, chooserMoves.size()),
                    chooserPlateW, ChooserArt.PLATE_H,
                    chooserLabel(chooserMoves.get(i)), true);
        }
    }

    private void closeChooser() {
        chooserMoves = List.of();
        chooserCard = null;
    }

    /** Fresh run. The tutorial plays its scripted deck and records nothing. */
    private void startRun() {
        if (tutorial != null) {
            state = engine.newGame(TutorialScript.deck());
        } else {
            long seed = new Random().nextLong();
            state = engine.newGame(seed);
            recorder = new RunRecorder(seed, mode.id(), Clock.systemUTC());
            tracker = new AchievementTracker(rules.cardsResolvedPerTurn());
        }
        endBestLine = null;
        lastPotion = null;
        weaponBeforeMove = null;
        newlyUnlocked = List.of();
        barPhase = HealthReadout.Phase.REST;
        barChange = HealthReadout.Change.NONE;
    }

    /**
     * End of game: persist the run, then evaluate and persist achievements.
     * Each is independently guarded — neither storage step may break play, and
     * a failure in one must not stop the other.
     */
    private void finishRun() {
        RunRecord record;
        try {
            record = recorder.toRecord();
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "failed to build the run record", e);
            endBestLine = null;
            newlyUnlocked = List.of();
            return;
        }
        finalRunSeconds = record.seconds(); // the timer freezes here, exactly as recorded
        try {
            OptionalInt bestBefore = HighScores.bestForRuleset(runLog.readAll(), mode.id());
            runLog.append(record);
            endBestLine = bestBefore.isEmpty() || state.score() > bestBefore.getAsInt()
                    ? "New best!"
                    : "best " + bestBefore.getAsInt();
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "failed to record the run", e);
            endBestLine = null;
        }
        // Achievements are earned in ranked (Standard) mode only; variants still
        // recorded their run above. The history must include the run just
        // appended, so milestone achievements (finish N runs, defeat N monsters)
        // see this game.
        if (mode.tracksAchievements()) {
            try {
                RunSummary summary = tracker.toSummary(record.seconds());
                AchievementContext context = new AchievementContext(summary, runLog.readAll());
                newlyUnlocked = AchievementService.newlyEarned(
                        Achievements.all(), context, achievements.unlockedIds());
                for (Achievement earned : newlyUnlocked) {
                    achievements.append(new UnlockedAchievement(earned.id(), record.endedAt()));
                }
            } catch (RuntimeException e) {
                Gdx.app.error("scoundrel", "failed to evaluate achievements", e);
                newlyUnlocked = List.of();
            }
        }
    }

    private void applyMove(Move move) {
        if (tutorial != null && !tutorial.accepts(move)) {
            return; // only the current step's highlighted move is allowed
        }
        if (move instanceof Move.TakePotion potion) {
            lastPotion = potion.targetCard();
        }
        int healthBefore = state.health();
        List<Card> roomBefore = state.room();
        weaponBeforeMove = state.weapon();
        board.beginMove();

        MoveResult result = engine.apply(state, move);
        state = result.state();
        if (tutorial != null) {
            tutorial.onMoveApplied(move);
        } else {
            recorder.observe(result);
            tracker.observe(result);
            if (state.status() != Status.IN_PROGRESS) {
                finishRun();
            }
        }
        for (GameEvent event : result.events()) {
            String line = FeedText.line(event);
            if (line != null) {
                feed.push(line);
            }
        }

        boolean died = tutorial == null && state.status() == Status.LOST;
        board.setRoom(state.room());
        playEffect(move, result, roomBefore, died);
        pulseHealth(healthBefore, ResolveEffect.damageTaken(result.events()),
                ResolveEffect.healed(result.events()));

        if (died) {
            // Withhold the score: the cinematic runs over the dead board first.
            endPending = true;
            deathElapsed = 0f;
            killerSlotX = move instanceof Move.CardMove cm
                    ? orMinusOne(board.previousSlotX(cm.targetCard().id()))
                    : -1;
        } else {
            syncBoard();
        }
    }

    private static int orMinusOne(Integer value) {
        return value == null ? -1 : value;
    }

    /**
     * The effect is chosen purely by move type; the rest is the wiring. Every
     * one of them ends by dealing the room back in, so a refill follows without
     * being asked for.
     */
    private void playEffect(Move move, MoveResult result, List<Card> roomBefore, boolean fatal) {
        switch (ResolveEffect.of(move)) {
            case AVOID -> board.playSweep(roomBefore);
            case STRIKE -> board.playStrike(((Move.FightBarehanded) move).targetCard(), fatal);
            case SLICE -> board.playSlice(((Move.FightWithWeapon) move).targetCard(), fatal);
            case EQUIP -> board.playEquip(((Move.TakeWeapon) move).targetCard());
            case POTION -> {
                Card drunk = ((Move.TakePotion) move).targetCard();
                boolean wasted = result.events().stream()
                        .anyMatch(e -> e instanceof GameEvent.PotionWasted);
                // A wasted potion goes nowhere near the bar: it spills where it
                // stood. Sending it to a bar that then does not move read as the
                // heal being broken rather than as the potion being wasted.
                if (wasted) {
                    board.playSpill(drunk);
                } else {
                    board.playPotion(drunk, this::startHeal);
                }
            }
        }
    }

    /**
     * Damage drains the bar straight away — the blow has already landed. A
     * drink does not: the bar holds its old reading until {@link BoardView}
     * calls back to say the bottle has poured, so the fill has a visible cause
     * and is only ever shown once.
     */
    private void pulseHealth(int before, int damage, int healed) {
        barElapsed = 0f;
        if (damage > 0) {
            setBarChange(before, state.health());
            barPhase = HealthReadout.Phase.BLEEDING;
        } else if (healed > 0) {
            setBarChange(before, state.health());
            barPhase = HealthReadout.Phase.HELD;
        } else {
            barPhase = HealthReadout.Phase.REST;
        }
    }

    private void setBarChange(int from, int to) {
        barChange = new HealthReadout.Change(
                HudArt.barFillWidth(from, rules.healthCap()),
                HudArt.barFillWidth(to, rules.healthCap()), from, to);
    }

    /** The bottle has tipped: release the reading the bar has been holding. */
    private void startHeal() {
        barPhase = HealthReadout.Phase.HEALING;
        barElapsed = 0f;
    }
}
