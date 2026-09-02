package com.chanceman.ui;

import com.chanceman.ChanceManConfig;
import com.chanceman.drops.DropItem;
import com.chanceman.drops.NpcDropData;
import com.chanceman.managers.ObtainedItemsManager;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class MusicWidgetController
{
    private static final int MUSIC_GROUP = InterfaceID.Music.UNIVERSE >>> 16;
    private static final int ICON_SIZE = 32;
    private static final int PADDING = 4;
    private static final int COLUMNS = 4;
    private static final int MARGIN_X = 8;
    private static final int MARGIN_Y = 8;
    private static final int BAR_HEIGHT = 15;
    private static final int EYE_SIZE = 20;
    private static final int SEARCH_SPRITE = 1113;
    private static final int MUSIC_LIST_REDRAW_SCRIPT = 9289;

    private static final int[] HIDE_DURING_OVERRIDE =
            {
                    InterfaceID.Music.CONTROLS,
                    InterfaceID.Music.AREA,
                    InterfaceID.Music.SHUFFLE,
                    InterfaceID.Music.SINGLE,
                    InterfaceID.Music.SKIP,
                    InterfaceID.Music.PLAYLIST,
                    InterfaceID.Music.DROPDOWN_CONTAINER,
                    InterfaceID.Music.DROPDOWN,
                    InterfaceID.Music.DROPDOWN_CONTENT,
                    InterfaceID.Music.DROPDOWN_SCROLLBAR,
                    InterfaceID.Music.COUNT,
                    InterfaceID.Music.NOW_PLAYING_TEXT,
                    InterfaceID.Music.JUKEBOX
            };

    private final Client client;
    private final ClientThread clientThread;
    private final ObtainedItemsManager obtainedItemsManager;
    private final SpriteOverrideManager spriteOverrideManager;
    private final ItemSpriteCache itemSpriteCache;
    private final ChanceManConfig config;
    private final NpcSearchService searchService;

    @Inject
    private MusicSearchButton musicSearchButton;

    @Getter
    private final Map<Widget, DropItem> iconItemMap = new LinkedHashMap<>();

    private final Map<Widget, Boolean> originalHiddenStates = new IdentityHashMap<>();

    private int originalRootChildCount = -1;
    private int originalScrollableChildCount = -1;
    private int originalScrollableScrollHeight = -1;
    private int originalScrollableScrollY = -1;

    private NpcDropData currentDrops;
    private String originalTitleText;

    @Getter
    private boolean overrideActive;

    private boolean hideObtainedItems;

    @Inject
    public MusicWidgetController(
            Client client,
            ClientThread clientThread,
            ObtainedItemsManager obtainedItemsManager,
            SpriteOverrideManager spriteOverrideManager,
            ItemSpriteCache itemSpriteCache,
            ChanceManConfig config,
            NpcSearchService searchService)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.obtainedItemsManager = obtainedItemsManager;
        this.spriteOverrideManager = spriteOverrideManager;
        this.itemSpriteCache = itemSpriteCache;
        this.config = config;
        this.searchService = searchService;
    }

    public boolean hasData()
    {
        return currentDrops != null;
    }

    public NpcDropData getCurrentData()
    {
        return currentDrops;
    }

    /**
     * Replace the music widget with a drop table view for the given NPC.
     * If an override is already active, it will be updated.
     */
    public void override(NpcDropData dropData)
    {
        if (dropData == null)
        {
            return;
        }

        currentDrops = dropData;
        hideObtainedItems = false;
        musicSearchButton.onOverrideActivated();

        if (!overrideActive)
        {
            overrideActive = true;

            clientThread.invokeLater(() ->
            {
                if (!overrideActive || currentDrops == null)
                {
                    return;
                }

                captureNativeState();
                applyOverride(currentDrops);
                spriteOverrideManager.register();
            });
        }
        else
        {
            clientThread.invokeLater(() ->
            {
                if (overrideActive && currentDrops != null)
                {
                    applyOverride(currentDrops);
                }
            });
        }
    }

    /**
     * Remove the drop table view and restore the original Music interface.
     */
    public void restore()
    {
        if (!overrideActive)
        {
            return;
        }

        spriteOverrideManager.unregister();
        itemSpriteCache.clear();
        hideObtainedItems = false;

        runOnClientThread(this::revertOverride);
    }

    private void runOnClientThread(Runnable runnable)
    {
        if (client.isClientThread())
        {
            runnable.run();
        }
        else
        {
            clientThread.invoke(runnable);
        }
    }

    private Widget widget(int packed)
    {
        return client.getWidget(packed);
    }

    private Widget createChild(Widget parent, int type, int x, int y, int width, int height)
    {
        Widget child = parent.createChild(-1);
        child.setHidden(false);
        child.setType(type);
        child.setOriginalX(x);
        child.setOriginalY(y);
        child.setOriginalWidth(width);
        child.setOriginalHeight(height);
        return child;
    }

    private static int childCount(Widget parent)
    {
        if (parent == null)
        {
            return 0;
        }

        Widget[] children = parent.getChildren();
        return children == null ? 0 : children.length;
    }

    private void captureNativeState()
    {
        if (originalRootChildCount >= 0 || originalScrollableChildCount >= 0)
        {
            return;
        }

        Widget root = widget(InterfaceID.Music.UNIVERSE);
        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);
        Widget title = widget(InterfaceID.Music.NOW_PLAYING_TITLE);

        originalRootChildCount = childCount(root);
        originalScrollableChildCount = childCount(scrollable);

        if (scrollable != null)
        {
            originalScrollableScrollHeight = scrollable.getScrollHeight();
            originalScrollableScrollY = scrollable.getScrollY();
        }

        if (title != null)
        {
            originalTitleText = title.getText();
        }

        originalHiddenStates.clear();
    }

    private static void trimChildren(Widget parent, int originalCount)
    {
        if (parent == null || originalCount < 0)
        {
            return;
        }

        Widget[] children = parent.getChildren();
        if (children == null || children.length <= originalCount)
        {
            return;
        }

        parent.setChildren(Arrays.copyOf(children, originalCount));
        parent.revalidate();
    }

    private void removeChanceManChildren()
    {
        trimChildren(widget(InterfaceID.Music.UNIVERSE), originalRootChildCount);
        trimChildren(widget(InterfaceID.Music.SCROLLABLE), originalScrollableChildCount);
        iconItemMap.clear();
    }

    private void rememberHiddenState(Widget widget)
    {
        if (widget != null)
        {
            originalHiddenStates.putIfAbsent(widget, widget.isSelfHidden());
        }
    }

    private void setHiddenPreservingState(Widget widget, boolean hidden)
    {
        if (widget == null)
        {
            return;
        }

        rememberHiddenState(widget);
        widget.setHidden(hidden);
        widget.revalidate();
    }

    private void setHiddenPreservingState(int packed, boolean hidden)
    {
        setHiddenPreservingState(widget(packed), hidden);
    }

    private void hideChildrenPreservingState(Widget parent)
    {
        if (parent == null)
        {
            return;
        }

        hideChildrenPreservingState(parent.getStaticChildren());
        hideChildrenPreservingState(parent.getDynamicChildren());
        hideChildrenPreservingState(parent.getNestedChildren());
    }

    private void hideChildrenPreservingState(Widget[] children)
    {
        if (children == null)
        {
            return;
        }

        for (Widget child : children)
        {
            if (child != null)
            {
                setHiddenPreservingState(child, true);
            }
        }
    }

    private void restoreHiddenStates()
    {
        for (Map.Entry<Widget, Boolean> entry : originalHiddenStates.entrySet())
        {
            Widget widget = entry.getKey();

            if (widget == null)
            {
                continue;
            }

            try
            {
                widget.setHidden(entry.getValue());
                widget.revalidate();
            }
            catch (Exception ignored)
            {
            }
        }

        originalHiddenStates.clear();
    }

    private void restoreScrollableState()
    {
        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);

        if (scrollable == null)
        {
            return;
        }

        if (originalScrollableScrollHeight >= 0)
        {
            scrollable.setScrollHeight(originalScrollableScrollHeight);
        }

        if (originalScrollableScrollY >= 0)
        {
            scrollable.setScrollY(originalScrollableScrollY);
        }

        scrollable.revalidate();
        revalidateScroll(widget(InterfaceID.Music.SCROLLBAR));
    }

    private void revalidateScroll(Widget scrollbar)
    {
        if (scrollbar != null)
        {
            scrollbar.revalidate();
            scrollbar.revalidateScroll();
        }
    }

    private Widget updateTitle(NpcDropData dropData)
    {
        Widget title = widget(InterfaceID.Music.NOW_PLAYING_TITLE);

        if (title != null)
        {
            setHiddenPreservingState(title, false);
            title.setText(dropData.getName());
            title.revalidate();
        }

        return title;
    }

    private void hideNativeMusicUi()
    {
        for (int packed : HIDE_DURING_OVERRIDE)
        {
            setHiddenPreservingState(packed, true);
        }

        hideChildrenPreservingState(widget(InterfaceID.Music.NOW_PLAYING));
    }

    private int absX(Widget root, Widget widget)
    {
        int x = 0;
        Widget current = widget;

        while (current != null && root != null && current.getId() != root.getId())
        {
            x += current.getOriginalX();

            int parentId = current.getParentId();
            if (parentId == -1)
            {
                break;
            }

            current = client.getWidget(parentId);
        }

        return x;
    }

    private int absY(Widget root, Widget widget)
    {
        int y = 0;
        Widget current = widget;

        while (current != null && root != null && current.getId() != root.getId())
        {
            y += current.getOriginalY();

            int parentId = current.getParentId();
            if (parentId == -1)
            {
                break;
            }

            current = client.getWidget(parentId);
        }

        return y;
    }

    private int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    private void drawProgressBarAndToggle(
            Widget root,
            Widget title,
            NpcDropData dropData,
            int obtainedCount,
            int totalDrops)
    {
        int fontId = title != null ? title.getFontId() : 0;
        boolean shadowed = title != null && title.getTextShadowed();

        final int closeSprite = 520;
        final int closeSize = 10;
        final int closePad = 4;

        Widget close = createChild(
                root, WidgetType.GRAPHIC,
                closePad, closePad, closeSize, closeSize);

        close.setSpriteId(closeSprite);
        close.setAction(0, "Close");
        close.setHasListener(true);
        close.setOnOpListener((JavaScriptCallback) (ScriptEvent event) -> restore());
        close.revalidate();

        String levelText = String.format("Lvl %d", dropData.getLevel());
        int levelWidth = Math.max(60, (levelText.length() * 6) + 8);

        int titleX = title != null ? absX(root, title) : 0;
        int titleY = title != null ? absY(root, title) : 0;
        int titleWidth = title != null ? title.getOriginalWidth() : 0;
        int titleHeight = title != null ? title.getOriginalHeight() : 0;

        Widget frame = widget(InterfaceID.Music.FRAME);
        int frameX = frame != null ? absX(root, frame) : 0;
        int frameWidth = frame != null ? frame.getOriginalWidth() : 0;
        int frameRight = frameWidth > 0 ? frameX + frameWidth : titleX + titleWidth;

        int levelX = clamp(
                frameRight - levelWidth - PADDING + 15,
                titleX + titleWidth + (PADDING * 2),
                frameRight - 10);

        Widget level = createChild(
                root, WidgetType.TEXT,
                levelX, titleY, levelWidth, titleHeight);

        level.setText(levelText);
        level.setFontId(fontId);
        level.setTextShadowed(shadowed);
        level.setTextColor(0x00b33c);
        level.revalidate();

        int barX = titleX;
        int barY = Math.max(0, titleY + titleHeight - 1);
        int barWidth = Math.max(120, levelX - PADDING - barX);

        Widget background = createChild(
                root, WidgetType.RECTANGLE,
                barX, barY, barWidth, BAR_HEIGHT);

        background.setFilled(true);
        background.setTextColor(0x000000);
        background.revalidate();

        final int border = 1;
        int innerWidth = barWidth - border * 2;
        int fillWidth = totalDrops <= 0
                ? 0
                : Math.round(innerWidth * (float) obtainedCount / totalDrops);

        Widget fill = createChild(
                root, WidgetType.RECTANGLE,
                barX + border,
                barY + border,
                fillWidth,
                BAR_HEIGHT - border * 2);

        fill.setFilled(true);
        fill.setTextColor(0x00b33c);
        fill.revalidate();

        String progressText = String.format("%d/%d", obtainedCount, totalDrops);

        Widget label = createChild(
                root, WidgetType.TEXT,
                barX + (barWidth / 2) - (progressText.length() * 4),
                barY + (BAR_HEIGHT / 2) - 6,
                barWidth,
                BAR_HEIGHT);

        label.setText(progressText);
        label.setTextColor(0xFFFFFF);
        label.setFontId(fontId);
        label.setTextShadowed(shadowed);
        label.revalidate();

        int eyeX = barX + barWidth + 4;
        int eyeY = barY + (BAR_HEIGHT / 2) - (EYE_SIZE / 2);

        Widget eye = createChild(
                root, WidgetType.GRAPHIC,
                eyeX, eyeY, EYE_SIZE, EYE_SIZE);

        eye.setSpriteId(hideObtainedItems ? 2222 : 2221);
        eye.setAction(0, "Toggle obtained items");
        eye.setHasListener(true);
        eye.setOnOpListener((JavaScriptCallback) (ScriptEvent event) ->
        {
            hideObtainedItems = !hideObtainedItems;
            updateIconsVisibilityAndLayout();
            eye.setSpriteId(hideObtainedItems ? 2222 : 2221);
            eye.revalidate();
        });
        eye.revalidate();

        Widget search = createChild(
                root, WidgetType.GRAPHIC,
                eyeX + EYE_SIZE + PADDING,
                eyeY,
                EYE_SIZE,
                EYE_SIZE);

        search.setSpriteId(SEARCH_SPRITE);
        search.setAction(0, "Search Drops");
        search.setHasListener(true);
        search.setOnOpListener((JavaScriptCallback) event -> showSearchDialog());
        search.revalidate();

        root.revalidate();
    }

    /**
     * Display a Swing dialog prompting the user for an NPC name or ID. The
     * potentially long running search executes on a background thread so the
     * UI remains responsive. Selecting a result will override the widget with
     * the chosen drop table.
     */
    private void showSearchDialog()
    {
        SwingUtilities.invokeLater(() ->
        {
            String query = JOptionPane.showInputDialog(
                    null,
                    "Enter NPC name or ID:",
                    "Search NPC",
                    JOptionPane.PLAIN_MESSAGE);

            if (query == null || query.trim().isEmpty())
            {
                return;
            }

            String trimmedQuery = query.trim();

            new Thread(() ->
            {
                List<NpcDropData> results = searchService.search(trimmedQuery);

                SwingUtilities.invokeLater(() ->
                {
                    if (results.isEmpty())
                    {
                        JOptionPane.showMessageDialog(
                                null,
                                "No NPCs found for: " + query,
                                "Search NPC",
                                JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    List<NpcDropData> limited = results.stream()
                            .limit(5)
                            .collect(Collectors.toList());

                    String[] choices = limited.stream()
                            .map(n -> String.format(
                                    "%s (ID %d, Lvl %d)",
                                    n.getName(),
                                    n.getNpcId(),
                                    n.getLevel()))
                            .toArray(String[]::new);

                    int index = JOptionPane.showOptionDialog(
                            null,
                            "Select NPC:",
                            "Search Results",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            choices,
                            choices[0]);

                    if (index >= 0 && index < limited.size())
                    {
                        NpcDropData selected = limited.get(index);
                        searchService.cacheSelected(selected);
                        override(selected);
                    }
                });
            }, "ChanceMan-DropSearch").start();
        });
    }

    public void openDropsSearch()
    {
        showSearchDialog();
    }

    private void drawDropIcons(
            Widget scrollable,
            Widget scrollbar,
            List<DropItem> drops,
            Set<Integer> obtainedIds)
    {
        if (scrollable == null || scrollbar == null)
        {
            return;
        }

        hideChildrenPreservingState(scrollable);

        for (DropItem drop : drops)
        {
            int itemId = drop.getItemId();

            Widget icon = createChild(
                    scrollable, WidgetType.GRAPHIC,
                    MARGIN_X, MARGIN_Y, ICON_SIZE, ICON_SIZE);

            icon.setSpriteId(itemSpriteCache.getSpriteId(itemId));
            icon.setItemQuantityMode(ItemQuantityMode.NEVER);
            icon.setOpacity(obtainedIds.contains(itemId) ? 0 : 150);
            icon.revalidate();

            iconItemMap.put(icon, drop);
        }

        updateIconsVisibilityAndLayout();
    }

    private void updateIconsVisibilityAndLayout()
    {
        Set<Integer> obtainedIds = obtainedItemsManager.getObtainedItems();
        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);
        Widget scrollbar = widget(InterfaceID.Music.SCROLLBAR);

        int displayIndex = 0;

        for (Map.Entry<Widget, DropItem> entry : iconItemMap.entrySet())
        {
            Widget icon = entry.getKey();
            boolean obtained = obtainedIds.contains(entry.getValue().getItemId());

            if (hideObtainedItems && obtained)
            {
                icon.setHidden(true);
                continue;
            }

            int col = displayIndex % COLUMNS;
            int row = displayIndex / COLUMNS;

            icon.setHidden(false);
            icon.setOriginalX(MARGIN_X + col * (ICON_SIZE + PADDING));
            icon.setOriginalY(MARGIN_Y + row * (ICON_SIZE + PADDING));
            icon.revalidate();

            displayIndex++;
        }

        int rows = (displayIndex + COLUMNS - 1) / COLUMNS;

        if (scrollable != null)
        {
            scrollable.setScrollHeight(MARGIN_Y * 2 + rows * (ICON_SIZE + PADDING));
            scrollable.revalidate();
        }

        revalidateScroll(scrollbar);
    }

    private List<DropItem> buildDrops(NpcDropData dropData)
    {
        List<DropItem> drops = dropData.getDropTableSections().stream()
                .filter(section ->
                {
                    String header = section.getHeader();

                    if (header == null)
                    {
                        return true;
                    }

                    String lower = header.toLowerCase();

                    if (lower.contains("rare and gem drop table"))
                    {
                        return config.showRareDropTable() && config.showGemDropTable();
                    }

                    if (!config.showRareDropTable() && lower.contains("rare drop table"))
                    {
                        return false;
                    }

                    return config.showGemDropTable() || !lower.contains("gem drop table");
                })
                .flatMap(section -> section.getItems().stream())
                .collect(Collectors.toList());

        return WidgetUtils.dedupeAndSort(drops, config.sortDropsByRarity());
    }

    private void applyOverride(NpcDropData dropData)
    {
        removeChanceManChildren();
        hideNativeMusicUi();

        Widget root = widget(InterfaceID.Music.UNIVERSE);
        Widget title = updateTitle(dropData);
        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);
        Widget scrollbar = widget(InterfaceID.Music.SCROLLBAR);

        setHiddenPreservingState(scrollable, false);
        setHiddenPreservingState(scrollbar, false);

        List<DropItem> drops = buildDrops(dropData);
        Set<Integer> obtainedIds = obtainedItemsManager.getObtainedItems();

        int totalDrops = drops.size();
        int obtainedCount = (int) drops.stream()
                .filter(drop -> obtainedIds.contains(drop.getItemId()))
                .count();

        if (root != null)
        {
            drawProgressBarAndToggle(root, title, dropData, obtainedCount, totalDrops);
        }

        drawDropIcons(scrollable, scrollbar, drops, obtainedIds);

        if (root != null)
        {
            root.revalidate();
        }
    }

    private void redrawNativeMusicList()
    {
        client.runScript(
                MUSIC_LIST_REDRAW_SCRIPT,
                InterfaceID.Music.INNER,
                InterfaceID.Music.JUKEBOX,
                InterfaceID.Music.SCROLLABLE,
                InterfaceID.Music.SCROLLBAR,
                InterfaceID.Music.COUNT,
                InterfaceID.Music.OVERLAY
        );

        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);

        if (scrollable != null)
        {
            scrollable.revalidate();
        }

        revalidateScroll(widget(InterfaceID.Music.SCROLLBAR));
    }

    private void revertOverride()
    {
        if (!overrideActive)
        {
            return;
        }

        removeChanceManChildren();
        restoreHiddenStates();

        Widget title = widget(InterfaceID.Music.NOW_PLAYING_TITLE);

        if (title != null && originalTitleText != null)
        {
            title.setText(originalTitleText);
            title.revalidate();
        }

        restoreScrollableState();

        overrideActive = false;
        currentDrops = null;
        hideObtainedItems = false;

        clearNativeStateBackup();
        redrawNativeMusicList();

        musicSearchButton.onOverrideDeactivated();
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() != MUSIC_GROUP)
        {
            return;
        }

        musicSearchButton.invalidate();

        if (!overrideActive || currentDrops == null)
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            if (!overrideActive || currentDrops == null)
            {
                return;
            }

            clearNativeStateBackup();
            captureNativeState();
            applyOverride(currentDrops);
        });
    }

    private void clearNativeStateBackup()
    {
        originalRootChildCount = -1;
        originalScrollableChildCount = -1;
        originalScrollableScrollHeight = -1;
        originalScrollableScrollY = -1;

        originalHiddenStates.clear();
        originalTitleText = null;
        iconItemMap.clear();
    }
}