package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;

import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;
import static com.tomer.scoundrel.screens.Widgets.mutedButton;
import static com.tomer.scoundrel.screens.Widgets.torchButton;

/**
 * The one in-game screen: draws the current GameState and (in later slices)
 * translates clicks into engine moves. Contains no rule logic — everything it
 * knows about the game comes from the state, the ruleset, and legalMoves().
 * The whole board is rebuilt from the state after every move.
 */
public final class GameScreen extends ScreenAdapter {

    private final ScoundrelGame game;
    private final Theme theme;
    private final GameMode mode;
    private final Ruleset rules;
    private final ScoundrelEngine engine;
    private final Stage stage;
    private final RunLog runLog;
    private final AchievementStore achievements;
    private final TutorialGuide tutorial; // null unless this is the guided tutorial
    private final Table root = new Table();
    private final VerticalGroup feed = new VerticalGroup();
    private final Choreographer choreographer;
    private final Map<Card, Table> roomTiles = new LinkedHashMap<>();
    private Actor tickerTicks;
    private Table hpBar;
    private Image hpFill;
    private Cell<Image> hpFillCell;
    private Label hpNumber;
    private TextButton avoidButton;
    private Table weaponMini;
    private GameState state;
    private RunRecorder recorder;
    private AchievementTracker tracker;
    private String endBestLine;
    private List<Achievement> newlyUnlocked = List.of();
    private Actor endOverlay;
    private Actor tutorialLayer;
    private boolean dealInPending;

    /** A normal run in the given mode, recorded to the run log. */
    public GameScreen(ScoundrelGame game, Theme theme, RunLog runLog,
                      AchievementStore achievements, GameMode mode) {
        this(game, theme, runLog, achievements, mode, null);
    }

    /** The guided tutorial: a scripted deck with narration, never recorded. */
    public GameScreen(ScoundrelGame game, Theme theme, GameMode mode, TutorialGuide tutorial) {
        this(game, theme, null, null, mode, tutorial);
    }

    private GameScreen(ScoundrelGame game, Theme theme, RunLog runLog,
                       AchievementStore achievements, GameMode mode, TutorialGuide tutorial) {
        this.game = game;
        this.theme = theme;
        this.runLog = runLog;
        this.achievements = achievements;
        this.mode = mode;
        this.tutorial = tutorial;
        this.rules = mode.ruleset();
        this.engine = new ScoundrelEngine(rules);
        this.stage = new Stage(new FitViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT));
        stage.addActor(new Backdrop(theme));
        this.choreographer = new Choreographer(stage, theme, this::resolveCardAt);
        startRun();

        root.setFillParent(true);
        stage.addActor(root);

        // The fading feed floats top-right, above the board and out of the
        // way of clicks; its lines outlive board rebuilds.
        feed.columnRight();
        feed.space(4);
        Table feedAnchor = new Table();
        feedAnchor.setFillParent(true);
        feedAnchor.top().right().padTop(96).padRight(28);
        feedAnchor.add(feed);
        feedAnchor.setTouchable(Touchable.disabled);
        stage.addActor(feedAnchor);

