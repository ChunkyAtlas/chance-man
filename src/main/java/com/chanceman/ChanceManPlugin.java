package com.chanceman;

import com.chanceman.helpers.ChanceManSpellHelper;
import com.chanceman.helpers.ToolActionHandler;
import com.chanceman.helpers.SpellActionHandler;
import com.chanceman.helpers.InventoryActionHandler;
import com.chanceman.helpers.ToolActionMapping;
import com.google.gson.Gson;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.TileItem;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.config.ConfigManager;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@PluginDescriptor(
        name = "ChanceMan",
        description = "Locks tradeable items until unlocked via a random roll.",
        tags = {"osrs", "chance", "roll", "lock", "unlock"}
)
@Singleton
public class ChanceManPlugin extends Plugin
{
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private ItemManager itemManager;
    @Inject
    private ChanceManOverlay chanceManOverlay;
    @Inject
    private Gson gson;
    @Inject
    private ChanceManConfig config;
    @Inject
    private ConfigManager configManager;

    private UnlockedItemsManager unlockedItemsManager;
    private RolledItemsManager rolledItemsManager;
    private RollAnimationManager rollAnimationManager;
    private ChanceManPanel chanceManPanel;
    private NavigationButton navButton;
    private ExecutorService fileExecutor;
    private ChanceManSpellHelper spellHelper;
    private final List<Integer> allTradeableItems = new ArrayList<>();
    private static final int GE_SEARCH_BUILD_SCRIPT = 751;
    private boolean itemsInitialized = false;

