package com.chanceman.ui;

import com.chanceman.managers.UnlockedItemsManager;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dims the actual item icon widgets (no overlay).
 * - Only dims when item is TRADEABLE && LOCKED.
 * - Runs at BeforeRender so scripts in the same frame can't overwrite opacity.
 * - No deprecated getWidget / WidgetInfo usage.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ItemDimmerController
{
    private final Client client;
    private final UnlockedItemsManager unlockedItemsManager;
    private final ItemManager itemManager;

    private volatile int dimOpacity = 150;
    @Setter
    private volatile boolean enabled = true;

    private final ConcurrentHashMap<Integer, Boolean> tradeableCache = new ConcurrentHashMap<>();

    // Set by script events; consumed on BeforeRender (once per frame).
    private volatile boolean uiDirty = false;

    public void setDimOpacity(int opacity) { this.dimOpacity = Math.max(0, Math.min(255, opacity)); }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired e)
    {
        if (!enabled || client.getGameState() != GameState.LOGGED_IN) return;

        switch (e.getScriptId())
        {
            case ScriptID.INVENTORY_DRAWITEM:
            case ScriptID.BANKMAIN_BUILD:
            case ScriptID.BANKMAIN_FINISHBUILDING:
            case ScriptID.BANKMAIN_SEARCH_REFRESH:
            case ScriptID.BANK_DEPOSITBOX_INIT:
                uiDirty = true;
                break;
            default:
                // ignore others
        }
    }

    /**
     * Last chance before drawing this frame; safe place to enforce opacity without races.
     */
    @Subscribe
    public void onBeforeRender(BeforeRender e)
    {
        if (!enabled || !uiDirty || client.getGameState() != GameState.LOGGED_IN) return;
        if (client.isDraggingWidget() || client.isMenuOpen()) return;

        uiDirty = false;

        final Widget[] roots = client.getWidgetRoots();
        if (roots == null) return;

        for (Widget root : roots)
        {
            if (root != null) walkAndDim(root);
        }
    }

    private void walkAndDim(Widget w)
    {
        if (w == null || w.isHidden()) return;

        final int itemId = w.getItemId();
        if (itemId > 0)
        {
            final int target = shouldDim(itemId) ? dimOpacity : 0;
            if (w.getOpacity() != target)
            {
                w.setOpacity(target);
            }
        }

        final Widget[] dyn = w.getDynamicChildren();
        if (dyn != null) for (Widget c : dyn) walkAndDim(c);

        final Widget[] stat = w.getStaticChildren();
        if (stat != null) for (Widget c : stat) walkAndDim(c);

        final Widget[] nest = w.getNestedChildren();
        if (nest != null) for (Widget c : nest) walkAndDim(c);
    }

    private boolean shouldDim(int rawItemId)
    {
        if (!isTradeable(rawItemId)) return false;
        return !isUnlocked(rawItemId);
    }

    private boolean isUnlocked(int itemId)
    {
        try { return unlockedItemsManager != null && unlockedItemsManager.isUnlocked(itemId); }
        catch (Exception e) { return true; } // fail open
    }

    private boolean isTradeable(int rawItemId)
    {
        try
        {
            final int canonical = itemManager.canonicalize(rawItemId);
            final Boolean cached = tradeableCache.get(canonical);
            if (cached != null) return cached;

            boolean tradeable = false;
            final var comp = itemManager.getItemComposition(canonical);
            if (comp != null) tradeable = comp.isTradeable();

            tradeableCache.put(canonical, tradeable);
            return tradeable;
        }
        catch (Exception e)
        {
            // If unsure, err on the side of NOT dimming
            return false;
        }
    }
}
