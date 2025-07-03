package com.chanceman.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.FontMetrics;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Overlay that displays item name tooltips when hovering drop icons
 * injected into the music tab by {@link MusicWidgetController}.
 */
@Singleton
public class DropsTooltipOverlay extends Overlay
{
    private final Client client;
    private final ItemManager itemManager;
    private final MusicWidgetController widgetController;

    @Inject
    public DropsTooltipOverlay(
            Client client,
            ItemManager itemManager,
            MusicWidgetController widgetController
    )
    {
        this.client = client;
        this.itemManager = itemManager;
        this.widgetController = widgetController;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!widgetController.isOverrideActive()) {return null;}

        Point mouse = client.getMouseCanvasPosition();
        for (Map.Entry<Widget, Integer> entry : widgetController.getIconItemMap().entrySet())
        {
            Widget w = entry.getKey();
            if (w == null || w.isHidden()) {continue;}
            Rectangle bounds = w.getBounds();
            if (bounds.contains(mouse.getX(), mouse.getY()))
            {
                String name = itemManager
                        .getItemComposition(entry.getValue())
                        .getName();
                drawTooltip(graphics, name, mouse);
                break;
            }
        }
        return null;
    }

    private void drawTooltip(Graphics2D g, String text, Point mouse)
    {
        FontMetrics fm = g.getFontMetrics();
        int padding = 4;
        int textW   = fm.stringWidth(text);
        int textH   = fm.getHeight();

        int boxW = textW + padding * 2;
        int boxH = textH + padding * 2;

        int x = mouse.getX() + 10;
        int y = mouse.getY() - 10;

        // clamp to game canvas
        Rectangle clip = g.getClipBounds();
        x = Math.max(clip.x, Math.min(x, clip.x + clip.width  - boxW));
        y = Math.max(clip.y + boxH, Math.min(y, clip.y + clip.height));

        // fill background (more transparent)
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRect(x, y - boxH, boxW, boxH);

        // draw a slightly darker border
        g.setColor(new Color(50, 50, 50, 200));
        g.drawRect(x, y - boxH, boxW, boxH);

        // draw the text
        g.setColor(Color.WHITE);
        g.drawString(text, x + padding, y - padding);
    }
}
