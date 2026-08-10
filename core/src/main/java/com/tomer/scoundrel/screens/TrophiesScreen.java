package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.tomer.scoundrel.ScoundrelGame;
import com.tomer.scoundrel.achievements.Achievement;
import com.tomer.scoundrel.achievements.AchievementStore;
import com.tomer.scoundrel.achievements.Achievements;
import com.tomer.scoundrel.achievements.UnlockedAchievement;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TROPHIES — the whole achievement catalog as a book of deeds, read once from
 * the store on entry. Earned ones are lit and carry the day they were won;
 * still-locked ones show what to aim for, except the hidden ones, which stay
 * "???" until earned.
 *
 * <p>Drawn in immediate mode from the menu kit. What a row says is the pure
 * {@link TrophyEntry}; this class places the ten of them and the header's
 * progress bar.
 */
public final class TrophiesScreen extends ScreenAdapter {

    private final ScoundrelGame game;
    private final Theme theme;

    private final PixelViewport viewport;
    private final SpriteBatch batch = new SpriteBatch();
    private final PixelSurface surface;
    private final Backdrop backdrop;
    private final Chrome chrome;
    private final PressGesture press = new PressGesture();

    private final List<TrophyEntry> entries;
    /** Each entry's description, already broken into the lines its row holds. */
    private final List<List<String>> descriptions;
    private final int earned;

