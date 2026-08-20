package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * What every screen needs from the GL side: the Silkscreen faces at their five
 * sizes, the generated textures the backdrop and the dither draw from, and the
 * two accent colours the torch itself is lit in. The drawables it used to hold
 * went with Scene2D. Must be created and disposed on the GL thread.
 */
public final class Theme implements Disposable {

    // The two the torch itself needs, both on the accent ramp. Every other
    // colour a screen draws is named in the art classes beside what it draws.
    public static final Color TORCHLIGHT = Color.valueOf("d9a441");
    public static final Color BONE = Color.valueOf("e8ddc7");

    // Virtual resolution shared by every screen's PixelViewport.
    public static final float WORLD_WIDTH = 1280;
    public static final float WORLD_HEIGHT = 720;

    /** Characters beyond the freetype defaults used by the UI copy. */
    private static final String EXTRA_CHARS = "—–×•";

    // The detail shapes (impact star, battleaxe, flask, slice halves) are rasterised
    // at this multiple and drawn at their world size, so they stay crisp when upscaled.
    private static final int SHAPE_SUPERSAMPLE = 2;

    /** Silkscreen — the only face left, now that every screen is converted. */
    public final BitmapFont pixelSmall;
    public final BitmapFont pixelLabel;
    public final BitmapFont pixelBody;
    public final BitmapFont pixelTitle;
    public final BitmapFont pixelDisplay;
    /** Silkscreen Bold, for headings and emphasised numerals. */
    public final BitmapFont pixelLabelBold;
    public final BitmapFont pixelDisplayBold;

    private final Texture white;
    private final Texture glow;
    private final Texture vignette;
    private final Texture dot;
    private final Texture shade;
    private final Texture dither;
    private final Texture veil;
    private final TextureRegion ditherRegion;
    private final TextureRegion veilRegion;
    private final TextureRegion whiteRegion;
    private final TextureRegion glowRegion;
    private final TextureRegion vignetteRegion;
    private final TextureRegion dotRegion;
    private final TextureRegion shadeRegion;
    private final Map<Character, Texture> suitTextures = new HashMap<>();

    public Theme() {
        FreeTypeFontGenerator silk =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/Silkscreen-Regular.ttf"));
        FreeTypeFontGenerator silkBold =
                new FreeTypeFontGenerator(Gdx.files.internal("fonts/Silkscreen-Bold.ttf"));
        pixelSmall = generatePixel(silk, PixelType.SMALL);
        pixelLabel = generatePixel(silk, PixelType.LABEL);
        pixelBody = generatePixel(silk, PixelType.BODY);
        pixelTitle = generatePixel(silk, PixelType.TITLE);
        pixelDisplay = generatePixel(silk, PixelType.DISPLAY);
        pixelLabelBold = generatePixel(silkBold, PixelType.LABEL);
        pixelDisplayBold = generatePixel(silkBold, PixelType.DISPLAY);

        silk.dispose();
        silkBold.dispose();

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        white = new Texture(pixel);
        pixel.dispose();
        whiteRegion = new TextureRegion(white);
        dither = ditherTexture(DITHER_MODAL);
        ditherRegion = new TextureRegion(dither);
        veil = ditherTexture(DITHER_VEIL);
        veilRegion = new TextureRegion(veil);

        glow = radialGlowTexture(256);
        vignette = vignetteTexture(256);
        dot = softDotTexture(32);
        shade = verticalShadeTexture(64);
        // Detail shapes are rasterised at SHAPE_SUPERSAMPLE× and drawn at their world
        // size, so they stay crisp when upscaled (like the fonts). The suit pips are
        // already drawn tiny — downsampled and crisp — and the soft gradients (glow,
        // vignette, embers, shade) are low-frequency, so those need no supersample.
        glowRegion = new TextureRegion(glow);
        vignetteRegion = new TextureRegion(vignette);
        dotRegion = new TextureRegion(dot);
        shadeRegion = new TextureRegion(shade);

        suitTextures.put('S', suitTexture('S'));
        suitTextures.put('H', suitTexture('H'));
        suitTextures.put('D', suitTexture('D'));
        suitTextures.put('C', suitTexture('C'));
    }

