package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;

/**
 * The dungeon's ambient light, drawn behind every screen: a warm torch glow
 * over a darkening vignette, both procedural textures from the {@link Theme}.
 * Added first to a stage so it sits under the UI; purely decorative and never a
 * hit target.
 */
final class Backdrop extends Actor {

    // The glow, centred a little above the world middle like an overhead torch.
    private static final float GLOW_SIZE = 1180f;
    private static final float GLOW_Y_OFFSET = 40f;
    private static final float GLOW_ALPHA = 0.16f;

    private final Theme theme;
    private final Embers embers = new Embers(1337);
    private float elapsed;

    Backdrop(Theme theme) {
        this.theme = theme;
        setBounds(0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        advance(delta);
        super.act(delta);
    }

    /** For screens that drive the backdrop themselves rather than via a stage. */
    void advance(float delta) {
        elapsed += delta;
        embers.update(delta);
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        // The actor's own alpha dims the living fire — the glow and embers — so a
        // death can snuff the torch (fade it to near zero) while the dark vignette
        // stays. Normally it is 1, so nothing changes.
        render(batch, getColor().a);
    }

    /**
     * Draws straight onto a batch, outside any stage — the board is drawn in
     * immediate mode and needs its backdrop under it, not over it.
     *
     * @param light how alive the fire is; 1 normally, near zero once it is snuffed
     */
    void render(Batch batch, float light) {
        float gx = (Theme.WORLD_WIDTH - GLOW_SIZE) / 2f;
        float gy = (Theme.WORLD_HEIGHT - GLOW_SIZE) / 2f + GLOW_Y_OFFSET;
        float glowAlpha = GLOW_ALPHA * TorchFlicker.intensityAt(elapsed) * light;
        batch.setColor(Theme.TORCHLIGHT.r, Theme.TORCHLIGHT.g, Theme.TORCHLIGHT.b, glowAlpha);
        batch.draw(theme.glowRegion(), gx, gy, GLOW_SIZE, GLOW_SIZE);

        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(theme.vignetteRegion(), 0, 0, Theme.WORLD_WIDTH, Theme.WORLD_HEIGHT);

        // Drifting embers, over the vignette so they still glow in the dark corners.
        for (Embers.Ember e : embers.particles()) {
            batch.setColor(Theme.TORCHLIGHT.r, Theme.TORCHLIGHT.g, Theme.TORCHLIGHT.b, e.alpha * light);
            batch.draw(theme.dotRegion(), e.x - e.size / 2f, e.y - e.size / 2f, e.size, e.size);
        }
        // Leave the batch colour as we found it for the actors drawn on top.
        batch.setColor(1f, 1f, 1f, 1f);
    }
}
