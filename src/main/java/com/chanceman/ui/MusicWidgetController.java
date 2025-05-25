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

    private NpcDropData currentDrops = null;
    private List<Widget> backupJukeboxStaticKids = null;
    private List<Widget> backupJukeboxDynamicKids = null;
    private List<Widget> backupScrollStaticKids = null;
    private List<Widget> backupScrollDynamicKids = null;
    private String originalTitleText = null;
    @Getter private boolean overrideActive = false;

    @Inject
    public MusicWidgetController(
            Client client,
            ClientThread clientThread,
            RolledItemsManager rolledItemsManager,
            SpriteOverrideManager spriteOverrideManager
    )
    {
        this.client = client;
        this.clientThread = clientThread;
        this.rolledItemsManager = rolledItemsManager;
        this.spriteOverrideManager = spriteOverrideManager;
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
        revertOverride();
    }

    private void applyOverride(NpcDropData dropData)
    {

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

        if (backupJukeboxStaticKids == null)
        {
            backupJukeboxStaticKids = Optional.ofNullable(Objects.requireNonNull(jukebox).getChildren())
                    .map(Arrays::asList)
                    .map(ArrayList::new)
                    .orElseGet(ArrayList::new);
        }
        if (backupJukeboxDynamicKids == null)
        {
            backupJukeboxDynamicKids = Optional.ofNullable(Objects.requireNonNull(jukebox).getDynamicChildren())
                    .map(Arrays::asList)
                    .map(ArrayList::new)
                    .orElseGet(ArrayList::new);
        }
        if (backupScrollStaticKids == null)
        {
            backupScrollStaticKids = Optional.ofNullable(Objects.requireNonNull(scrollable).getChildren())
                    .map(Arrays::asList)
                    .map(ArrayList::new)
                    .orElseGet(ArrayList::new);
        }
        if (backupScrollDynamicKids == null)
        {
            backupScrollDynamicKids = Optional.ofNullable(Objects.requireNonNull(scrollable).getDynamicChildren())
                    .map(Arrays::asList)
                    .map(ArrayList::new)
                    .orElseGet(ArrayList::new);
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
            icon.setItemId(itemId);
            icon.setItemQuantityMode(ItemQuantityMode.NEVER);
            icon.setOriginalX(x);
            icon.setOriginalY(y);
            icon.setOriginalWidth(ICON_SIZE);
            icon.setOriginalHeight(ICON_SIZE);
            icon.setBorderType(1);
            icon.setOpacity(rolledIds.contains(itemId) ? 0 : 150);
            icon.revalidate();

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

            for (Widget w : scrollable.getChildren())
            {
                w.setHidden(true);
            }
            for (Widget w : scrollable.getDynamicChildren())
            {
                w.setHidden(true);
            }

            for (Widget w : backupScrollStaticKids)
            {
                w.setHidden(false);
            }
            for (Widget w : backupScrollDynamicKids)
            {
                w.setHidden(false);
            }
            scrollable.revalidate();
        }

        if (jukebox != null && backupJukeboxStaticKids != null && backupJukeboxDynamicKids != null)
        {
            for (Widget w : jukebox.getChildren())
            {
                w.setHidden(true);
            }
            for (Widget w : jukebox.getDynamicChildren())
            {
                w.setHidden(true);
            }
            for (Widget w : backupJukeboxStaticKids)
            {
                w.setHidden(false);
            }
            for (Widget w : backupJukeboxDynamicKids)
            {
                w.setHidden(false);
            }
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
    }
}