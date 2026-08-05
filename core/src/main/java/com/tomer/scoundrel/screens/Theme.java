package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The torchlit-dungeon look of the plain UI: palette, fonts, and flat
 * drawables. Every visual decision derives from here so the later sprite
 * pass swaps assets, not screen code. Must be created and disposed on the
 * GL thread.
 */
public final class Theme implements Disposable {

    // Palette (see docs: torchlit dungeon mood).
    public static final Color SOOT = Color.valueOf("17130f");
    public static final Color STONE = Color.valueOf("241d16");
    public static final Color DRIED_BLOOD = Color.valueOf("8c2f22");
    public static final Color IRON = Color.valueOf("7a8794");
    public static final Color HERBAL = Color.valueOf("5d8a4a");
    public static final Color TORCHLIGHT = Color.valueOf("d9a441");
    public static final Color BONE = Color.valueOf("e8ddc7");

    // Card panels ("Ashen"): deep and low-lit so the board sits inside the
    // torchlit mood rather than shouting over it. Type still reads from the
    // label and suit pips, so the quiet colours never cost legibility.
    public static final Color CARD_MONSTER = Color.valueOf("4e2620");
    public static final Color CARD_WEAPON = Color.valueOf("3f484e");
    public static final Color CARD_POTION = Color.valueOf("374b32");

    // Virtual resolution shared by every screen's Fit viewport.
    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;

    // Motion tokens (seconds) — the art pass tunes these in one place. Kept
    // short because the input gate stays up for the whole deal (see Motion).
    public static final float DEAL_DURATION = 0.18f;
    public static final float DEAL_STAGGER = 0.04f;
    public static final float SWEEP_DURATION = 0.20f;

    // A bare-handed monster is struck this many times; each blow takes
    // STRIKE_HIT_DURATION to flare and fade, the next follows one stagger later.
    public static final int STRIKE_HITS = 2;
    public static final float STRIKE_HIT_DURATION = 0.16f;
    public static final float STRIKE_HIT_STAGGER = 0.10f;

    // An equipped weapon flies from its card slot into the trophy rail over this
    // long, shrinking into and morphing toward the rail's axe mini as it goes.
    public static final float EQUIP_FLIGHT = 0.24f;

    // A drunk potion's card shrinks into a flask and flies up to the health bar
    // over this long, spilling a few drops as it lands (a wasted one just fizzles).
    public static final float POTION_FLIGHT = 0.3f;

    // A weapon kill cleaves the monster's card in two along a curved diagonal; the
    // halves lift and slide apart over this long before the next card deals in.
    public static final float SLICE_DURATION = 0.36f;

    // Death cinematic beats (seconds): the fatal blow flares and shakes, then the
    // screen bleeds dark, then YOU DIED fades and grows, holds, and the score +
    // buttons settle in beneath it.
    public static final float DEATH_BLOW = 0.4f;
    public static final float DEATH_DIM = 0.8f;
    public static final float DEATH_REVEAL = 1.2f;
    public static final float DEATH_HOLD = 0.6f;
    public static final float DEATH_SETTLE = 0.5f;

    // Card tile size (virtual pixels), shared by the board layout and flight proxies.
    public static final float CARD_WIDTH = 170;
    public static final float CARD_HEIGHT = 240;

    /** Characters beyond the freetype defaults used by the UI copy. */
    private static final String EXTRA_CHARS = "—–×•";

    // Fonts are rasterised at this multiple of their design size, then scaled back
    // down to world units — a high-res glyph atlas that stays crisp when the
    // PixelViewport upscales the 720p design to a larger screen (crisp up to ~4K).
    private static final int FONT_SUPERSAMPLE = 3;
    // The detail shapes (impact star, battleaxe, flask, slice halves) are rasterised
    // at this multiple and drawn at their world size, so they stay crisp when upscaled.
    private static final int SHAPE_SUPERSAMPLE = 2;

    /** IM Fell English — card values and other large set pieces. */
    public final BitmapFont display;
    /** IM Fell English — overlay titles and the wordmark. */
    public final BitmapFont title;
    /** Alegreya Sans — HUD labels, buttons, feed lines. */
    public final BitmapFont body;
    /** Alegreya Sans Bold — numbers and emphasized labels. */
    public final BitmapFont bodyBold;
    /** Alegreya Sans — the smallest text: feed detail, card corners. */
    public final BitmapFont small;

