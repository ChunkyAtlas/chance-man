package com.chanceman.ui;

import com.chanceman.ChanceManConfig;
import com.chanceman.drops.DropItem;
import com.chanceman.drops.NpcDropData;
import com.chanceman.managers.RolledItemsManager;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptEvent;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;

@Slf4j
@Singleton
public class MusicWidgetController
{
    private static final int MUSIC_GROUP = 239;
    private static final int ICON_SIZE = 32;
    private static final int PADDING = 4;
    private static final int COLUMNS = 4;
    private static final int MARGIN_X = 8;
    private static final int MARGIN_Y = 8;
    private static final int BAR_HEIGHT = 15;
    private static final float WIDTH_RATIO = 0.7f;
    private static final int EYE_SIZE = 20;
    private static final int SEARCH_SPRITE = 1113;

    private final Client client;
    private final ClientThread clientThread;
    private final RolledItemsManager rolledItemsManager;
    private final SpriteOverrideManager spriteOverrideManager;
    private final ItemSpriteCache itemSpriteCache;
    private final ChanceManConfig config;
    private final NpcSearchService searchService;

    private NpcDropData currentDrops = null;
    private List<Widget> backupJukeboxStaticKids = null;
    private List<Widget> backupJukeboxDynamicKids = null;
    private List<Widget> backupScrollStaticKids = null;
    private List<Widget> backupScrollDynamicKids = null;
    private List<Widget> backupRootStaticKids = null;
    private List<Widget> backupRootDynamicKids = null;
    private final List<Widget> overrideRootWidgets = new ArrayList<>();
    private final List<Widget> overrideScrollWidgets = new ArrayList<>();
    private Integer originalRootType = null;
    private String originalTitleText = null;
    @Getter private final Map<Widget, DropItem> iconItemMap = new LinkedHashMap<>();
    @Getter private boolean overrideActive = false;
    @Inject private MusicSearchButton musicSearchButton;
    private boolean hideRolledItems = false;

    @Inject
    public MusicWidgetController(
            Client client,
            ClientThread clientThread,
            RolledItemsManager rolledItemsManager,
            SpriteOverrideManager spriteOverrideManager,
            ItemSpriteCache itemSpriteCache,
            ChanceManConfig config,
            NpcSearchService searchService)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.rolledItemsManager = rolledItemsManager;
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
     * If an override is already active, it will be updated
     */
    public void override(NpcDropData dropData)
    {
        if (dropData == null)
        {
            return;
        }
        currentDrops = dropData;
        hideRolledItems = false;
        musicSearchButton.onOverrideActivated();
        if (!overrideActive)
        {
            overrideActive = true;
            clientThread.invokeLater(() ->
            {
                applyOverride(dropData);
                spriteOverrideManager.register();
            });
        }
        else
        {
            clientThread.invokeLater(() -> applyOverride(dropData));
        }
    }

    /**
     * Remove the drop table overlay and restore the original music widget.
     */
    public void restore()
    {
        if (!overrideActive)
        {
            return;
        }
        spriteOverrideManager.unregister();
        itemSpriteCache.clear();
        hideRolledItems = false;
        clientThread.invokeLater(this::revertOverride);
    }

    private void updateIconsVisibilityAndLayout()
    {
        Set<Integer> rolledIds = rolledItemsManager.getRolledItems();
        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        Widget scrollbar = client.getWidget(MUSIC_GROUP, 7);

        int displayIndex = 0;

        for (Map.Entry<Widget, DropItem> e : iconItemMap.entrySet())
        {
            Widget icon = e.getKey();
            DropItem d = e.getValue();
            boolean rolled = rolledIds.contains(d.getItemId());

            if (hideRolledItems && rolled)
            {
                icon.setHidden(true);
            }
            else
            {
                icon.setHidden(false);
                int col = displayIndex % COLUMNS;
                int row = displayIndex / COLUMNS;
                int x = MARGIN_X + col * (ICON_SIZE + PADDING);
                int y = MARGIN_Y + row * (ICON_SIZE + PADDING);
                icon.setOriginalX(x);
                icon.setOriginalY(y);
                icon.revalidate();
                displayIndex++;
            }
        }

        int rows = (displayIndex + COLUMNS - 1) / COLUMNS;
        if (scrollable != null)
        {
            scrollable.setScrollHeight(MARGIN_Y * 2 + rows * (ICON_SIZE + PADDING));
            scrollable.revalidate();
        }
        if (scrollbar != null)
        {
            scrollbar.revalidateScroll();
        }
    }

