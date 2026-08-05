package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * The packed pixel art, loaded once and shared. Owns the atlas and
 * is the only thing that should: the source PNGs live outside the asset path
 * precisely so nothing can load a sprite as a loose file and get a smoothed
 * texture.
 *
 * <p>Region names are the art's contract — {@code creature_<value>_<name>_<suit>},
 * with idle frames adding {@code _idle_1}…{@code _idle_5} so
 * {@link #frames(String)} returns the five in order.
 */
public final class Sprites implements Disposable {

    /** Every sprite is 64×64, and only integer multiples of it may be drawn. */
    public static final int SIZE = 64;

    private final TextureAtlas atlas;

    public Sprites() {
        atlas = new TextureAtlas(Gdx.files.internal("sprites/sprites.atlas"));
        assertNearestFiltering();
    }

    /**
     * The whole art direction rests on nearest-neighbour sampling: these are
     * hand-placed pixels on a locked 80-colour ramp, and any smoothing invents
     * colours that are not in it. The atlas file carries {@code filter: Nearest,
     * Nearest} and libGDX applies it on load, but a wrong pack setting or a
     * texture swapped in later would only show up as art that looks slightly
     * soft — easy to blame on something else. Fail loudly instead.
     */
    private void assertNearestFiltering() {
        for (Texture texture : atlas.getTextures()) {
            if (texture.getMinFilter() != Texture.TextureFilter.Nearest
                    || texture.getMagFilter() != Texture.TextureFilter.Nearest) {
                throw new GdxRuntimeException("sprite atlas is not nearest-filtered ("
                        + texture.getMinFilter() + "/" + texture.getMagFilter()
                        + ") — pixel art would be blurred");
            }
        }
    }

    /** A single region by its exact name; fails loudly rather than drawing nothing. */
    public TextureRegion region(String name) {
        TextureRegion region = atlas.findRegion(name);
        if (region == null) {
            throw new GdxRuntimeException("no sprite region named '" + name + "'");
        }
        return region;
    }

    /**
     * The frames of an animation cycle, in index order — pass the stem without
     * the trailing index, e.g. {@code "creature_02_cellar_rat_clubs_idle"}.
     */
    public Array<TextureRegion> frames(String stem) {
        Array<TextureAtlas.AtlasRegion> found = atlas.findRegions(stem);
        if (found.size == 0) {
            throw new GdxRuntimeException("no sprite frames named '" + stem + "'");
        }
        return new Array<>(found);
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }
}
