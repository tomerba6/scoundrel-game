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

    // Virtual resolution shared by every screen's Fit viewport.
    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;

    // Motion tokens (seconds) — the art pass tunes these in one place. Kept
    // short because the input gate stays up for the whole deal (see Motion).
    public static final float DEAL_DURATION = 0.18f;
    public static final float DEAL_STAGGER = 0.04f;
    public static final float SWEEP_DURATION = 0.20f;

    // Card tile size (virtual pixels), shared by the board layout and flight proxies.
    public static final float CARD_WIDTH = 170;
    public static final float CARD_HEIGHT = 240;

    /** Characters beyond the freetype defaults used by the UI copy. */
    private static final String EXTRA_CHARS = "—–×•";

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
    private final TextureRegion glowRegion;
    private final TextureRegion vignetteRegion;
    private final TextureRegion dotRegion;
    private final TextureRegion shadeRegion;
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

        glow = radialGlowTexture(256);
        vignette = vignetteTexture(256);
        dot = softDotTexture(32);
        shade = verticalShadeTexture(64);
        glowRegion = new TextureRegion(glow);
        vignetteRegion = new TextureRegion(vignette);
        dotRegion = new TextureRegion(dot);
        shadeRegion = new TextureRegion(shade);

        suitTextures.put('S', suitTexture('S'));
        suitTextures.put('H', suitTexture('H'));
        suitTextures.put('D', suitTexture('D'));
        suitTextures.put('C', suitTexture('C'));
    }

    /** A flat rectangle of the given color, stretchable to any size. */
    public Drawable solid(Color color) {
        return new TextureRegionDrawable(new TextureRegion(white)).tint(color);
    }

    /** Soft warm radial glow (white; tinted at draw). Package-private — only the Backdrop uses it. */
    TextureRegion glowRegion() {
        return glowRegion;
    }

    /** Black edge-darkening vignette with its alpha baked in. */
    TextureRegion vignetteRegion() {
        return vignetteRegion;
    }

    /** A soft round mote (white; tinted at draw) for the drifting embers. */
    TextureRegion dotRegion() {
        return dotRegion;
    }

    /** A symmetric top-and-bottom edge shade (black, alpha baked in) for card panels. */
    TextureRegion shadeRegion() {
        return shadeRegion;
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
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(parameter);
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
        suitTextures.values().forEach(Texture::dispose);
    }
}