    private static List<Widget> copyChildren(Widget parent, boolean dynamic)
    {
        if (parent == null)
        {
            return new ArrayList<>();
        }
        Widget[] kids = dynamic ? parent.getDynamicChildren() : parent.getChildren();
        return kids != null ? new ArrayList<>(Arrays.asList(kids)) : new ArrayList<>();
    }

    private static void restoreChildren(Widget parent, List<Widget> staticKids, List<Widget> dynamicKids)
    {
        if (parent == null)
        {
            return;
        }

        Widget[] currentStatic = parent.getChildren();
        if (currentStatic != null)
        {
            for (Widget w : currentStatic)
            {
                w.setHidden(true);
            }
        }

        Widget[] currentDyn = parent.getDynamicChildren();
        if (currentDyn != null)
        {
            for (Widget w : currentDyn)
            {
                w.setHidden(true);
            }
        }

        if (staticKids != null)
        {
            for (Widget w : staticKids)
            {
                w.setHidden(false);
            }
        }
        if (dynamicKids != null)
        {
            for (Widget w : dynamicKids)
            {
                w.setHidden(false);
            }
        }

        parent.revalidate();
    }

    private Widget updateTitle(NpcDropData dropData)
    {
        Widget title = client.getWidget(MUSIC_GROUP, 8);
        if (title != null)
        {
            if (originalTitleText == null)
            {
                originalTitleText = title.getText();
            }
            title.setText(dropData.getName());
            title.revalidate();
        }
        return title;
    }