    private final Texture white;
    private final Texture glow;
    private final Texture vignette;
    private final Texture dot;
    private final Texture shade;
    private final Texture burst;
    private final Texture axe;
    private final Texture flask;
    private final Texture sliceUpper;
    private final Texture sliceLower;
    private final TextureRegion whiteRegion;
    private final TextureRegion glowRegion;
    private final TextureRegion vignetteRegion;
    private final TextureRegion dotRegion;
    private final TextureRegion shadeRegion;
    private final TextureRegion burstRegion;
    private final TextureRegion axeRegion;
    private final TextureRegion flaskRegion;
    private final TextureRegion sliceUpperRegion;
    private final TextureRegion sliceLowerRegion;
    private final Map<Character, Texture> suitTextures = new HashMap<>();

    public Theme() {
        FreeTypeFontGenerator fell =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/IMFellEnglish-Regular.ttf"));
        FreeTypeFontGenerator sans =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/AlegreyaSans-Regular.ttf"));
        FreeTypeFontGenerator sansBold =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/AlegreyaSans-Bold.ttf"));
        display = generate(fell, 64);
        title = generate(fell, 42);
        body = generate(sans, 18);
        bodyBold = generate(sansBold, 18);
        small = generate(sans, 14);
        fell.dispose();
        sans.dispose();
        sansBold.dispose();

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        white = new Texture(pixel);
        pixel.dispose();
        whiteRegion = new TextureRegion(white);

        glow = radialGlowTexture(256);
        vignette = vignetteTexture(256);
        dot = softDotTexture(32);
        shade = verticalShadeTexture(64);
        // Detail shapes are rasterised at SHAPE_SUPERSAMPLE× and drawn at their world
        // size, so they stay crisp when upscaled (like the fonts). The suit pips are
        // already drawn tiny — downsampled and crisp — and the soft gradients (glow,
        // vignette, embers, shade) are low-frequency, so those need no supersample.
        burst = burstTexture(64 * SHAPE_SUPERSAMPLE);
        axe = axeTexture(64 * SHAPE_SUPERSAMPLE);
        flask = flaskTexture(64 * SHAPE_SUPERSAMPLE);
        sliceUpper = sliceHalfTexture((int) (CARD_WIDTH * SHAPE_SUPERSAMPLE),
                (int) (CARD_HEIGHT * SHAPE_SUPERSAMPLE), true);
        sliceLower = sliceHalfTexture((int) (CARD_WIDTH * SHAPE_SUPERSAMPLE),
                (int) (CARD_HEIGHT * SHAPE_SUPERSAMPLE), false);
        glowRegion = new TextureRegion(glow);
        vignetteRegion = new TextureRegion(vignette);
        dotRegion = new TextureRegion(dot);
        shadeRegion = new TextureRegion(shade);
        burstRegion = new TextureRegion(burst);
        axeRegion = new TextureRegion(axe);
        flaskRegion = new TextureRegion(flask);
        sliceUpperRegion = new TextureRegion(sliceUpper);
        sliceLowerRegion = new TextureRegion(sliceLower);

        suitTextures.put('S', suitTexture('S'));
        suitTextures.put('H', suitTexture('H'));
        suitTextures.put('D', suitTexture('D'));
        suitTextures.put('C', suitTexture('C'));
    }

