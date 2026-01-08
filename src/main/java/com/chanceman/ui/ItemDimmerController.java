package com.chanceman.ui;

import com.chanceman.managers.RolledItemsManager;
import com.chanceman.filters.EnsouledHeadMapping;
import com.chanceman.menus.EnabledUI;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dims item icon widgets when item is TRADEABLE && LOCKED.
 * Runs at BeforeRender so scripts in the same frame can't overwrite opacity.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ItemDimmerController {
    private final Client client;
    private final RolledItemsManager rolledItemsManager;
    private final ItemManager itemManager;

    // Cache (long-lived) for tradeable-by-canonical-id
    private final ConcurrentHashMap<Integer, Boolean> tradeableCache = new ConcurrentHashMap<>();

    // Cache (per-frame) for "should dim?" decisions by raw item id
    private final Map<Integer, Boolean> dimDecisionCache = new HashMap<>(256);

    private volatile int dimOpacity = 150;
    @Setter
    private volatile boolean enabled = true;

    public void setDimOpacity(int opacity) {
        this.dimOpacity = Math.max(0, Math.min(255, opacity));
    }

    private boolean isCollectionLogWidget(Widget w)
    {
        return w != null && w.getId() >>> 16 == 621;
    }

    /**
     * Last chance before drawing this frame; safe place to enforce opacity without races.
     */
    @Subscribe
    public void onBeforeRender(BeforeRender e) {
        if (!enabled || client.getGameState() != GameState.LOGGED_IN) return;

        dimDecisionCache.clear();
        dimAllRoots();
    }

    private void dimAllRoots() {
        final Widget[] roots = client.getWidgetRoots();
        if (roots == null) return;

        for (Widget root : roots) {
            if (root != null) {
                walkAndDim(root);
            }
        }
    }

    private void walkAndDim(Widget w) {
        if (w == null || w.isHidden()) return;

        // Don't dim item icons in EnabledUIs configured to not grey locked items
        int groupId = w.getId() >>> 16;
        EnabledUI ui = EnabledUI.fromGroupId(groupId);
        if (ui != null && !ui.isGreyLockedItems()) {
            Widget[] dyn = w.getDynamicChildren();
            if (dyn != null) for (Widget c : dyn) walkAndDim(c);
            Widget[] stat = w.getStaticChildren();
            if (stat != null) for (Widget c : stat) walkAndDim(c);
            Widget[] nest = w.getNestedChildren();
            if (nest != null) for (Widget c : nest) walkAndDim(c);
            return;
        }

        if (isCollectionLogWidget(w)) return;
        final int itemId = w.getItemId();
        if (itemId > 0) {
            // Don’t override the game’s own dim on bank placeholders
            if (!isBankPlaceholderWidget(w)) {
                final int target = shouldDimMemoized(itemId) ? dimOpacity : 0;
                if (w.getOpacity() != target) {
                    w.setOpacity(target);
                }
            }
        }

        final Widget[] dyn = w.getDynamicChildren();
        if (dyn != null) for (Widget c : dyn) walkAndDim(c);
        final Widget[] stat = w.getStaticChildren();
        if (stat != null) for (Widget c : stat) walkAndDim(c);
        final Widget[] nest = w.getNestedChildren();
        if (nest != null) for (Widget c : nest) walkAndDim(c);
    }

    private boolean shouldDimMemoized(int rawItemId) {
        final int key = EnsouledHeadMapping.toTradeableId(rawItemId); // normalize cache key
        final Boolean cached = dimDecisionCache.get(key);
        if (cached != null) return cached;

        final boolean result = shouldDim(rawItemId);
        dimDecisionCache.put(key, result);
        return result;
    }

    private boolean shouldDim(int rawItemId) {
        final int mappedItemId = EnsouledHeadMapping.toTradeableId(rawItemId);
        final int canonicalItemId = canonicalize(mappedItemId);
        if (canonicalItemId <= 0) return false;
        if (!isTradeableCanonical(canonicalItemId)) return false;
        return !isRolled(mappedItemId, canonicalItemId);
    }

    private int canonicalize(int rawItemId) {
        try {
            return itemManager.canonicalize(rawItemId);
        } catch (Exception e) {
            // Fall back to the raw ID if canonicalization fails
            return rawItemId;
        }
    }

    private boolean isRolled(int normalizedItemId, int canonicalItemId) {
        if (rolledItemsManager  == null) return true; // fail open if manager missing

        try {
            // Fast paths: raw or canonical known obtained
            if (normalizedItemId > 0 && rolledItemsManager.isRolled(normalizedItemId)) return true;
            if (canonicalItemId > 0 && normalizedItemId != canonicalItemId && obtainedItemSubCheck(canonicalItemId))
                return true;

            // Slower path: related ids (placeholders / noted variants)
            final Set<Integer> candidates = new LinkedHashSet<>(4);
            candidates.add(normalizedItemId);
            candidates.add(canonicalItemId);

            collectRelatedIds(normalizedItemId, candidates);
            if (canonicalItemId != normalizedItemId) {
                collectRelatedIds(canonicalItemId, candidates);
            }

            for (int id : candidates) {
                if (id > 0 && obtainedItemSubCheck(id)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return true; // fail open on unexpected errors
        }

        return false;
    }

    private boolean obtainedItemSubCheck(int itemId) {
        try {
            return rolledItemsManager.isRolled(itemId);
        } catch (Exception e) {
            return true; // fail open if manager misbehaves
        }
    }

    private void collectRelatedIds(int itemId, Set<Integer> sink) {
        if (itemId <= 0) return;

        try {
            final ItemComposition comp = itemManager.getItemComposition(itemId);
            if (comp == null) return;

            // Placeholder
            if (comp.getPlaceholderTemplateId() != -1) {
                sink.add(comp.getPlaceholderId());
            }

            // Noted/unnoted pair
            final int linkedNoteId = comp.getLinkedNoteId();
            if (linkedNoteId > 0 && linkedNoteId != itemId) {
                sink.add(linkedNoteId);
            }
        } catch (Exception ignored) {
            // ignore bad compositions, rely on other IDs
        }
    }

    private boolean isBankPlaceholderWidget(Widget w) {
        return w != null && w.getItemId() > 0 && w.getItemQuantity() == 0;
    }

    private boolean isTradeableCanonical(int canonicalItemId) {
        try {
            final Boolean cached = tradeableCache.get(canonicalItemId);
            if (cached != null) return cached;

            boolean tradeable = false;
            final ItemComposition comp = itemManager.getItemComposition(canonicalItemId);
            if (comp != null) {
                tradeable = comp.isTradeable();
            }

            tradeableCache.put(canonicalItemId, tradeable);
            return tradeable;
        } catch (Exception e) {
            // If unsure, err on the side of NOT dimming
            return false;
        }
    }
}