    private void drawProgressBarAndToggle(Widget root, Widget title, NpcDropData dropData, int rolledCount, int totalDrops)
    {
        int fontId = title != null ? title.getFontId() : 0;
        boolean shadowed = title != null && title.getTextShadowed();

        int lvlX = Objects.requireNonNull(title).getOriginalX() + title.getOriginalWidth() + 83;
        int lvlY = title.getOriginalY();

        Widget lvl = root.createChild(-1);
        lvl.setHidden(false);
        lvl.setType(WidgetType.TEXT);
        lvl.setText(String.format("Lvl %d", dropData.getLevel()));
        lvl.setFontId(fontId);
        lvl.setTextShadowed(shadowed);
        lvl.setTextColor(0x00b33c);
        lvl.setOriginalX(lvlX);
        lvl.setOriginalY(lvlY);
        lvl.setOriginalWidth(title.getOriginalWidth());
        lvl.setOriginalHeight(title.getOriginalHeight());
        lvl.revalidate();
        overrideRootWidgets.add(lvl);

        Widget oldBar = client.getWidget(MUSIC_GROUP, 9);
        if (oldBar == null)
        {
            return;
        }
        int xOld = oldBar.getOriginalX();
        int yOld = oldBar.getOriginalY();
        int wOld = oldBar.getOriginalWidth();
        int hOld = oldBar.getOriginalHeight();

        int newW = Math.round(wOld * WIDTH_RATIO);
        int newY = yOld + (hOld - BAR_HEIGHT) / 2;

        Widget bg = root.createChild(-1);
        bg.setHidden(false);
        bg.setType(WidgetType.RECTANGLE);
        bg.setOriginalX(xOld);
        bg.setOriginalY(newY);
        bg.setOriginalWidth(newW);
        bg.setOriginalHeight(BAR_HEIGHT);
        bg.setFilled(true);
        bg.setTextColor(0x000000);
        bg.revalidate();
        overrideRootWidgets.add(bg);

        final int border = 1;
        int innerWidth = newW - border * 2;
        int fillW = Math.round(innerWidth * (float) rolledCount / totalDrops);

        Widget fill = root.createChild(-1);
        fill.setHidden(false);
        fill.setType(WidgetType.RECTANGLE);
        fill.setOriginalX(xOld + border);
        fill.setOriginalY(newY + border);
        fill.setOriginalWidth(fillW);
        fill.setOriginalHeight(BAR_HEIGHT - border * 2);
        fill.setFilled(true);
        fill.setTextColor(0x00b33c);
        fill.revalidate();
        overrideRootWidgets.add(fill);

        String txt = String.format("%d/%d", rolledCount, totalDrops);
        Widget label = root.createChild(-1);
        label.setHidden(false);
        label.setType(WidgetType.TEXT);
        label.setText(txt);
        label.setTextColor(0xFFFFFF);
        label.setFontId(fontId);
        label.setTextShadowed(shadowed);
        label.setOriginalWidth(newW);
        label.setOriginalHeight(BAR_HEIGHT);
        label.setOriginalX(xOld + (newW / 2) - (txt.length() * 4));
        label.setOriginalY(newY + (BAR_HEIGHT / 2) - 6);
        label.revalidate();
        overrideRootWidgets.add(label);

        int eyeX = xOld + newW + 4;
        int eyeY = newY + (BAR_HEIGHT / 2) - (EYE_SIZE / 2);

        Widget eye = root.createChild(-1);
        eye.setHidden(false);
        eye.setType(WidgetType.GRAPHIC);
        eye.setOriginalX(eyeX);
        eye.setOriginalY(eyeY);
        eye.setOriginalWidth(EYE_SIZE);
        eye.setOriginalHeight(EYE_SIZE);
        eye.setSpriteId(hideRolledItems ? 2222 : 2221);
        eye.revalidate();
        eye.setAction(0, "Toggle rolled items");

        overrideRootWidgets.add(eye);
        eye.setOnOpListener((JavaScriptCallback) (ScriptEvent ev) ->
        {
            hideRolledItems = !hideRolledItems;
            updateIconsVisibilityAndLayout();
            eye.setSpriteId(hideRolledItems ? 2222 : 2221);
            eye.revalidate();
        });
        eye.setHasListener(true);

        int searchX = eyeX + EYE_SIZE + PADDING;
        int searchY = eyeY;

        Widget search = root.createChild(-1);
        search.setHidden(false);
        search.setType(WidgetType.GRAPHIC);
        search.setOriginalX(searchX);
        search.setOriginalY(searchY);
        search.setOriginalWidth(EYE_SIZE);
        search.setOriginalHeight(EYE_SIZE);
        search.setSpriteId(SEARCH_SPRITE);
        search.revalidate();
        search.setAction(0, "Search Drops");
        overrideRootWidgets.add(search);

        search.setOnOpListener((JavaScriptCallback) ev -> showSearchDialog());
        search.setHasListener(true);

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
                    JOptionPane.PLAIN_MESSAGE
            );
            if (query == null || query.trim().isEmpty())
            {
                return;
            }