    /** A flat rectangle of the given color, stretchable to any size. */
    public Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(white)).tint(color);
    }

    /** A single white pixel, for drawing tinted rectangles straight onto a Batch. */
    TextureRegion whiteRegion() {
        return whiteRegion;
    }

    /** Soft warm radial glow (white; tinted at draw). Package-private — only the Backdrop uses it. */
    TextureRegion glowRegion() {
        return glowRegion;
    }

    /** Black edge-darkening vignette with its alpha baked in. */
    TextureRegion vignetteRegion() {
        return vignetteRegion;
    }

    /** The edge vignette as a tinted drawable — blood-red for the death bleed-out. */
    Drawable vignette(Color tint) {
        return new TextureRegionDrawable(new TextureRegion(vignetteRegion)).tint(tint);
    }

    /** A soft round mote (white; tinted at draw) for the drifting embers. */
    TextureRegion dotRegion() {
        return dotRegion;
    }

    /** A symmetric top-and-bottom edge shade (black, alpha baked in) for card panels. */
    TextureRegion shadeRegion() {
        return shadeRegion;
    }

    /** A spiky impact star (white; tinted at draw) flared when a monster is struck. */
    TextureRegion burstRegion() {
        return burstRegion;
    }

    /** A single-bit axe (white; tinted at draw) for the equipped-weapon flight and rail. */
    TextureRegion axeRegion() {
        return axeRegion;
    }

    /** A round-bodied potion flask (white; tinted at draw) for the drink flight. */
    TextureRegion flaskRegion() {
        return flaskRegion;
    }

    /** The upper-left half of a monster card cleaved along a curved TR→BL diagonal. */
    TextureRegion sliceUpperRegion() {
        return sliceUpperRegion;
    }

    /** The lower-right half of a monster card cleaved along a curved TR→BL diagonal. */
    TextureRegion sliceLowerRegion() {
        return sliceLowerRegion;
    }

    /** Suit shape for a card id's suit letter (S/H/D/C), tinted. */
    public Drawable suitIcon(char suitLetter, Color tint) {
        Texture texture = suitTextures.get(suitLetter);
        if (texture == null) {
            throw new IllegalArgumentException("Unknown suit letter: " + suitLetter);
        }
        return new TextureRegionDrawable(new TextureRegion(texture)).tint(tint);
    }

    // A gradient stored in 8-bit alpha bands into visible contours when stretched
    // this large; ±1 LSB of dither noise breaks the flat steps into a smooth ramp.
    private static final float DITHER = 2.5f / 255f;

    /**
     * A soft warm light: white with alpha falling off from the centre (squared,
     * for a quick soft edge). Drawn large and tinted torchlight behind the board.
     */
    private static Texture radialGlowTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        Random rng = new Random(1);
        float c = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - c) / c;
                float dy = (y - c) / c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                p.setColor(1f, 1f, 1f, dither(a * a, rng));
                p.drawPixel(x, y);
            }
        }
        return linearTexture(p);
    }

    /**
     * Edge darkening: transparent through the middle, ramping to a soft black
     * toward the corners. Stretched over the whole world; the square-to-wide
     * stretch turns the radius into a pleasing widescreen ellipse.
     */
    private static Texture vignetteTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        Random rng = new Random(2);
        float c = size / 2f;
        float corner = (float) Math.sqrt(2);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - c) / c;
                float dy = (y - c) / c;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / corner;
                p.setColor(0f, 0f, 0f, dither(smoothstep(0.55f, 1f, d) * 0.6f, rng));
                p.drawPixel(x, y);
            }
        }
        return linearTexture(p);
    }

    private static float dither(float a, Random rng) {
        return Math.max(0f, Math.min(1f, a + (rng.nextFloat() - 0.5f) * DITHER));
    }

    /** A soft round mote: white with a gentle alpha falloff from the centre. */
    private static Texture softDotTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        float c = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - c) / c;
                float dy = (y - c) / c;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - d);
                p.setColor(1f, 1f, 1f, (float) Math.pow(a, 1.5));
                p.drawPixel(x, y);
            }
        }
        return linearTexture(p);
    }

    /**
     * A one-pixel-wide vertical strip that darkens toward the top and bottom
     * edges, clear through the middle — stretched over a card panel it reads as
     * soft, orientation-independent lighting under the frame.
     */
    private static Texture verticalShadeTexture(int height) {
        Pixmap p = new Pixmap(1, height, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < height; y++) {
            float t = y / (float) (height - 1);      // 0..1 across the strip
            float edge = Math.abs(t - 0.5f) * 2f;    // 0 in the middle, 1 at both ends
            p.setColor(0f, 0f, 0f, smoothstep(0.45f, 1f, edge) * 0.28f);
            p.drawPixel(0, y);
        }
        return linearTexture(p);
    }

    private static Texture linearTexture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0f, Math.min(1f, (x - edge0) / (edge1 - edge0)));
        return t * t * (3f - 2f * t);
    }

    private static BitmapFont generate(FreeTypeFontGenerator generator, int size) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size * FONT_SUPERSAMPLE;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        // Scale back to the design size (so layout is unchanged) while keeping the
        // high-res atlas; fractional positions let the downscaled glyphs stay smooth.
        font.getData().setScale(1f / FONT_SUPERSAMPLE);
        font.setUseIntegerPositions(false);
        return font;
    }

    /**
     * The bundled fonts have no suit glyphs, so the four suits are drawn as
     * simple shapes on a 64px canvas (white, tinted at use).
     */
    private static Texture suitTexture(char suitLetter) {
        Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        switch (suitLetter) {
            case 'H' -> { // two lobes over a point
                p.fillCircle(19, 20, 14);
                p.fillCircle(45, 20, 14);
                p.fillTriangle(5, 26, 59, 26, 32, 60);
            }
            case 'D' -> {
                p.fillTriangle(32, 2, 6, 32, 58, 32);
                p.fillTriangle(6, 32, 58, 32, 32, 62);
            }
            case 'S' -> { // inverted heart plus a flared stem
                p.fillTriangle(32, 2, 4, 34, 60, 34);
                p.fillCircle(18, 38, 14);
                p.fillCircle(46, 38, 14);
                p.fillTriangle(32, 42, 22, 62, 42, 62);
            }
            case 'C' -> { // three lobes plus a flared stem
                p.fillCircle(32, 16, 13);
                p.fillCircle(18, 36, 13);
                p.fillCircle(46, 36, 13);
                p.fillTriangle(32, 40, 22, 62, 42, 62);
            }
            default -> throw new IllegalArgumentException("Unknown suit letter: " + suitLetter);
        }
        Texture texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return texture;
    }

    /**
     * A spiky impact star: eight points, drawn as triangles fanned from the
     * centre out to alternating far/near vertices. White, tinted where used.
     */
    private static Texture burstTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        int c = size / 2;
        float outer = size / 2f - 1f;
        float inner = size / 6f;
        int spikes = 8;
        int points = spikes * 2;
        for (int i = 0; i < points; i++) {
            double a0 = Math.PI * i / spikes;
            double a1 = Math.PI * (i + 1) / spikes;
            float r0 = (i % 2 == 0) ? outer : inner;
            float r1 = (i % 2 == 0) ? inner : outer;
            p.fillTriangle(c, c,
                    c + Math.round(r0 * (float) Math.cos(a0)),
                    c + Math.round(r0 * (float) Math.sin(a0)),
                    c + Math.round(r1 * (float) Math.cos(a1)),
                    c + Math.round(r1 * (float) Math.sin(a1)));
        }
        Texture texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return texture;
    }

    /**
     * A big double-bit battleaxe: a broad twin-bladed head riding the TOP of a
     * central haft, its cutting edges flaring out to either side. The head is
     * tall and solid through the middle so the weapon's value sits inside the
     * blades, clear of the shaft. Drawn bold so it reads even shrunk into the
     * trophy rail. White on a transparent canvas, tinted where used.
     */
    private static Texture axeTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        int c = size / 2;
        // Haft: a stout bar down the centre, dropping from under the head. It
        // stops below the horns so the head's throat (the notch between the two
        // bits) reads at the top.
        p.fillRectangle(c - Math.round(size * 0.05f), Math.round(size * 0.18f),
                Math.round(size * 0.10f), Math.round(size * 0.76f));
        axeBlade(p, c, -1, size); // left bit
        axeBlade(p, c, +1, size); // right bit, mirrored
        Texture texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return texture;
    }

    /**
     * One bit of the battleaxe head. The blade attaches to the haft over a middle
     * band (solid, behind the value) but its horn and beard pull away and up/down,
     * so above and below that band the haft shows through — the throat that makes
     * two bits read instead of one block. {@code dir} is its side.
     */
    private static void axeBlade(Pixmap p, int c, int dir, int size) {
        float s = size;
        int attachTopX = c, attachTopY = Math.round(s * 0.20f);
        int attachBotX = c, attachBotY = Math.round(s * 0.38f);
        int hornX = c + dir * Math.round(s * 0.30f), hornY = Math.round(s * 0.06f);
        int edgeUpX = c + dir * Math.round(s * 0.42f), edgeUpY = Math.round(s * 0.17f);
        int edgeMidX = c + dir * Math.round(s * 0.46f), edgeMidY = Math.round(s * 0.29f);
        int edgeLowX = c + dir * Math.round(s * 0.42f), edgeLowY = Math.round(s * 0.41f);
        int beardX = c + dir * Math.round(s * 0.30f), beardY = Math.round(s * 0.52f);
        p.fillTriangle(attachTopX, attachTopY, hornX, hornY, edgeUpX, edgeUpY);
        p.fillTriangle(attachTopX, attachTopY, edgeUpX, edgeUpY, edgeMidX, edgeMidY);
        p.fillTriangle(attachTopX, attachTopY, edgeMidX, edgeMidY, attachBotX, attachBotY);
        p.fillTriangle(attachBotX, attachBotY, edgeMidX, edgeMidY, edgeLowX, edgeLowY);
        p.fillTriangle(attachBotX, attachBotY, edgeLowX, edgeLowY, beardX, beardY);
    }

    /**
     * A round-bodied potion flask: a rim, a slim neck, cone shoulders, and a
     * bulbous body. White on a transparent canvas, tinted where used.
     */
    private static Texture flaskTexture(int size) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        int c = size / 2;
        float s = size;
        p.fillRectangle(c - Math.round(s * 0.11f), Math.round(s * 0.08f),
                Math.round(s * 0.22f), Math.round(s * 0.07f));   // rim
        p.fillRectangle(c - Math.round(s * 0.08f), Math.round(s * 0.13f),
                Math.round(s * 0.16f), Math.round(s * 0.24f));   // neck
        p.fillTriangle(c - Math.round(s * 0.26f), Math.round(s * 0.66f),
                c + Math.round(s * 0.26f), Math.round(s * 0.66f),
                c, Math.round(s * 0.34f));                       // shoulders
        p.fillCircle(c, Math.round(s * 0.70f), Math.round(s * 0.26f)); // body
        Texture texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return texture;
    }

    /**
     * One half of a cleaved monster card. The cut runs from the top-right corner
     * to the bottom-left as a straight diagonal plus a gentle sine bulge (zero at
     * both corners, widest in the middle) — a slightly curved slash. Pixels on the
     * chosen side get the monster panel, its dark border on the original card
     * edges, and a bright seared line right along the cut; the rest is transparent.
     */
    private static Texture sliceHalfTexture(int w, int h, boolean upper) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setBlending(Pixmap.Blending.None);
        Color frame = new Color(CARD_MONSTER.r * 0.45f, CARD_MONSTER.g * 0.45f,
                CARD_MONSTER.b * 0.45f, 1f);
        float bulge = Math.min(w, h) * 0.14f;
        float[] boundary = new float[w];
        for (int x = 0; x < w; x++) {
            boundary[x] = h * (1f - x / (float) w) - bulge * (float) Math.sin(Math.PI * x / w);
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean isUpper = y < boundary[x];
                if (isUpper != upper) {
                    continue; // the other half — leave it transparent
                }
                Color c;
                if (Math.abs(y - boundary[x]) < 2.5f) {
                    c = BONE;                 // the seared cut edge
                } else if (x < 3 || x >= w - 3 || y < 3 || y >= h - 3) {
                    c = frame;                // the card's dark border
                } else {
                    c = CARD_MONSTER;         // the monster panel
                }
                p.setColor(c);
                p.drawPixel(x, y);
            }
        }
        Texture texture = new Texture(p);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        p.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        display.dispose();
        title.dispose();
        body.dispose();
        bodyBold.dispose();
        small.dispose();
        white.dispose();
        glow.dispose();
        vignette.dispose();
        dot.dispose();
        shade.dispose();
        burst.dispose();
        axe.dispose();
        flask.dispose();
        sliceUpper.dispose();
        sliceLower.dispose();
        suitTextures.values().forEach(Texture::dispose);
    }
}