    @Provides
    ChanceManConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ChanceManConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        if (!isNormalWorld())
        {
            return;
        }
        fileExecutor = Executors.newSingleThreadExecutor();
        refreshTradeableItems();
        // Managers and UI are initialized on the first game tick.
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (clientToolbar != null && navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
        }
        if (overlayManager != null)
        {
            overlayManager.remove(chanceManOverlay);
        }
        if (rollAnimationManager != null)
        {
            rollAnimationManager.shutdown();
        }
        if (fileExecutor != null)
        {
            fileExecutor.shutdownNow();
        }
        // Reset plugin state for a fresh initialization on restart.
        itemsInitialized = false;
        unlockedItemsManager = null;
        rolledItemsManager = null;
        rollAnimationManager = null;
        chanceManPanel = null;
        navButton = null;
        fileExecutor = null;
        allTradeableItems.clear();
    }

    /**
     * Refreshes the list of tradeable item IDs based on the current configuration.
     */
    private void refreshTradeableItems()
    {
        clientThread.invokeLater(() -> {
            allTradeableItems.clear();
            for (int i = 0; i < 30000; i++)
            {
                ItemComposition comp = itemManager.getItemComposition(i);
                if (comp != null && comp.isTradeable() && !isNotTracked(i) && !ItemsFilter.isBlocked(comp.getName()))
                {
                    if (config.freeToPlay() && comp.isMembers())
                    {
                        continue;
                    }
                    if (config.noFlatpacks() && ItemsFilter.isFpBlocked(comp.getName()) || config.noSets() && ItemsFilter.isSetBlocked(comp.getName()) || config.noPoisoned() && ItemsFilter.isPoisonedBlocked(comp.getName()))
                    {
                        continue;
                    }                 
                    allTradeableItems.add(i);
                }
            }
            if (chanceManPanel != null) {
                SwingUtilities.invokeLater(() -> chanceManPanel.updatePanel());
            }
        });
    }

    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event) {
        if (!event.getGroup().equals("chanceman")) {
            return;
        }
        if (event.getKey().equals("freeToPlay") || event.getKey().equals("noFlatpacks") || event.getKey().equals("noSets") || event.getKey().equals("NoPoisoned")) {
            refreshTradeableItems();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getLocalPlayer() != null && !itemsInitialized)
        {
            String playerName = client.getLocalPlayer().getName();
            spellHelper = new ChanceManSpellHelper(gson);
            unlockedItemsManager = new UnlockedItemsManager(playerName, gson, fileExecutor);
            unlockedItemsManager.loadUnlockedItems();
            rolledItemsManager = new RolledItemsManager(playerName, gson, fileExecutor);
            rolledItemsManager.loadRolledItems();
            rollAnimationManager = new RollAnimationManager(
                    unlockedItemsManager,
                    chanceManOverlay,
                    allTradeableItems,
                    itemManager,
                    client,
                    chatMessageManager,
                    this,
                    clientThread
            );
            chanceManPanel = new ChanceManPanel(
                    unlockedItemsManager,
                    rolledItemsManager,
                    itemManager,
                    allTradeableItems,
                    clientThread,
                    rollAnimationManager
            );
            BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/net/runelite/client/plugins/chanceman/icon.png");
            navButton = NavigationButton.builder()
                    .tooltip("ChanceMan")
                    .icon(icon)
                    .priority(5)
                    .panel(chanceManPanel)
                    .build();
            clientToolbar.addNavigation(navButton);
            overlayManager.add(chanceManOverlay);
            refreshTradeableItems();
            itemsInitialized = true;
        }
        if (rollAnimationManager != null)
        {
            rollAnimationManager.process();
        }
        if (chanceManPanel != null)
        {
            SwingUtilities.invokeLater(() -> chanceManPanel.updatePanel());
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == GE_SEARCH_BUILD_SCRIPT) {
            killSearchResults();
        }
    }

    private void killSearchResults() {
        Widget geSearchResults = client.getWidget(162, 51);
        if (geSearchResults == null) {
            return;
        }
        Widget[] children = geSearchResults.getDynamicChildren();
        if (children == null || children.length < 2 || children.length % 3 != 0) {
            return;
        }
        Set<Integer> unlocked = unlockedItemsManager.getUnlockedItems();
        for (int i = 0; i < children.length; i += 3) {
            int offerItemId = children[i + 2].getItemId();
            if (!unlocked.contains(offerItemId)) {
                children[i].setHidden(true);
                children[i + 1].setOpacity(70);
                children[i + 2].setOpacity(70);
            }
        }
    }

    @Subscribe
    public void onItemSpawned(ItemSpawned event)
    {
        if (!isNormalWorld())
        {
            return;
        }
        TileItem tileItem = (TileItem) event.getItem();
        int itemId = tileItem.getId();
        ItemComposition comp = itemManager.getItemComposition(itemId);
        String name = (comp != null && comp.getName() != null) ? comp.getName() : tileItem.toString();
        if (name.toLowerCase().contains("ensouled"))
        {
            int mappedId = ItemsFilter.getEnsouledHeadId(name);
            if (mappedId != ItemsFilter.DEFAULT_ENSOULED_HEAD_ID)
            {
                itemId = mappedId;
            }
        }
        int canonicalItemId = itemManager.canonicalize(itemId);
        if (!isTradeable(canonicalItemId) || isNotTracked(canonicalItemId))
        {
            return;
        }
        if (tileItem.getOwnership() != TileItem.OWNERSHIP_SELF)
        {
            return;
        }
        if (rolledItemsManager == null)
        {
            return;
        }
        if (!rolledItemsManager.isRolled(canonicalItemId))
        {
            if (rollAnimationManager != null)
            {
                rollAnimationManager.enqueueRoll(canonicalItemId);
            }
            rolledItemsManager.markRolled(canonicalItemId);
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (rolledItemsManager == null)
        {
            return;
        }
        if (event.getContainerId() == 93)
        {
            Set<Integer> processed = new HashSet<>();
            for (net.runelite.api.Item item : event.getItemContainer().getItems())
            {
                int rawItemId = item.getId();
                int canonicalId = itemManager.canonicalize(rawItemId);
                if (!isTradeable(canonicalId) || isNotTracked(canonicalId))
                {
                    continue;
                }
                if (!processed.contains(canonicalId) && !rolledItemsManager.isRolled(canonicalId))
                {
                    if (rollAnimationManager != null)
                    {
                        rollAnimationManager.enqueueRoll(canonicalId);
                    }
                    rolledItemsManager.markRolled(canonicalId);
                    processed.add(canonicalId);
                }
            }
        }
    }

    @Subscribe
    public void onMenuOptionClicked(net.runelite.api.events.MenuOptionClicked event)
    {
        String option = event.getMenuEntry().getOption().toLowerCase();

        // 1. Handle tool/skilling actions.
        if (!ToolActionHandler.handleToolAction(client, itemManager, unlockedItemsManager, ToolActionMapping.getToolActionMap(), event))
        {
            return;
        }

        // 2. Handle spell casting actions.
        if (!SpellActionHandler.handleSpellAction(client, itemManager, unlockedItemsManager, spellHelper, event))
        {
            return;
        }

        // 3. Handle direct inventory actions (including ground items).
        InventoryActionHandler.handleInventoryAction(itemManager, unlockedItemsManager, event, this);
    }

    public boolean isNormalWorld()
    {
        EnumSet<WorldType> worldTypes = client.getWorldType();
        return !(worldTypes.contains(WorldType.DEADMAN)
                || worldTypes.contains(WorldType.SEASONAL)
                || worldTypes.contains(WorldType.HIGH_RISK)
                || worldTypes.contains(WorldType.PVP));
    }

    public boolean isTradeable(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp != null && comp.isTradeable();
    }

    public boolean isNotTracked(int itemId)
    {
        return itemId == 995 || itemId == 13191 || itemId == 13190 ||
                itemId == 7588 || itemId == 1589 || itemId == 7590 || itemId == 7591;
    }

    public String getItemName(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp != null ? comp.getName() : "Unknown";
    }
}
