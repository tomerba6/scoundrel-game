package com.tomer.scoundrel.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tomer.scoundrel.model.Card;
import com.tomer.scoundrel.model.CardType;

import static com.tomer.scoundrel.screens.Widgets.dim;
import static com.tomer.scoundrel.screens.Widgets.label;

/**
 * Builds the typed card tiles. One builder feeds both the interactive board
 * tiles and the Choreographer's cosmetic flight proxies, so they can never
 * drift apart visually.
 */
final class CardTiles {

    /** Frame thickness (virtual px) between a card's dark border and its panel. */
    private static final int BORDER = 3;

    private CardTiles() {
    }

    static Table build(Theme theme, Card card) {
        Color panel = roleColor(card.type());
        Color text = card.type() == CardType.WEAPON ? Theme.SOOT : Theme.BONE;

        // The frame: a darkened role colour showing as a thin border around the panel.
        Table tile = new Table();
        makeWholeFaceHittable(tile);
        tile.setBackground(theme.solid(darken(panel, 0.45f)));

        // The panel: base colour, a soft edge shade for depth, and the content on top.
        Stack stack = new Stack();
        stack.add(new Image(theme.solid(panel)));
        stack.add(new Image(theme.shadeRegion()));
        stack.add(content(theme, card, text));
        tile.add(stack).grow().pad(BORDER);
        return tile;
    }

    /** Rank+suit index top-left and bottom-right, type label between, big value centred. */
    private static Table content(Theme theme, Card card, Color text) {
        Table content = new Table();
        Table top = new Table();
        top.add(pip(theme, card, text)).left();
        top.add(label(card.type().name(), theme.small, dim(text, 0.7f))).expandX().right();
        content.add(top).growX().pad(10, 12, 0, 12);
        content.row();
        content.add(label(String.valueOf(card.value()), theme.display, text)).expand();
        content.row();
        Table bottom = new Table();
        bottom.add().expandX();
        bottom.add(pip(theme, card, text)).right();
        content.add(bottom).growX().pad(0, 12, 10, 12);
        return content;
    }

    private static Color darken(Color color, float factor) {
        return new Color(color.r * factor, color.g * factor, color.b * factor, 1f);
    }

    /**
     * Scene2D's {@link Table} defaults to {@link Touchable#childrenOnly}, so a
     * table is never itself a hit target — only its children are. Left alone, a
     * card would only respond where a label's glyphs happen to cover the pixel,
     * and presses anywhere else on its face would silently vanish.
     */
    static void makeWholeFaceHittable(Actor tile) {
        tile.setTouchable(Touchable.enabled);
    }

    static Color roleColor(CardType type) {
        return switch (type) {
            case MONSTER -> Theme.DRIED_BLOOD;
            case WEAPON -> Theme.IRON;
            case POTION -> Theme.HERBAL;
        };
    }

    /** Rank + suit identity — a playing-card corner index. */
    private static Table pip(Theme theme, Card card, Color text) {
        Table pip = new Table();
        String id = card.id();
        char suit = id.charAt(id.length() - 1);
        if (id.length() >= 2 && "SHDC".indexOf(suit) >= 0) {
            pip.add(label(id.substring(0, id.length() - 1), theme.bodyBold, text)).padRight(4);
            pip.add(new Image(theme.suitIcon(suit, text))).size(14, 14);
        }
        return pip;
    }
}
