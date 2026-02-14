package com.chanceman.ui;

import com.chanceman.ChanceManConfig;
import com.chanceman.drops.DropItem;
import com.chanceman.drops.NpcDropData;
import com.chanceman.managers.ObtainedItemsManager;

import java.util.*;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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

    // Widgets we hide during override and MUST ensure come back after restore.
    private static final int[] RESTORE_FORCE_VISIBLE_PACKEDS = new int[]
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
            };

    private static final int[] RESTORE_FORCE_SHOW = new int[]
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

                    InterfaceID.Music.JUKEBOX,
                    InterfaceID.Music.INNER,
                    InterfaceID.Music.SCROLLABLE,
                    InterfaceID.Music.SCROLLBAR,
                    InterfaceID.Music.NOW_PLAYING,
                    InterfaceID.Music.CONTENTS,
                    InterfaceID.Music.OVERLAY,
                    InterfaceID.Music.UNIVERSE
            };

    private final Client client;
    private final ClientThread clientThread;
    private final ObtainedItemsManager obtainedItemsManager;
    private final SpriteOverrideManager spriteOverrideManager;
    private final ItemSpriteCache itemSpriteCache;
    private final ChanceManConfig config;
    private final NpcSearchService searchService;

    private NpcDropData currentDrops = null;

    private static final class ChildBackup
    {
        List<Widget> stat = Collections.emptyList();
        List<Widget> dyn  = Collections.emptyList();

        boolean captured()
        {
            return !stat.isEmpty() || !dyn.isEmpty();
        }
    }

    private enum SnapTarget
    {
        SCROLLABLE(InterfaceID.Music.SCROLLABLE),
        JUKEBOX(InterfaceID.Music.JUKEBOX),
        NOW_PLAYING(InterfaceID.Music.NOW_PLAYING),
        CONTROLS(InterfaceID.Music.CONTROLS);

        final int packed;

        SnapTarget(int packed)
        {
            this.packed = packed;
        }
    }

    private final EnumMap<SnapTarget, ChildBackup> backups = new EnumMap<>(SnapTarget.class);

    private final List<Widget> overrideRootWidgets = new ArrayList<>();
    private final List<Widget> overrideScrollWidgets = new ArrayList<>();
    private String originalTitleText = null;

    @Getter private final Map<Widget, DropItem> iconItemMap = new LinkedHashMap<>();
    @Getter private boolean overrideActive = false;

    @Inject private MusicSearchButton musicSearchButton;
    private boolean hideObtainedItems = false;

    private final Map<Integer, Boolean> hiddenStateByPacked = new HashMap<>();

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
     * If an override is already active, it will be updated
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
        hideObtainedItems = false;

        runOnClientThread(this::revertOverride);
    }

    private void runOnClientThread(Runnable r)
    {
        if (client.isClientThread())
        {
            r.run();
        }
        else
        {
            clientThread.invoke(r);
        }
    }

    private Widget widget(int packed)
    {
        return client.getWidget(packed);
    }

    private void setHiddenRevalidate(Widget w, boolean hidden)
    {
        if (w == null)
        {
            return;
        }
        w.setHidden(hidden);
        w.revalidate();
    }

    private void revalidateScroll(Widget scrollbar)
    {
        if (scrollbar == null)
        {
            return;
        }
        scrollbar.revalidate();
        scrollbar.revalidateScroll();
    }

    private void rememberAndHidePacked(int packed)
    {
        Widget w = widget(packed);
        if (w == null)
        {
            return;
        }
        hiddenStateByPacked.putIfAbsent(packed, w.isHidden());
        setHiddenRevalidate(w, true);
    }

    private void rememberAndHideAll(int... packeds)
    {
        for (int p : packeds)
        {
            rememberAndHidePacked(p);
        }
    }

    private void restoreHiddenStates()
    {
        for (Map.Entry<Integer, Boolean> e : hiddenStateByPacked.entrySet())
        {
            Widget w = widget(e.getKey());
            if (w == null)
            {
                continue;
            }
            setHiddenRevalidate(w, Boolean.TRUE.equals(e.getValue()));
        }
        hiddenStateByPacked.clear();
    }

    private void forceShowCoreMusicControls()
    {
        for (int packed : RESTORE_FORCE_SHOW)
        {
            setHiddenRevalidate(widget(packed), false);
        }
    }

    private void revalidateAll(int... packeds)
    {
        for (int packed : packeds)
        {
            Widget w = widget(packed);
            if (w != null)
            {
                w.revalidate();
            }
        }
    }

    private void hardRevalidateMusicTab()
    {
        revalidateAll(
                InterfaceID.Music.CONTROLS,
                InterfaceID.Music.NOW_PLAYING,
                InterfaceID.Music.JUKEBOX,
                InterfaceID.Music.INNER,
                InterfaceID.Music.SCROLLABLE,
                InterfaceID.Music.CONTENTS,
                InterfaceID.Music.UNIVERSE
        );

        revalidateScroll(widget(InterfaceID.Music.SCROLLBAR));
    }

    private void settleMusicTab()
    {
        forceShowCoreMusicControls();
        hardRevalidateMusicTab();
    }

    private void updateIconsVisibilityAndLayout()
    {
        Set<Integer> obtainedIds = obtainedItemsManager.getObtainedItems();
        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);
        Widget scrollbar = widget(InterfaceID.Music.SCROLLBAR);

        int displayIndex = 0;

        for (Map.Entry<Widget, DropItem> e : iconItemMap.entrySet())
        {
            Widget icon = e.getKey();
            DropItem d = e.getValue();
            boolean obtained = obtainedIds.contains(d.getItemId());

            if (hideObtainedItems && obtained)
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
        revalidateScroll(scrollbar);
    }

    private static List<Widget> copyChildren(Widget parent, boolean dynamic)
    {
        if (parent == null)
        {
            return Collections.emptyList();
        }
        Widget[] kids = dynamic ? parent.getDynamicChildren() : parent.getChildren();
        if (kids == null)
        {
            return Collections.emptyList();
        }
        List<Widget> out = new ArrayList<>(kids.length);
        for (Widget k : kids)
        {
            if (k != null) out.add(k);
        }
        return out;
    }

    private void ensureBaselineCaptured()
    {
        for (SnapTarget t : SnapTarget.values())
        {
            ChildBackup b = backups.computeIfAbsent(t, k -> new ChildBackup());
            if (b.captured())
            {
                continue;
            }
            Widget w = widget(t.packed);
            b.stat = copyChildren(w, false);
            b.dyn = copyChildren(w, true);
        }
    }

    private static void hideChildren(Widget[] kids)
    {
        if (kids == null)
        {
            return;
        }
        for (Widget w : kids)
        {
            if (w != null) w.setHidden(true);
        }
    }

    private static void unhideAndRevalidate(List<Widget> kids)
    {
        if (kids == null)
        {
            return;
        }
        for (Widget w : kids)
        {
            if (w != null && w.getType() != 0)
            {
                w.setHidden(false);
                w.revalidate();
            }
        }
    }

    private static void restoreChildren(Widget parent, List<Widget> staticKids, List<Widget> dynamicKids)
    {
        if (parent == null)
        {
            return;
        }

        hideChildren(parent.getChildren());
        hideChildren(parent.getDynamicChildren());

        unhideAndRevalidate(staticKids);
        unhideAndRevalidate(dynamicKids);

        parent.revalidate();
    }

    private void restoreBaseline()
    {
        for (Map.Entry<SnapTarget, ChildBackup> e : backups.entrySet())
        {
            Widget w = widget(e.getKey().packed);
            ChildBackup b = e.getValue();
            restoreChildren(w, b.stat, b.dyn);
        }
        backups.clear();
    }

    private Widget updateTitle(NpcDropData dropData)
    {
        Widget title = widget(InterfaceID.Music.NOW_PLAYING_TITLE);
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

    private int absX(Widget root, Widget w)
    {
        int x = 0;
        Widget cur = w;
        while (cur != null && root != null && cur.getId() != root.getId())
        {
            x += cur.getOriginalX();
            int pid = cur.getParentId();
            if (pid == -1)
            {
                break;
            }
            cur = client.getWidget(pid);
        }
        return x;
    }

    private int absY(Widget root, Widget w)
    {
        int y = 0;
        Widget cur = w;
        while (cur != null && root != null && cur.getId() != root.getId())
        {
            y += cur.getOriginalY();
            int pid = cur.getParentId();
            if (pid == -1)
            {
                break;
            }
            cur = client.getWidget(pid);
        }
        return y;
    }

    private int clamp(int v, int min, int max)
    {
        return Math.max(min, Math.min(max, v));
    }

    private void drawProgressBarAndToggle(Widget root, Widget title, NpcDropData dropData, int obtainedCount, int totalDrops)
    {
        int fontId = title != null ? title.getFontId() : 0;
        boolean shadowed = title != null && title.getTextShadowed();

        final int CLOSE_SPRITE = 520;
        final int CLOSE_SIZE = 10;
        final int CLOSE_PAD  = 4;

        Widget close = root.createChild(-1);
        close.setHidden(false);
        close.setType(WidgetType.GRAPHIC);
        close.setOriginalX(CLOSE_PAD);
        close.setOriginalY(CLOSE_PAD);
        close.setOriginalWidth(CLOSE_SIZE);
        close.setOriginalHeight(CLOSE_SIZE);
        close.setSpriteId(CLOSE_SPRITE);
        close.setAction(0, "Close");
        close.setHasListener(true);
        close.setOnOpListener((JavaScriptCallback) (ScriptEvent ev) -> restore());
        close.revalidate();
        overrideRootWidgets.add(close);

        String lvlText = String.format("Lvl %d", dropData.getLevel());
        int lvlW = Math.max(60, (lvlText.length() * 6) + 8);

        int titleX = title != null ? absX(root, title) : 0;
        int titleY = title != null ? absY(root, title) : 0;
        int titleW = title != null ? title.getOriginalWidth() : 0;
        int titleH = title != null ? title.getOriginalHeight() : 0;

        Widget frame = widget(InterfaceID.Music.FRAME);
        int frameX = frame != null ? absX(root, frame) : 0;
        int frameW = frame != null ? frame.getOriginalWidth() : 0;
        int frameRight = frameW > 0 ? (frameX + frameW) : (titleX + titleW);

        int lvlX = frameRight - lvlW - PADDING + 15;
        int lvlY = titleY;
        lvlX = clamp(lvlX, titleX + titleW + (PADDING * 2), frameRight - 10);

        Widget lvl = root.createChild(-1);
        lvl.setHidden(false);
        lvl.setType(WidgetType.TEXT);
        lvl.setText(lvlText);
        lvl.setFontId(fontId);
        lvl.setTextShadowed(shadowed);
        lvl.setTextColor(0x00b33c);
        lvl.setOriginalX(lvlX);
        lvl.setOriginalY(lvlY);
        lvl.setOriginalWidth(lvlW);
        lvl.setOriginalHeight(titleH);
        lvl.revalidate();
        overrideRootWidgets.add(lvl);

        int barX = titleX;
        int barY = Math.max(0, titleY + titleH - 1);

        int rightLimit = lvlX - PADDING;
        int newW = Math.max(120, rightLimit - barX);

        Widget bg = root.createChild(-1);
        bg.setHidden(false);
        bg.setType(WidgetType.RECTANGLE);
        bg.setOriginalX(barX);
        bg.setOriginalY(barY);
        bg.setOriginalWidth(newW);
        bg.setOriginalHeight(BAR_HEIGHT);
        bg.setFilled(true);
        bg.setTextColor(0x000000);
        bg.revalidate();
        overrideRootWidgets.add(bg);

        final int border = 1;
        int innerWidth = newW - border * 2;
        int fillW = (totalDrops <= 0)
                ? 0
                : Math.round(innerWidth * (float) obtainedCount / totalDrops);

        Widget fill = root.createChild(-1);
        fill.setHidden(false);
        fill.setType(WidgetType.RECTANGLE);
        fill.setOriginalX(barX + border);
        fill.setOriginalY(barY + border);
        fill.setOriginalWidth(fillW);
        fill.setOriginalHeight(BAR_HEIGHT - border * 2);
        fill.setFilled(true);
        fill.setTextColor(0x00b33c);
        fill.revalidate();
        overrideRootWidgets.add(fill);

        String txt = String.format("%d/%d", obtainedCount, totalDrops);
        Widget label = root.createChild(-1);
        label.setHidden(false);
        label.setType(WidgetType.TEXT);
        label.setText(txt);
        label.setTextColor(0xFFFFFF);
        label.setFontId(fontId);
        label.setTextShadowed(shadowed);
        label.setOriginalWidth(newW);
        label.setOriginalHeight(BAR_HEIGHT);
        label.setOriginalX(barX + (newW / 2) - (txt.length() * 4));
        label.setOriginalY(barY + (BAR_HEIGHT / 2) - 6);
        label.revalidate();
        overrideRootWidgets.add(label);

        int eyeX = barX + newW + 4;
        int eyeY = barY + (BAR_HEIGHT / 2) - (EYE_SIZE / 2);

        Widget eye = root.createChild(-1);
        eye.setHidden(false);
        eye.setType(WidgetType.GRAPHIC);
        eye.setOriginalX(eyeX);
        eye.setOriginalY(eyeY);
        eye.setOriginalWidth(EYE_SIZE);
        eye.setOriginalHeight(EYE_SIZE);
        eye.setSpriteId(hideObtainedItems ? 2222 : 2221);
        eye.revalidate();
        eye.setAction(0, "Toggle obtained items");

        overrideRootWidgets.add(eye);
        eye.setOnOpListener((JavaScriptCallback) (ScriptEvent ev) ->
        {
            hideObtainedItems = !hideObtainedItems;
            updateIconsVisibilityAndLayout();
            eye.setSpriteId(hideObtainedItems ? 2222 : 2221);
            eye.revalidate();
        });
        eye.setHasListener(true);

        int searchX = eyeX + EYE_SIZE + PADDING;

        Widget search = root.createChild(-1);
        search.setHidden(false);
        search.setType(WidgetType.GRAPHIC);
        search.setOriginalX(searchX);
        search.setOriginalY(eyeY);
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

    private void hideOtherMusicUi()
    {
        rememberAndHidePacked(InterfaceID.Music.JUKEBOX);
        rememberAndHideAll(RESTORE_FORCE_VISIBLE_PACKEDS);

        Widget np = widget(InterfaceID.Music.NOW_PLAYING);
        Widget c = widget(InterfaceID.Music.CONTROLS);

        if (np != null)
        {
            WidgetUtils.hideAllChildrenSafely(np);
            np.revalidate();
        }
        if (c != null)
        {
            WidgetUtils.hideAllChildrenSafely(c);
            c.revalidate();
        }
    }

    private void drawDropIcons(Widget scrollable, Widget scrollbar, Widget jukebox, List<DropItem> drops, Set<Integer> obtainedIds)
    {
        if (scrollable == null || scrollbar == null)
        {
            return;
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
            icon.setOpacity(obtainedIds.contains(itemId) ? 0 : 150);
            icon.revalidate();

            iconItemMap.put(icon, d);
            overrideScrollWidgets.add(icon);
        }

        updateIconsVisibilityAndLayout();
    }

    private List<DropItem> buildDrops(NpcDropData dropData)
    {
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

        return WidgetUtils.dedupeAndSort(drops, config.sortDropsByRarity());
    }

    private void applyOverride(NpcDropData dropData)
    {
        ensureBaselineCaptured();
        purgeOverrideWidgets();
        hideOtherMusicUi();

        Widget root = widget(InterfaceID.Music.UNIVERSE);
        Widget title = updateTitle(dropData);

        List<DropItem> drops = buildDrops(dropData);

        Set<Integer> obtainedIds = obtainedItemsManager.getObtainedItems();
        int totalDrops = drops.size();
        int obtainedCount = (int) drops.stream()
                .filter(d -> obtainedIds.contains(d.getItemId()))
                .count();

        Widget scrollable = widget(InterfaceID.Music.SCROLLABLE);
        Widget jukebox = widget(InterfaceID.Music.JUKEBOX);
        Widget scrollbar = widget(InterfaceID.Music.SCROLLBAR);

        setHiddenRevalidate(scrollable, false);
        setHiddenRevalidate(scrollbar, false);

        if (root != null)
        {
            drawProgressBarAndToggle(root, title, dropData, obtainedCount, totalDrops);
        }

        drawDropIcons(scrollable, scrollbar, jukebox, drops, obtainedIds);

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

        purgeOverrideWidgets();
        restoreBaseline();

        restoreHiddenStates();
        settleMusicTab();

        clientThread.invokeLater(this::settleMusicTab);

        Widget title = widget(InterfaceID.Music.NOW_PLAYING_TITLE);
        if (title != null && originalTitleText != null)
        {
            title.setText(originalTitleText);
            title.revalidate();
        }

        originalTitleText = null;
        currentDrops = null;
        overrideActive = false;
        musicSearchButton.onOverrideDeactivated();
    }

    @Subscribe
    private void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() != MUSIC_GROUP)
        {
            return;
        }

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

            applyOverride(currentDrops);
        });
    }

    private void purgeOverrideWidgets()
    {
        purgeWidgets(overrideRootWidgets);
        purgeWidgets(overrideScrollWidgets);
        overrideRootWidgets.clear();
        overrideScrollWidgets.clear();
        iconItemMap.clear();
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
                w.setOriginalX(0);
                w.setOriginalY(0);
                w.setOriginalWidth(0);
                w.setOriginalHeight(0);
                w.setType(0);
                w.revalidate();
            } catch (Exception ignored) {}
        }
    }
}