    /** A single white pixel, for drawing tinted rectangles straight onto a Batch. */
    TextureRegion whiteRegion() {
        return whiteRegion;
    }

    /**
     * The dim a modal overlay puts the live screen under: a 4×4 ordered dither
     * at 82%, sized in tiles so one draw covers the stage.
     *
     * <p>Not an alpha scrim — HANDOFF §11 is explicit, and it is the same rule
     * the death wipe follows. A scrim blends every colour underneath toward
     * black and lands them off the eighty; the dither turns whole pixels off in
     * a fixed order and leaves the rest exactly as they were, so what shows
     * through is still palette-true and still legible.
     */
    TextureRegion ditherRegion(boolean heavy, int width, int height) {
        TextureRegion region = heavy ? ditherRegion : veilRegion;
        region.setRegion(0, 0, width / DITHER_TILE, height / DITHER_TILE);
        return region;
    }

    private static final int DITHER_TILE = 4;
    /**
     * Two strengths, because two things want dimming for different reasons.
     *
     * <p>{@code MODAL} is 13 of 16 cells — the 82% §11 names — for a dialog that
     * must be answered before anything behind it matters. {@code VEIL} is half
     * that, for the tutorial, where the board underneath is the thing you are
     * about to click and has to stay readable. §11 asks for 82% there too, but
     * its own render disagrees with it: sampled over a card, the reference has
     * <em>no</em> pure-black pixels at all and keeps roughly half the art.
     */
    private static final int DITHER_MODAL = 13;
    private static final int DITHER_VEIL = 8;
    private static final int[] BAYER = {
        0, 8, 2, 10,
        12, 4, 14, 6,
        3, 11, 1, 9,
        15, 7, 13, 5,
    };

    private static Texture ditherTexture(int level) {
        Pixmap pixmap = new Pixmap(DITHER_TILE, DITHER_TILE, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < DITHER_TILE; y++) {
            for (int x = 0; x < DITHER_TILE; x++) {
                // Opaque black or nothing at all. No partial alpha anywhere —
                // that is what makes it a pattern rather than a fade, and what
                // leaves every surviving pixel exactly on the palette.
                boolean dark = BAYER[y * DITHER_TILE + x] < level;
                pixmap.drawPixel(x, y, dark ? 0x000000ff : 0x00000000);
            }
        }
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        texture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();
        return texture;
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

    /**
     * Silkscreen, rasterised the way a pixel face has to be: at its exact size
     * with no supersampling, nearest-filtered, unhinted, un-gamma'd and with
     * anti-aliasing off entirely.
     *
     * <p>Every one of those is the opposite of what release 1 did to the vector
     * faces, and for good reason: those were rasterised at 3× and scaled back
     * down so they stayed smooth when the viewport upscaled them. Doing that to
     * Silkscreen would take a face that is already exactly right at 1:1 and blur
     * it. Same problem, opposite answer, because the art changed kind — and the
     * generator that did the 3× went with the faces themselves.
     */
    private static BitmapFont generatePixel(FreeTypeFontGenerator generator, int size) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS + EXTRA_CHARS;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;
        parameter.hinting = FreeTypeFontGenerator.Hinting.None;
        parameter.gamma = 1f;
        // The one that actually matters: FreeType renders a 1-bit glyph, so a
        // pixel is either on or off and no edge is ever blended.
        parameter.mono = true;
        BitmapFont font = generator.generateFont(parameter);
        // Whole positions only; a glyph on a half pixel is the same blur.
        font.setUseIntegerPositions(true);
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


    @Override
    public void dispose() {
        pixelSmall.dispose();
        pixelLabel.dispose();
        pixelBody.dispose();
        pixelTitle.dispose();
        pixelDisplay.dispose();
        pixelLabelBold.dispose();
        pixelDisplayBold.dispose();
        white.dispose();
        dither.dispose();
        veil.dispose();
        glow.dispose();
        vignette.dispose();
        dot.dispose();
        shade.dispose();
        suitTextures.values().forEach(Texture::dispose);
    }
}
