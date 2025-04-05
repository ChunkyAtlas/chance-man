package com.chanceman;

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
     * Runs on the client thread. Iterates through item IDs 0–29999 and adds an item to allTradeableItems if:
     * - The item is tradeable and is not blocked by ItemsFilter.
     * - If the "freeToPlay" option is enabled, members-only items are skipped.
     * - If the "noFlatpacks", "noSets" or "noPoisoned" option is enabled, it blocks those items as well from being rollable
     * If the chanceManPanel is initialized, the panel is updated to reflect the refreshed list.
     */
    private void refreshTradeableItems()
    {
        clientThread.invokeLater(() -> {
            allTradeableItems.clear();
            int count = 0;
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
                    count++;
                }
            }
            if (chanceManPanel != null) {
                SwingUtilities.invokeLater(() -> chanceManPanel.updatePanel());
            }
        });
    }

    /**
     * Listens for configuration changes
     * When the "freeToPlay" option changes, it refreshes the list of tradeable items.
     * When the flatpack, sets or poisoned option changes, it refreshes tradeable items.
     */
    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event) {
        if (!event.getGroup().equals("chanceman")) {
            return;
        }
        if (event.getKey().equals("freeToPlay") || event.getKey().equals("noFlatpacks") || event.getKey().equals("noSets") || event.getKey().equals("NoPoisoned")) {
            refreshTradeableItems();
        }
    }

    /**
     * Processes game ticks, initializing managers and UI when the local player is available,
     * updating the roll animation.
     *
     * @param event The game tick event.
     */
    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (client.getLocalPlayer() != null && !itemsInitialized)
        {
            String playerName = client.getLocalPlayer().getName();
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
            refreshTradeableItems(); // Refresh once after initialization.
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

    /**
     * Listens for the GE search build script to be fired (script ID 751).
     * When triggered, it calls killSearchResults() to process the GE search results
     * and block any offers for items that are not unlocked.
     */
    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (event.getScriptId() == GE_SEARCH_BUILD_SCRIPT) {
            killSearchResults();
        }
    }

    /**
     * Retrieves the GE search results widget (ComponentID.CHATBOX_GE_SEARCH_RESULTS 162:51) and processes its dynamic children.
     * For each offer, if the offered item (child index 2) is locked, it hides the offer and reduces its opacity.
     */
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

    /**
     * Handles the event when an item spawns on the ground.
     * If the item has not been rolled (by item ID), it enqueues a roll.
     *
     * @param event The item spawned event.
     */
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
        // Convert to canonical (unnoted) ID
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

    /**
     * Handles inventory changes. When items are added to the inventory,
     * this method checks for items (by item ID) that have not been rolled and enqueues a roll.
     *
     * @param event The item container changed event.
     */
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

    /**
     * Handles menu option clicks.
     * For ground items, it consumes actions if the item is locked.
     * For inventory items, if the item is locked, only the "examine" and "drop" actions are allowed.
     *
     * @param event The menu option clicked event.
     */
    @Subscribe
    public void onMenuOptionClicked(net.runelite.api.events.MenuOptionClicked event)
    {
        String option = event.getMenuEntry().getOption().toLowerCase();
        // Handle ground item actions.
        if (event.getMenuAction() != null &&
                (event.getMenuAction().toString().contains("GROUND_ITEM") ||
                        option.contains("take") || option.contains("pick-up") || option.contains("pickup")))
        {
            int rawItemId = event.getId() != -1 ? event.getId() : event.getMenuEntry().getItemId();
            int canonicalGroundId = itemManager.canonicalize(rawItemId);
            if (isTradeable(canonicalGroundId) && !isNotTracked(canonicalGroundId)
                    && unlockedItemsManager != null && !unlockedItemsManager.isUnlocked(canonicalGroundId))
            {
                event.consume();
                return;
            }
        }
        // Handle inventory item actions.
        if (event.getMenuEntry().getItemId() != -1)
        {
            int rawItemId = event.getMenuEntry().getItemId();
            int canonicalId = itemManager.canonicalize(rawItemId);
            String itemName = getItemName(canonicalId).toLowerCase();
            // Skip non-tradeable items and special cases
            if (!isTradeable(canonicalId)
                    || isNotTracked(canonicalId)
                    || itemName.contains("coin pouch")
                    || itemName.contains("clue scroll"))
            {
                return;
            }
            // For locked inventory items, allow only "examine" and "drop" actions.
            if (unlockedItemsManager != null && !unlockedItemsManager.isUnlocked(canonicalId))
            {
                if (!option.equals("examine") && !option.equals("drop"))
                {
                    event.consume();
                }
            }
        }
    }

    private boolean isNormalWorld()
    {
        EnumSet<WorldType> worldTypes = client.getWorldType();
        return !(worldTypes.contains(WorldType.DEADMAN)
                || worldTypes.contains(WorldType.SEASONAL)
                || worldTypes.contains(WorldType.HIGH_RISK)
                || worldTypes.contains(WorldType.PVP));
    }

    private boolean isTradeable(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp != null && comp.isTradeable();
    }

    private boolean isNotTracked(int itemId)
    {
     return
     itemId == 995 || itemId == 13191 || itemId == 13190 || //Coins and Bonds
     itemId == 7588 || itemId == 1589 || itemId == 7590 || itemId == 7591; //Coffin from leo random
    }

    public String getItemName(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp != null ? comp.getName() : "Unknown";
    }
}
