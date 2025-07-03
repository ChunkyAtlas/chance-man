package com.chanceman.ui;

import com.chanceman.drops.NpcDropData;
import com.chanceman.drops.DropItem;
import com.chanceman.managers.RolledItemsManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.api.widgets.ItemQuantityMode;

@Slf4j
@Singleton
public class MusicWidgetController
{
    private static final int MUSIC_GROUP = 239;
    private final Client client;
    private final ClientThread clientThread;
    private final RolledItemsManager rolledItemsManager;
    private final SpriteOverrideManager spriteOverrideManager;
    private final ItemSpriteCache itemSpriteCache;

    private NpcDropData currentDrops = null;
    private List<Widget> backupJukeboxStaticKids = null;
    private List<Widget> backupJukeboxDynamicKids = null;
    private List<Widget> backupScrollStaticKids = null;
    private List<Widget> backupScrollDynamicKids = null;
    private String originalTitleText = null;
    @Getter private final Map<Widget, Integer> iconItemMap = new HashMap<>();
    @Getter private boolean overrideActive = false;

    @Inject
    public MusicWidgetController(
            Client client,
            ClientThread clientThread,
            RolledItemsManager rolledItemsManager,
            SpriteOverrideManager spriteOverrideManager,
            ItemSpriteCache itemSpriteCache
    )
    {
        this.client = client;
        this.clientThread = clientThread;
        this.rolledItemsManager = rolledItemsManager;
        this.spriteOverrideManager = spriteOverrideManager;
        this.itemSpriteCache = itemSpriteCache;
    }

    public boolean hasData()
    {
        return currentDrops != null;
    }

    public NpcDropData getCurrentData()
    {
        return currentDrops;
    }

    public void override(NpcDropData dropData)
    {
        this.currentDrops = dropData;
        this.overrideActive = true;
        applyOverride(dropData);
        spriteOverrideManager.register();
    }

    public void restore()
    {
        if (!overrideActive) return;
        spriteOverrideManager.unregister();
        itemSpriteCache.clear();
        revertOverride();
    }

    private void applyOverride(NpcDropData dropData)
    {
        iconItemMap.clear();
        int[] toHide = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19};
        for (int childId : toHide)
        {
            Widget w = client.getWidget(MUSIC_GROUP, childId);
            if (w != null) w.setHidden(true);
        }

        List<DropItem> drops = dropData.getDropTableSections().stream()
                .flatMap(sec -> sec.getItems().stream())
                .collect(Collectors.toList());
        drops = WidgetUtils.dedupeAndSort(drops);

        Set<Integer> rolledIds = rolledItemsManager.getRolledItems();
        int totalDrops = drops.size();
        int rolledCount = (int) drops.stream()
                .filter(d -> rolledIds.contains(d.getItemId()))
                .count();

        Widget root = client.getWidget(MUSIC_GROUP, 0);
        if (root != null)
        {
            Widget title = client.getWidget(MUSIC_GROUP, 8);
            if (title != null && originalTitleText == null) originalTitleText = title.getText();

            if (title != null && dropData != null)
            {
                title.setText(dropData.getName());
                title.revalidate();
            }

            int fontId = title != null ? title.getFontId() : 0;
            boolean shadowed = title != null && title.getTextShadowed();
            root.setHidden(false);
            root.setType(WidgetType.LAYER);
            root.revalidate();

            WidgetUtils.hideAllChildrenSafely(root);

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

            Widget oldBar = client.getWidget(MUSIC_GROUP, 9);
            if (oldBar == null) return;
            int xOld = oldBar.getOriginalX();
            int yOld = oldBar.getOriginalY();
            int wOld = oldBar.getOriginalWidth();
            int hOld = oldBar.getOriginalHeight();

            final int BAR_HEIGHT    = 15;
            final float WIDTH_RATIO = 0.7f;
            int newW  = Math.round(wOld * WIDTH_RATIO);

            int newY  = yOld + (hOld - BAR_HEIGHT) / 2;

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

            final int BORDER = 1;
            int innerWidth = newW - BORDER * 2;
            int fillW = Math.round(innerWidth * (float) rolledCount / totalDrops);

            Widget fill = root.createChild(-1);
            fill.setHidden(false);
            fill.setType(WidgetType.RECTANGLE);
            fill.setOriginalX(xOld + BORDER);
            fill.setOriginalY(newY + BORDER);
            fill.setOriginalWidth(fillW);
            fill.setOriginalHeight(BAR_HEIGHT - BORDER * 2);
            fill.setFilled(true);
            fill.setTextColor(0x00b33c);
            fill.revalidate();

            String txt = String.format("%d/%d", rolledCount, totalDrops);
            Widget label = root.createChild(-1);
            label.setHidden(false);
            label.setType(WidgetType.TEXT);
            label.setText(txt);
            label.setTextColor(0xFFFFFF);
            label.setFontId(title.getFontId());
            label.setTextShadowed(title.getTextShadowed());
            label.setOriginalWidth(newW);
            label.setOriginalHeight(BAR_HEIGHT);
            label.setOriginalX(xOld + (newW / 2) - (txt.length() * 4));
            label.setOriginalY(newY + (BAR_HEIGHT / 2) - 6);
            label.revalidate();

            root.revalidate();
        }