        rebuild();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Theme.SOOT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return; // minimized window
        }
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }

    /** Rebuilds the whole board from the current state. */
    private void rebuild() {
        if (endOverlay != null) {
            endOverlay.remove();
            endOverlay = null;
        }
        if (tutorialLayer != null) {
            tutorialLayer.remove();
            tutorialLayer = null;
        }
        root.clearChildren();
        root.top();
        root.add(topStrip()).growX().height(72).pad(12, 24, 0, 24);
        root.row();
        root.add(roomRow()).grow();
        root.row();
        root.add(bottomStrip()).growX().height(64).pad(0, 24, 12, 24);
        if (state.status() != Status.IN_PROGRESS) {
            endOverlay = tutorial != null ? buildTutorialComplete() : buildEndOverlay();
            stage.addActor(endOverlay);
        } else if (tutorial != null && !tutorial.isComplete()) {
            root.validate(); // real tile and button positions for the glow and callout
            tutorialLayer = buildTutorialLayer();
            stage.addActor(tutorialLayer);
        }
        if (dealInPending && state.status() == Status.IN_PROGRESS) {
            // The opening room has no previous slots, so every card flies in from
            // the dungeon (the depth ticker) — the same choreography as a refill.
            dealInPending = false;
            root.validate(); // force a real layout so the tile destinations are set
            choreographer.playDealIn(roomTiles, Map.of(), tickerCenter());
        }
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
        overlay.add(label("score " + state.score(), theme.display, Theme.BONE)).padBottom(8);
        overlay.row();
        if (endBestLine != null) {
            Color bestColor = endBestLine.equals("New best!")
                    ? Theme.TORCHLIGHT : dim(Theme.BONE, 0.6f);
            overlay.add(label(endBestLine, theme.bodyBold, bestColor)).padBottom(24);
            overlay.row();
        }
        if (!newlyUnlocked.isEmpty()) {
            overlay.add(unlockedBanner()).padBottom(24);
            overlay.row();
        }
        TextButton newGame = torchButton(theme, "New game");
        newGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startNewGame();
            }
        });
        overlay.add(newGame).padBottom(10);
        overlay.row();
        TextButton mainMenu = torchButton(theme, "Main menu");
        mainMenu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTitle();
            }
        });
        overlay.add(mainMenu).padBottom(10);
        overlay.row();
        TextButton trophies = torchButton(theme, "Trophies");
        trophies.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTrophies();
            }
        });
        overlay.add(trophies).padBottom(10);
        overlay.row();
        TextButton records = torchButton(theme, "Records");
        records.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showRecords();
            }
        });
        overlay.add(records);
        return overlay;
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
        choreographer.finish();
        startRun();
        feed.clearChildren();
        rebuild();
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

        Actor target = tutorialTarget(step);
        Vector2 targetCentre = null;
        float targetHeight = 0;
        if (target != null) {
            Vector2 corner = target.localToStageCoordinates(new Vector2(0, 0));
            float w = target.getWidth();
            float h = target.getHeight();
            targetCentre = new Vector2(corner.x + w / 2f, corner.y + h / 2f);
            targetHeight = h;
            layer.addActor(frameHighlight(corner.x, corner.y, w, h));
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
        skip.setPosition(Theme.WORLD_WIDTH - skip.getWidth() - 20, 90);
        layer.addActor(skip);
        return layer;
    }

    /** The board actor the current step points at: a room card, the Avoid button, or none. */
    private Actor tutorialTarget(TutorialStep step) {
        if (step.expectedMove() instanceof Move.CardMove cardMove) {
            return roomTiles.get(cardMove.targetCard());
        }
        if (step.expectedMove() instanceof Move.AvoidRoom) {
            return avoidButton;
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
                    rebuild();
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

    /** The tutorial's end: no score-keeping, just the way onward. */
    private Actor buildTutorialComplete() {
        Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setTouchable(Touchable.enabled);
        overlay.setBackground(theme.solid(dim(Theme.SOOT, 0.85f)));
        overlay.add(label("TUTORIAL COMPLETE", theme.title, Theme.TORCHLIGHT)).padBottom(10);
        overlay.row();
        Label blurb = label("You cleared it. A cleared dungeon scores your remaining health — "
                + "that is everything you need to know. Good luck down there.", theme.body, Theme.BONE);
        blurb.setWrap(true);
        blurb.setAlignment(com.badlogic.gdx.utils.Align.center);
        overlay.add(blurb).width(560).padBottom(26);
        overlay.row();
        TextButton play = torchButton(theme, "Play for real");
        play.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showModeSelect();
            }
        });
        overlay.add(play).padBottom(10);
        overlay.row();
        TextButton menu = torchButton(theme, "Main menu");
        menu.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showTitle();
            }
        });
        overlay.add(menu);
        return overlay;
    }

    // --- top strip: health, depth ticker, avoid ---

    private Actor topStrip() {
        Table strip = new Table();
        strip.add(healthGroup()).left();
        strip.add(depthTicker()).expandX();
        strip.add(avoidButton()).right();
        return strip;
    }

    private Actor healthGroup() {
        Table group = new Table();
        group.add(label("HP", theme.bodyBold, Theme.BONE)).padRight(8);
        group.add(healthBar()).width(160).height(14).padRight(8);
        hpNumber = label(String.valueOf(state.health()), theme.bodyBold, Theme.BONE);
        group.add(hpNumber);
        return group;
    }

    /** Bone at full health, charring toward dried blood as it drops. */
    private Actor healthBar() {
        float fraction = Math.max(0f, Math.min(1f, state.health() / (float) rules.healthCap()));
        Color fill = new Color(Theme.DRIED_BLOOD).lerp(Theme.BONE, fraction);
        hpBar = new Table();
        hpBar.setBackground(theme.solid(Theme.STONE));
        hpBar.left().pad(2);
        hpFill = new Image(theme.solid(fill));
        hpFillCell = hpBar.add(hpFill).width(156 * fraction).height(10);
        return hpBar;
    }

    /**
     * Paints the bar and number for an arbitrary health value — not necessarily
     * the current state's. Lets a potion heal hold the pre-heal reading during
     * the flask's flight and only tick up once it lands.
     */
    private void renderHealth(int value) {
        float fraction = Math.max(0f, Math.min(1f, value / (float) rules.healthCap()));
        hpNumber.setText(String.valueOf(value));
        hpFill.setDrawable(theme.solid(new Color(Theme.DRIED_BLOOD).lerp(Theme.BONE, fraction)));
        hpFillCell.width(156 * fraction);
        hpBar.invalidate();
    }

    /** Damage: the bar shudders and the number flashes dried blood. */
    private void pulseDamage() {
        hpBar.addAction(Actions.sequence(
                Actions.moveBy(5, 0, 0.04f),
                Actions.moveBy(-10, 0, 0.07f),
                Actions.moveBy(8, 0, 0.05f),
                Actions.moveBy(-3, 0, 0.04f)));
        hpNumber.addAction(Actions.sequence(
                Actions.color(new Color(Theme.DRIED_BLOOD), 0.05f),
                Actions.color(Color.WHITE, 0.5f)));
    }

    /** Healing: the fill glows back in and the number flashes herbal. */
    private void pulseHeal() {
        hpFill.getColor().a = 0.35f;
        hpFill.addAction(Actions.alpha(1f, 0.45f));
        hpNumber.addAction(Actions.sequence(
                Actions.color(new Color(Theme.HERBAL), 0.05f),
                Actions.color(Color.WHITE, 0.5f)));
    }

    /**
     * The depth ticker — one tick per card the dungeon started with; lit
     * ticks are what is still face-down below you. Avoided rooms visibly
     * return their ticks to the pile.
     */
    private Actor depthTicker() {
        Color consumed = Color.valueOf("3a3229");
        int remaining = state.dungeon().size();
        int total = rules.deck().cards().size();
        Table ticker = new Table();
        Table ticks = new Table();
        tickerTicks = ticks;
        for (int i = 0; i < total; i++) {
            Color c = i < remaining ? Theme.TORCHLIGHT : consumed;
            ticks.add(new Image(theme.solid(c))).width(4).height(16).padRight(2);
        }
        ticker.add(ticks);
        ticker.row();
        ticker.add(label("depth: " + remaining + " cards", theme.small, dim(Theme.BONE, 0.6f)))
                .padTop(4);
        return ticker;
    }

    private Actor avoidButton() {
        avoidButton = torchButton(theme, "Avoid");
        // In the tutorial, Avoid is live only on the step that teaches it.
        boolean enabled = tutorial != null
                ? tutorialExpects(new Move.AvoidRoom())
                : engine.legalMoves(state).contains(new Move.AvoidRoom());
        avoidButton.setDisabled(!enabled);
        avoidButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                applyMove(new Move.AvoidRoom());
            }
        });
        return avoidButton;
    }

    /** True when the tutorial's current step expects exactly this move. */
    private boolean tutorialExpects(Move move) {
        return tutorial != null && !tutorial.isComplete()
                && move.equals(tutorial.current().expectedMove());
    }

    // --- center: the room ---

    private Actor roomRow() {
        roomTiles.clear();
        Table row = new Table();
        for (Card card : state.room()) {
            row.add(cardTile(card)).size(Theme.CARD_WIDTH, Theme.CARD_HEIGHT).pad(12);
        }
        return row;
    }

    private Actor cardTile(Card card) {
        Table tile = CardTiles.build(theme, card);
        roomTiles.put(card, tile);
        // Press, not click: fast play releases the button mid-travel.
        tile.addListener(Widgets.pressListener(() -> onCardClicked(card, tile)));
        return tile;
    }

    /**
     * A click that skipped an animation also resolves the card it landed on,
     * so no click is ever spent purely on dismissing the motion. The board is
     * already settled and laid out by the time this runs.
     */
    private void resolveCardAt(float stageX, float stageY) {
        if (state.status() != Status.IN_PROGRESS) {
            return;
        }
        List<CardHitRegions.CardRect> rects = new ArrayList<>();
        roomTiles.forEach((card, tile) -> {
            Vector2 corner = tile.localToStageCoordinates(new Vector2(0, 0));
            rects.add(new CardHitRegions.CardRect(card, corner.x, corner.y,
                    tile.getWidth(), tile.getHeight()));
        });
        Card card = CardHitRegions.cardAt(rects, stageX, stageY);
        if (card != null) {
            onCardClicked(card, roomTiles.get(card));
        }
    }

    /** One legal move plays immediately; two or more open the chooser. */
    private void onCardClicked(Card card, Actor tile) {
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
            showChooser(moves, tile);
        }
    }

    /**
     * Button stack over the pressed card. A press outside it dismisses the
     * chooser AND resolves whatever card it landed on, so the press is never
     * spent merely closing the popup. The popup carries no padding, so its
     * whole area is button: a press inside it can neither fall through to the
     * catcher (which would just re-open the chooser) nor land on inert frame.
     */
    private void showChooser(List<Move> moves, Actor tile) {
        Group overlay = new Group();
        Actor catcher = new Actor();
        catcher.setBounds(0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        catcher.addListener(Widgets.pressListenerAt((stageX, stageY) -> {
            overlay.remove();
            resolveCardAt(stageX, stageY);
        }));
        overlay.addActor(catcher);

        Table popup = new Table();
        popup.setBackground(theme.solid(Theme.STONE));
        popup.pad(0);
        popup.defaults().growX().space(0);
        for (Move move : moves) {
            TextButton button = torchButton(theme, moveLabel(move));
            // Press, like the cards: the chooser sits on the hot path for every
            // armed monster, so it must not drop a fast click either.
            button.addListener(Widgets.pressListener(() -> {
                overlay.remove();
                applyMove(move);
            }));
            popup.add(button);
            popup.row();
        }
        popup.pack();
        Vector2 center = tile.localToStageCoordinates(
                new Vector2(tile.getWidth() / 2f, tile.getHeight() / 2f));
        popup.setPosition(center.x - popup.getWidth() / 2f, center.y - popup.getHeight() / 2f);
        overlay.addActor(popup);
        stage.addActor(overlay);
    }

    private static String moveLabel(Move move) {
        return switch (move) {
            case Move.FightWithWeapon ignored -> "Use weapon";
            case Move.FightBarehanded ignored -> "Barehanded";
            case Move.TakeWeapon ignored -> "Equip";
            case Move.TakePotion ignored -> "Drink";
            case Move.AvoidRoom ignored -> "Avoid";
        };
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
        newlyUnlocked = List.of();
        dealInPending = true; // the opening room deals in from the dungeon like any other
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
            String line = feedLine(event);
            if (line != null) {
                pushFeedLine(line);
            }
        }
        Map<String, Vector2> previousSlots = captureRoomSlots();
        rebuild();
        if (state.status() == Status.IN_PROGRESS) {
            int damage = result.events().stream()
                    .filter(e -> e instanceof GameEvent.MonsterDefeated)
                    .mapToInt(e -> ((GameEvent.MonsterDefeated) e).damageTaken())
                    .sum();
            int healed = result.events().stream()
                    .filter(e -> e instanceof GameEvent.PotionUsed)
                    .mapToInt(e -> ((GameEvent.PotionUsed) e).healed())
                    .sum();
            if (damage > 0) {
                pulseDamage();
            } else if (healed > 0) {
                // The heal — count, fill, and green flash — all land with the
                // potion, not on the click: hold the pre-heal reading during the
                // flight, then tick up when the flask spills onto the bar.
                int landed = state.health();
                renderHealth(landed - healed);
                hpBar.addAction(Actions.delay(Theme.POTION_FLIGHT, Actions.run(() -> {
                    renderHealth(landed);
                    pulseHeal();
                })));
            }
            List<Card> avoided = result.events().stream()
                    .filter(e -> e instanceof GameEvent.RoomAvoided)
                    .map(e -> ((GameEvent.RoomAvoided) e).room())
                    .findFirst().orElse(null);
            boolean roomDealt = result.events().stream()
                    .anyMatch(e -> e instanceof GameEvent.RoomDealt);
            // A monster taken bare-handed is struck in its slot; an equipped
            // weapon flies to the rail; a drunk potion flies to the HP bar. Each
            // resolves before any deal-in.
            Card struck = move instanceof Move.FightBarehanded fought ? fought.targetCard() : null;
            Vector2 struckSlot = struck != null ? previousSlots.get(struck.id()) : null;
            Card equipped = move instanceof Move.TakeWeapon taken ? taken.targetCard() : null;
            Vector2 equipSlot = equipped != null ? previousSlots.get(equipped.id()) : null;
            Card drunk = move instanceof Move.TakePotion sipped ? sipped.targetCard() : null;
            Vector2 drunkSlot = drunk != null ? previousSlots.get(drunk.id()) : null;
            boolean potionWasted = result.events().stream()
                    .anyMatch(e -> e instanceof GameEvent.PotionWasted);
            Card slain = move instanceof Move.FightWithWeapon slew ? slew.targetCard() : null;
            Vector2 slainSlot = slain != null ? previousSlots.get(slain.id()) : null;
            root.validate(); // force a fresh layout so tile destinations are real
            if (avoided != null) {
                choreographer.playAvoid(avoided, previousSlots, roomTiles, tickerCenter());
            } else if (struck != null && struckSlot != null) {
                choreographer.playBarehanded(struck, struckSlot, roomTiles, previousSlots,
                        tickerCenter(), roomDealt);
            } else if (equipped != null && equipSlot != null && weaponMini != null) {
                choreographer.playEquip(equipped, equipSlot, weaponMini, roomTiles, previousSlots,
                        tickerCenter(), roomDealt);
            } else if (drunk != null && drunkSlot != null) {
                choreographer.playPotion(drunk, drunkSlot, hpBar, potionWasted, roomTiles,
                        previousSlots, tickerCenter(), roomDealt);
            } else if (slain != null && slainSlot != null) {
                choreographer.playSlice(slainSlot, roomTiles, previousSlots,
                        tickerCenter(), roomDealt);
            } else if (roomDealt) {
                choreographer.playDealIn(roomTiles, previousSlots, tickerCenter());
            }
        }
    }

    /** Stage positions of the outgoing room tiles, keyed by card id. */
    private Map<String, Vector2> captureRoomSlots() {
        Map<String, Vector2> slots = new HashMap<>();
        roomTiles.forEach((card, tile) ->
                slots.put(card.id(), tile.localToStageCoordinates(new Vector2(0, 0))));
        return slots;
    }

    /** Where dealt cards fly from: the depth ticker is the dungeon. */
    private Vector2 tickerCenter() {
        return tickerTicks.localToStageCoordinates(
                new Vector2(tickerTicks.getWidth() / 2f, tickerTicks.getHeight() / 2f));
    }

    // --- the fading feed ---

    private void pushFeedLine(String text) {
        Label line = label(text, theme.body, dim(Theme.BONE, 0.9f));
        line.addAction(Actions.sequence(
                Actions.delay(4f), Actions.fadeOut(1.5f), Actions.removeActor()));
        feed.addActor(line);
        while (feed.getChildren().size > 4) {
            feed.removeActorAt(0, false);
        }
    }

    /** Events the player should read; null for ones the board already shows. */
    private static String feedLine(GameEvent event) {
        return switch (event) {
            case GameEvent.MonsterDefeated m -> {
                String name = cardName(m.monster());
                if (!m.withWeapon()) {
                    yield "Fought " + name + " barehanded — took " + m.damageTaken();
                }
                yield m.damageTaken() > 0
                        ? "Slew " + name + " — took " + m.damageTaken()
                        : "Slew " + name + " — unharmed";
            }
            case GameEvent.PotionUsed p -> p.healed() > 0
                    ? "Drank " + cardName(p.potion()) + " — healed " + p.healed()
                    : "Drank " + cardName(p.potion()) + " — already full";
            case GameEvent.PotionWasted p ->
                    cardName(p.potion()) + " wasted — one potion a turn";
            case GameEvent.WeaponEquipped w -> "Equipped " + cardName(w.weapon());
            case GameEvent.WeaponDegraded d -> d.newThreshold() <= 2
                    ? "The weapon is spent"
                    : "The weapon dulls — slays < " + d.newThreshold();
            case GameEvent.RoomAvoided ignored -> "Avoided the room";
            default -> null; // RoomDealt is visible on the board; win/loss get the overlay
        };
    }

    /** "the Queen of clubs", "the 7 of hearts" — the fonts have no suit glyphs. */
    private static String cardName(Card card) {
        String id = card.id();
        char suitChar = id.charAt(id.length() - 1);
        String suit = switch (suitChar) {
            case 'S' -> "spades";
            case 'H' -> "hearts";
            case 'D' -> "diamonds";
            case 'C' -> "clubs";
            default -> null;
        };
        if (suit == null || id.length() < 2) {
            return card.type().name().toLowerCase() + " " + card.value();
        }
        String rank = switch (id.substring(0, id.length() - 1)) {
            case "J" -> "Jack";
            case "Q" -> "Queen";
            case "K" -> "King";
            case "A" -> "Ace";
            default -> id.substring(0, id.length() - 1);
        };
        return "the " + rank + " of " + suit;
    }

    // --- bottom strip: trophy rail and potion marker ---

    private Actor bottomStrip() {
        Table strip = new Table();
        strip.add(trophyRail()).left();
        strip.add().expandX();
        strip.add(potionMarker()).right();
        return strip;
    }

    /** Equipped weapon, its slain stack, and the degradation plate. */
    private Actor trophyRail() {
        Table rail = new Table();
        weaponMini = null;
        EquippedWeapon weapon = state.weapon();
        if (weapon == null) {
            rail.add(label("Barehanded", theme.body, dim(Theme.BONE, 0.6f)));
            return rail;
        }
        // The weapon is a big battleaxe with its value stamped inside the blades —
        // no card frame; this is the shape the equip flight settles into. (Its
        // slain still read as card-panel chips below.)
        Image battleaxe = new Image(theme.axeRegion());
        battleaxe.setColor(Theme.IRON);
        Table axeLayer = new Table();
        axeLayer.add(battleaxe).size(72, 72);
        Table numLayer = new Table();
        numLayer.add(label(String.valueOf(weapon.weapon().value()), theme.bodyBold, Theme.SOOT))
                .expand().top().padTop(9);
        Table slot = new Table();
        slot.add(new Stack(axeLayer, numLayer)).size(72, 72);
        weaponMini = slot;
        rail.add(slot).size(72, 72).padRight(10);
        for (Card slain : weapon.slain()) {
            Table chip = new Table();
            chip.setBackground(theme.solid(Theme.CARD_MONSTER));
            chip.add(label(String.valueOf(slain.value()), theme.small, Theme.BONE));
            rail.add(chip).size(26, 36).padRight(4);
        }
        Table plate = new Table();
        plate.setBackground(theme.solid(Theme.TORCHLIGHT));
        plate.add(label(thresholdText(weapon), theme.bodyBold, Theme.SOOT)).pad(2, 10, 2, 10);
        rail.add(plate).padLeft(8);
        return rail;
    }

    private static String thresholdText(EquippedWeapon weapon) {
        if (weapon.threshold().isEmpty()) {
            return "slays anything";
        }
        int threshold = weapon.threshold().getAsInt();
        return threshold <= 2 ? "spent" : "slays < " + threshold;
    }

    private Actor potionMarker() {
        boolean used = state.potionsUsedThisRoom() >= rules.potionsPerTurn();
        return used
                ? label("• potion used this turn", theme.body, Theme.TORCHLIGHT)
                : label("potion ready", theme.body, dim(Theme.BONE, 0.4f));
    }

}
