package com.tomer.scoundrel.screens;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

/** Tiny shared widget helpers for the screens package. */
final class Widgets {

    private Widgets() {
    }

    /**
     * Acts the instant the left button goes down. Scene2D's {@code
     * ClickListener} only fires when the release lands back on the same actor,
     * so a click made while the mouse is already travelling to the next card —
     * exactly what fast play looks like — is silently discarded. Cards use
     * this instead; buttons keep the conventional release semantics so a press
     * can still be cancelled by sliding off.
     */
    static InputListener pressListener(Runnable onPress) {
        return pressListenerAt((stageX, stageY) -> onPress.run());
    }

    /** Told where the press landed, in stage coordinates. */
    @FunctionalInterface
    interface PressAt {
        void at(float stageX, float stageY);
    }

    /** As {@link #pressListener}, for presses whose location matters. */
    static InputListener pressListenerAt(PressAt onPress) {
        return new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button != Input.Buttons.LEFT) {
                    return false;
                }
                onPress.at(event.getStageX(), event.getStageY());
                return true;
            }
        };
    }

    static Label label(String text, BitmapFont font, Color color) {
        return new Label(text, new Label.LabelStyle(font, color));
    }

    static Color dim(Color color, float alpha) {
        Color dimmed = new Color(color);
        dimmed.a = alpha;
        return dimmed;
    }

    /** The one button style: torchlight when enabled, stone when not. */
    static TextButton torchButton(Theme theme, String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = theme.bodyBold;
        style.up = theme.solid(Theme.TORCHLIGHT);
        style.fontColor = Theme.SOOT;
        style.disabled = theme.solid(Theme.STONE);
        style.disabledFontColor = dim(Theme.BONE, 0.35f);
        TextButton button = new TextButton(text, style);
        button.pad(6, 20, 6, 20);
        return button;
    }

    /** A quiet, low-emphasis button — stone with dim text; won't invite a stray click. */
    static TextButton mutedButton(Theme theme, String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = theme.bodyBold;
        style.up = theme.solid(Theme.STONE);
        style.fontColor = dim(Theme.BONE, 0.55f);
        style.disabled = theme.solid(Theme.STONE);
        style.disabledFontColor = dim(Theme.BONE, 0.2f);
        TextButton button = new TextButton(text, style);
        button.pad(6, 20, 6, 20);
        return button;
    }
}