        final int ICON_SIZE = 32, PADDING = 4, COLUMNS = 4, MARGIN_X = 8, MARGIN_Y = 8;
        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        Widget jukebox    = client.getWidget(MUSIC_GROUP, 6);
        Widget overlay    = client.getWidget(MUSIC_GROUP, 5);
        Widget scrollbar  = client.getWidget(MUSIC_GROUP, 7);

        if (backupJukeboxStaticKids == null && jukebox != null)
        {
            Widget[] kids = jukebox.getChildren();
            backupJukeboxStaticKids = (kids != null)
                    ? new ArrayList<>(Arrays.asList(kids))
                    : new ArrayList<>();
        }

        // backup dynamic children of jukebox
        if (backupJukeboxDynamicKids == null && jukebox != null)
        {
            Widget[] kids = jukebox.getDynamicChildren();
            backupJukeboxDynamicKids = (kids != null)
                    ? new ArrayList<>(Arrays.asList(kids))
                    : new ArrayList<>();
        }

        // backup static children of scrollable
        if (backupScrollStaticKids == null && scrollable != null)
        {
            Widget[] kids = scrollable.getChildren();
            backupScrollStaticKids = (kids != null)
                    ? new ArrayList<>(Arrays.asList(kids))
                    : new ArrayList<>();
        }

        // backup dynamic children of scrollable
        if (backupScrollDynamicKids == null && scrollable != null)
        {
            Widget[] kids = scrollable.getDynamicChildren();
            backupScrollDynamicKids = (kids != null)
                    ? new ArrayList<>(Arrays.asList(kids))
                    : new ArrayList<>();
        }

        WidgetUtils.hideAllChildrenSafely(jukebox);
        WidgetUtils.hideAllChildrenSafely(scrollable);

        int displayIndex = 0;
        for (DropItem d : drops)
        {
            int itemId = d.getItemId();
            int col    = displayIndex % COLUMNS;
            int row    = displayIndex / COLUMNS;
            int x      = MARGIN_X + col * (ICON_SIZE + PADDING);
            int y      = MARGIN_Y + row * (ICON_SIZE + PADDING);

            Widget icon = Objects.requireNonNull(scrollable).createChild(-1);
            icon.setHidden(false);
            icon.setType(WidgetType.GRAPHIC);
            int spriteId = itemSpriteCache.getSpriteId(itemId);
            icon.setSpriteId(spriteId);
            icon.setItemQuantityMode(ItemQuantityMode.NEVER);
            icon.setOriginalX(x);
            icon.setOriginalY(y);
            icon.setOriginalWidth(ICON_SIZE);
            icon.setOriginalHeight(ICON_SIZE);
            icon.setOpacity(rolledIds.contains(itemId) ? 0 : 150);
            icon.revalidate();

            iconItemMap.put(icon, itemId);

            displayIndex++;
        }

        int rows = (displayIndex + COLUMNS - 1) / COLUMNS;
        Objects.requireNonNull(scrollable).setScrollHeight(MARGIN_Y * 2 + rows * (ICON_SIZE + PADDING));
        Objects.requireNonNull(scrollbar).revalidateScroll();
    }

    private void revertOverride()
    {
        if (!overrideActive)
        {
            return;
        }

        Widget root       = client.getWidget(MUSIC_GROUP, 0);
        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        Widget jukebox    = client.getWidget(MUSIC_GROUP, 6);

        // 1) Hide any dynamic widgets under the root (lvl text, bar, counter)
        if (root != null)
        {
            Widget[] dynRoot = root.getDynamicChildren();
            if (dynRoot != null)
            {
                for (Widget w : dynRoot)
                {
                    w.setHidden(true);
                    w.revalidate();
                }
            }
        }

        // 2) Hide injected drop icons under scrollable
        if (scrollable != null && backupScrollStaticKids != null && backupScrollDynamicKids != null)
        {
            Widget[] staticKids = scrollable.getChildren();
            if (staticKids != null) {for (Widget w : staticKids) {w.setHidden(true);}}

            Widget[] dynamicKids = scrollable.getDynamicChildren();
            if (dynamicKids != null) {for (Widget w : dynamicKids) {w.setHidden(true);}}
            for (Widget w : backupScrollStaticKids) {w.setHidden(false);}
            for (Widget w : backupScrollDynamicKids) {w.setHidden(false);}
            scrollable.revalidate();
        }

        if (jukebox != null && backupJukeboxStaticKids != null && backupJukeboxDynamicKids != null)
        {
            Widget[] jbStaticKids = jukebox.getChildren();
            if (jbStaticKids != null) {for (Widget w : jbStaticKids) {w.setHidden(true);}}

            Widget[] jbDynamicKids = jukebox.getDynamicChildren();
            if (jbDynamicKids != null) {for (Widget w : jbDynamicKids) {w.setHidden(true);}}
            for (Widget w : backupJukeboxStaticKids) {w.setHidden(false);}
            for (Widget w : backupJukeboxDynamicKids) {w.setHidden(false);}
            jukebox.revalidate();
        }

        Widget title     = client.getWidget(MUSIC_GROUP, 8);
        Widget overlay   = client.getWidget(MUSIC_GROUP, 5);
        Widget scrollbar = client.getWidget(MUSIC_GROUP, 7);
        Widget progress  = client.getWidget(MUSIC_GROUP, 9);

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
        currentDrops      = null;
        overrideActive    = false;
        backupJukeboxStaticKids = null;
        backupJukeboxDynamicKids = null;
        backupScrollStaticKids  = null;
        backupScrollDynamicKids = null;
        iconItemMap.clear();
    }
}