    public TrophiesScreen(ScoundrelGame game, Theme theme, AchievementStore store) {
        this.game = game;
        this.theme = theme;
        this.viewport = new PixelViewport(Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        this.surface = new PixelSurface((int) Theme.WORLD_WIDTH, (int) Theme.WORLD_HEIGHT);
        this.backdrop = new Backdrop(theme);
        this.chrome = new Chrome(theme);

        Map<String, Instant> unlocked = readSafely(store);
        List<TrophyEntry> built = new ArrayList<>();
        List<List<String>> wrapped = new ArrayList<>();
        for (Achievement achievement : Achievements.all()) {
            TrophyEntry entry = TrophyEntry.of(achievement, unlocked.get(achievement.id()));
            built.add(entry);
            // Wrapped once here rather than per frame: the measuring needs a
            // font, but the text never changes while the screen is up.
            wrapped.add(TextWrap.wrap(entry.description(), ScreenArt.trophyTextWidth(),
                    ScreenArt.TROPHY_DESC_LINES, s -> chrome.width(theme.pixelLabel, s)));
        }
        this.entries = List.copyOf(built);
        this.descriptions = List.copyOf(wrapped);
        this.earned = (int) entries.stream().filter(TrophyEntry::earned).count();
    }

    private static Map<String, Instant> readSafely(AchievementStore store) {
        try {
            Map<String, Instant> found = new HashMap<>();
            for (UnlockedAchievement achievement : store.readAll()) {
                found.put(achievement.id(), achievement.earnedAt());
            }
            return found;
        } catch (RuntimeException e) {
            Gdx.app.error("scoundrel", "failed to read achievements", e);
            return Map.of();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new TrophiesInput());
    }

    /** Only the header's back plate is a target; the rows are a reading. */
    private int hit(int screenX, int screenY) {
        Vector2 point = viewport.unproject(new Vector2(screenX, screenY));
        return ScreenArt.backContains(point.x, point.y) ? ScreenArt.BACK : PressGesture.NONE;
    }

    private final class TrophiesInput extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            return press.press(hit(screenX, screenY));
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            press.moveOver(hit(screenX, screenY));
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button != Input.Buttons.LEFT) {
                return false;
            }
            return press.release(hit(screenX, screenY));
        }

        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                game.showTitle();
                return true;
            }
            return false;
        }
    }

    @Override
    public void render(float delta) {
        backdrop.advance(delta);

        // Leaving disposes this screen along with its batch and surface, so
        // nothing may be drawn after the press acts.
        int fired = press.advance(delta);
        if (fired != PressGesture.NONE) {
            activate();
            if (game.getScreen() != this) {
                return;
            }
        }

        surface.begin(new Color((CardArt.BACKDROP << 8) | 0xff));
        batch.setProjectionMatrix(surface.projection());
        batch.begin();
        backdrop.render(batch, 1f);
        // The count goes beside the bar rather than into the header's own
        // caption slot, which the bar is standing in.
        chrome.header(batch, "TROPHIES", "", press.sunk() == ScreenArt.BACK);
        drawProgress();
        for (int i = 0; i < entries.size(); i++) {
            drawEntry(entries.get(i), i);
        }
        batch.end();
        surface.end();

        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        surface.draw(batch, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        batch.end();
    }

    private void activate() {
        game.showTitle();
    }

    /**
     * The header's progress bar, built exactly like the board's health bar: a
     * 2px frame, a three-band fill so it reads as lit rather than flat, and
     * separators laid <em>over</em> a continuous fill rather than one cell per
     * trophy — which is why a part-filled bar can end mid-segment.
     */
    private void drawProgress() {
        int x = ScreenArt.PROGRESS_X;
        int y = ScreenArt.PROGRESS_Y;
        int t = ScreenArt.THICK;
        int inX = x + t;
        int inY = y + t;
        int inW = ScreenArt.PROGRESS_W - 2 * t;
        int inH = ScreenArt.PROGRESS_H - 2 * t;

        chrome.face(batch, x, y, ScreenArt.PROGRESS_W, ScreenArt.PROGRESS_H, ScreenArt.FRAME);
        chrome.face(batch, inX, inY, inW, inH, ScreenArt.PROGRESS_EMPTY);

        int filled = ScreenArt.progressFillWidth(earned, entries.size());
        if (filled > 0) {
            int top = ScreenArt.PROGRESS_BAND_TOP;
            int mid = ScreenArt.PROGRESS_BAND_MID;
            chrome.face(batch, inX, inY, filled, top, ScreenArt.GOLD_LIGHT);
            chrome.face(batch, inX, inY + top, filled, mid, ScreenArt.GOLD);
            chrome.face(batch, inX, inY + top + mid, filled, inH - top - mid, ScreenArt.GOLD_DARK);
        }
        for (int sx = inX + ScreenArt.PROGRESS_SEGMENT - ScreenArt.PROGRESS_GAP;
                sx < inX + inW; sx += ScreenArt.PROGRESS_SEGMENT) {
            chrome.face(batch, sx, inY, ScreenArt.PROGRESS_GAP, inH,
                    ScreenArt.FRAME, ScreenArt.PROGRESS_SEGMENT_ALPHA);
        }

        chrome.textInRow(batch, theme.pixelLabel, earned + " OF " + entries.size() + " EARNED",
                x + ScreenArt.PROGRESS_W + 22, y, ScreenArt.PROGRESS_H,
                ScreenArt.HEADER_CAPTION, 1f);
    }

    /** One deed: its seal, its name, what it takes, and when it was won. */
    private void drawEntry(TrophyEntry entry, int index) {
        int x = ScreenArt.trophyX(index);
        int y = ScreenArt.trophyY(index);
        chrome.face(batch, x, y, ScreenArt.TROPHY_W, ScreenArt.TROPHY_H, entry.rowColour());
        chrome.face(batch, x + ScreenArt.SEAL_DX, y + ScreenArt.SEAL_DY,
                ScreenArt.SEAL_SIZE, ScreenArt.SEAL_SIZE, entry.sealColour());

        int textX = x + ScreenArt.TROPHY_TEXT_DX;
        chrome.text(batch, theme.pixelBody, entry.title(), textX,
                y + ScreenArt.TROPHY_TITLE_DY, entry.textColour());
        List<String> lines = descriptions.get(index);
        for (int line = 0; line < lines.size(); line++) {
            chrome.text(batch, theme.pixelLabel, lines.get(line), textX,
                    y + ScreenArt.TROPHY_DESC_DY + line * ScreenArt.TROPHY_LINE_H,
                    entry.earned() ? ScreenArt.BODY : ScreenArt.TROPHY_LOCKED_TEXT,
                    entry.earned() ? ScreenArt.BODY_ALPHA : 1f);
        }
        chrome.textRight(batch, theme.pixelLabel, entry.status(),
                x + ScreenArt.TROPHY_W - ScreenArt.TROPHY_STATUS_INSET,
                y + ScreenArt.TROPHY_TITLE_DY,
                entry.earned() ? ScreenArt.CELL_QUIET : ScreenArt.TROPHY_LOCKED_TEXT, 1f);
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        surface.dispose();
        batch.dispose();
    }
}
