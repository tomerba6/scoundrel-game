package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
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
import com.tomer.scoundrel.rules.GameModes;
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
    private final Backdrop backdrop;
    private final BoardView board;
    private final BoardHud hud;
    private final Sprites sprites;
    private final Feed feed = new Feed();
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
    private Actor chooser;
    /** The last potion drunk, which is what the marker shows once one has been. */
    private Card lastPotion;

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
            if (button != Input.Buttons.LEFT || state.status() != Status.IN_PROGRESS) {
                return false;
            }
            Vector2 point = viewport.unproject(new Vector2(screenX, screenY));
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
        ScreenUtils.clear(BACKDROP);
        backdrop.advance(delta);
        board.update(delta);
        feed.update(delta);
        board.setHovered(hoveredCard());

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        backdrop.render(batch, 1f);
        drawHud();
        board.draw(batch);
        drawFeed();
        batch.end();

        stage.act(delta);
        stage.draw();
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
        hud.drawHealth(batch, state.health(), rules.healthCap(), false);
        int depth = state.dungeon().size();
        hud.drawTicker(batch, depth, rules.deck().cards().size());
        hud.drawDepthLine(batch, depth, rules.deck().cards().size(),
                tutorial == null ? ClockText.format(currentRunSeconds()) : null);
        hud.drawAvoid(batch, avoidAllowed());
        drawRail();
        hud.drawPotionMarker(batch, potionMarkerRegion(),
                state.potionsUsedThisRoom() >= rules.potionsPerTurn());
    }

    /** Equipped weapon, its slain stack, and how much bite it has left. */
    private void drawRail() {
        EquippedWeapon weapon = state.weapon();
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
        batch.dispose();
    }

    /**
     * Pushes the current state onto the board and rebuilds whichever overlay
     * belongs on top of it. Called after every move; the board itself is drawn
     * from the state each frame, so there is nothing else to rebuild.
     */
    private void syncBoard() {
        board.setRoom(state.room());
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
        if (chooser != null) {
            return; // the chooser's own catcher handles this press
        }
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
     * Button stack over the pressed card. A press outside it dismisses the
     * chooser AND resolves whatever card it landed on, so the press is never
     * spent merely closing the popup. The popup carries no padding, so its
     * whole area is button: a press inside it can neither fall through to the
     * catcher (which would just re-open the chooser) nor land on inert frame.
     */
    private void showChooser(List<Move> moves, Card card) {
        Group overlay = new Group();
        Actor catcher = new Actor();
        catcher.setBounds(0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        catcher.addListener(Widgets.pressListenerAt((stageX, stageY) -> {
            closeChooser();
            Card landed = board.cardAt(stageX, stageY);
            if (landed != null) {
                onCardClicked(landed);
            }
        }));
        overlay.addActor(catcher);

        Table popup = new Table();
        popup.setBackground(theme.solid(Theme.STONE));
        popup.pad(0);
        popup.defaults().growX().space(0);
        for (Move move : moves) {
            TextButton button = torchButton(theme, Labels.move(move));
            // Press, like the cards: the chooser sits on the hot path for every
            // armed monster, so it must not drop a fast click either.
            button.addListener(Widgets.pressListener(() -> {
                closeChooser();
                applyMove(move);
            }));
            popup.add(button);
            popup.row();
        }
        popup.pack();
        int index = state.room().indexOf(card);
        float centreX = board.slotX(Math.max(0, index)) + CardArt.CARD_W / 2f;
        float centreY = CardArt.toWorldY(CardArt.SLOT_Y, CardArt.CARD_H) + CardArt.CARD_H / 2f;
        popup.setPosition(centreX - popup.getWidth() / 2f, centreY - popup.getHeight() / 2f);
        overlay.addActor(popup);
        stage.addActor(overlay);
        chooser = overlay;
    }

    private void closeChooser() {
        if (chooser != null) {
            chooser.remove();
            chooser = null;
        }
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
        newlyUnlocked = List.of();
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
        syncBoard();
    }
}