            new Thread(() -> {
                List<NpcDropData> results = searchService.search(query.trim());

                SwingUtilities.invokeLater(() -> {
                    if (results.isEmpty())
                    {
                        JOptionPane.showMessageDialog(
                                null,
                                "No NPCs found for: " + query,
                                "Search NPC",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                    }

                    List<NpcDropData> limited = results.stream().limit(5).collect(Collectors.toList());
                    String[] choices = limited.stream()
                            .map(n -> String.format("%s (ID %d, Lvl %d)", n.getName(), n.getNpcId(), n.getLevel()))
                            .toArray(String[]::new);
                    int idx = JOptionPane.showOptionDialog(
                            null,
                            "Select NPC:",
                            "Search Results",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            choices,
                            choices[0]
                    );
                    if (idx >= 0 && idx < limited.size())
                    {
                        override(limited.get(idx));
                    }
                });
            }).start();
        });
    }

    public void openDropsSearch()
    {
        showSearchDialog();
    }

    private void drawDropIcons(Widget scrollable, Widget scrollbar, Widget jukebox, List<DropItem> drops, Set<Integer> rolledIds)
    {
        if (scrollable == null || scrollbar == null)
        {
            return;
        }

        if (backupJukeboxStaticKids == null && jukebox != null)
        {
            backupJukeboxStaticKids = copyChildren(jukebox, false);
        }
        if (backupJukeboxDynamicKids == null && jukebox != null)
        {
            backupJukeboxDynamicKids = copyChildren(jukebox, true);
        }
        if (backupScrollStaticKids == null)
        {
            backupScrollStaticKids = copyChildren(scrollable, false);
        }
        if (backupScrollDynamicKids == null)
        {
            backupScrollDynamicKids = copyChildren(scrollable, true);
        }

        WidgetUtils.hideAllChildrenSafely(jukebox);
        WidgetUtils.hideAllChildrenSafely(scrollable);

        for (DropItem d : drops)
        {
            int itemId = d.getItemId();
            Widget icon = scrollable.createChild(-1);
            icon.setHidden(false);
            icon.setType(WidgetType.GRAPHIC);
            int spriteId = itemSpriteCache.getSpriteId(itemId);
            icon.setSpriteId(spriteId);
            icon.setItemQuantityMode(ItemQuantityMode.NEVER);
            icon.setOriginalX(MARGIN_X);
            icon.setOriginalY(MARGIN_Y);
            icon.setOriginalWidth(ICON_SIZE);
            icon.setOriginalHeight(ICON_SIZE);
            icon.setOpacity(rolledIds.contains(itemId) ? 0 : 150);
            icon.revalidate();

            iconItemMap.put(icon, d);
            overrideScrollWidgets.add(icon);
        }

        updateIconsVisibilityAndLayout();
    }

    private void applyOverride(NpcDropData dropData)
    {
        iconItemMap.clear();
        int[] toHide = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        for (int childId : toHide)
        {
            Widget w = client.getWidget(MUSIC_GROUP, childId);
            if (w != null)
            {
                w.setHidden(true);
            }
        }

        Widget root = client.getWidget(MUSIC_GROUP, 0);
        if (backupRootStaticKids == null && root != null)
        {
            backupRootStaticKids = copyChildren(root, false);
        }
        if (backupRootDynamicKids == null && root != null)
        {
            backupRootDynamicKids = copyChildren(root, true);
        }
        overrideRootWidgets.clear();
        overrideScrollWidgets.clear();

        List<DropItem> drops = dropData.getDropTableSections().stream()
                .filter(sec ->
                {
                    String h = sec.getHeader();
                    if (h == null)
                    {
                        return true;
                    }
                    String lower = h.toLowerCase();
                    if (lower.contains("rare and gem drop table"))
                    {
                        return config.showRareDropTable() && config.showGemDropTable();
                    }
                    if (!config.showRareDropTable() && lower.contains("rare drop table"))
                    {
                        return false;
                    }
                    if (!config.showGemDropTable() && lower.contains("gem drop table"))
                    {
                        return false;
                    }
                    return true;
                })
                .flatMap(sec -> sec.getItems().stream())
                .collect(Collectors.toList());
        drops = WidgetUtils.dedupeAndSort(drops, config.sortDropsByRarity());

        Set<Integer> rolledIds = rolledItemsManager.getRolledItems();
        int totalDrops = drops.size();
        int rolledCount = (int) drops.stream()
                .filter(d -> rolledIds.contains(d.getItemId()))
                .count();

        Widget title = updateTitle(dropData);

        if (root != null)
        {
            root.setHidden(false);
            if (originalRootType == null)
            {
                originalRootType = root.getType();
            }
            root.setType(WidgetType.LAYER);
            WidgetUtils.hideAllChildrenSafely(root);

            drawProgressBarAndToggle(root, title, dropData, rolledCount, totalDrops);
        }

        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        Widget jukebox = client.getWidget(MUSIC_GROUP, 6);
        Widget scrollbar = client.getWidget(MUSIC_GROUP, 7);

        drawDropIcons(scrollable, scrollbar, jukebox, drops, rolledIds);

        if (root != null)
        {
            root.revalidate();
        }
    }

    private void revertOverride()
    {
        if (!overrideActive)
        {
            return;
        }

        Widget root = client.getWidget(MUSIC_GROUP, 0);
        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        Widget jukebox = client.getWidget(MUSIC_GROUP, 6);

        purgeWidgets(overrideRootWidgets);
        purgeWidgets(overrideScrollWidgets);
        overrideRootWidgets.clear();
        overrideScrollWidgets.clear();

        if (root != null)
        {
            restoreChildren(root, backupRootStaticKids, backupRootDynamicKids);
            if (originalRootType != null)
            {
                root.setType(originalRootType);
            }
        }

        restoreChildren(scrollable, backupScrollStaticKids, backupScrollDynamicKids);
        restoreChildren(jukebox, backupJukeboxStaticKids, backupJukeboxDynamicKids);

        Widget title = client.getWidget(MUSIC_GROUP, 8);
        Widget overlay = client.getWidget(MUSIC_GROUP, 5);
        Widget scrollbar = client.getWidget(MUSIC_GROUP, 7);
        Widget progress = client.getWidget(MUSIC_GROUP, 9);

        if (title != null && originalTitleText != null)
        {
            title.setText(originalTitleText);
            title.revalidate();
            for (int id = 9; id <= 19; id++)
            {
                Widget w = client.getWidget(MUSIC_GROUP, id);
                if (w != null)
                {
                    w.setHidden(false);
                    w.revalidate();
                }
            }
        }

        if (overlay != null)
        {
            overlay.setHidden(false);
            overlay.revalidate();
        }
        if (scrollbar != null)
        {
            scrollbar.setHidden(false);
            scrollbar.revalidate();
        }
        if (progress != null)
        {
            progress.setHidden(false);
            progress.revalidate();
        }

        if (root != null && root.getOnLoadListener() != null)
        {
            client.createScriptEvent(root.getOnLoadListener())
                    .setSource(root)
                    .run();
            root.revalidate();
        }
        if (overlay != null && overlay.getOnLoadListener() != null)
        {
            client.createScriptEvent(overlay.getOnLoadListener())
                    .setSource(overlay)
                    .run();
            overlay.revalidate();
        }
        if (scrollbar != null && scrollbar.getOnLoadListener() != null)
        {
            client.createScriptEvent(scrollbar.getOnLoadListener())
                    .setSource(scrollbar)
                    .run();
            scrollbar.revalidate();
        }
        if (jukebox != null && jukebox.getOnLoadListener() != null)
        {
            client.createScriptEvent(jukebox.getOnLoadListener())
                    .setSource(jukebox)
                    .run();
            jukebox.revalidate();
        }

        originalTitleText = null;
        currentDrops = null;
        overrideActive = false;
        backupRootStaticKids = null;
        backupRootDynamicKids = null;
        originalRootType = null;
        backupJukeboxStaticKids = null;
        backupJukeboxDynamicKids = null;
        backupScrollStaticKids = null;
        backupScrollDynamicKids = null;
        iconItemMap.clear();
        musicSearchButton.onOverrideDeactivated();
    }


    /**
     * Forcefully removes widgets we created during override so they cannot
     * be resurrected by the music tab's onLoad() which tends to unhide children.
     */
    private static void purgeWidgets(List<Widget> widgets)
    {
        if (widgets == null)
        {
            return;
        }
        for (Widget w : widgets)
        {
            if (w == null)
            {
                continue;
            }
            try {
                w.setOnOpListener((JavaScriptCallback) null);
                w.setHasListener(false);
                w.setHidden(true);
                w.setType(0);
                w.revalidate();
            } catch (Exception ignored) {}
        }
    }
}