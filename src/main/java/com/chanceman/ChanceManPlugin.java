package com.chanceman;


import com.chanceman.account.AccountChanged;
import com.chanceman.account.AccountManager;
import com.chanceman.drops.DropFetcher;
import com.chanceman.filters.EnsouledHeadMapping;
import com.chanceman.menus.ActionHandler;
import com.chanceman.filters.ItemsFilter;
import com.chanceman.ui.DropsTabUI;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Getter;
import com.chanceman.managers.RollAnimationManager;
import com.chanceman.managers.RolledItemsManager;
import com.chanceman.managers.UnlockedItemsManager;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.eventbus.EventBus;
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
    @Inject
    private AccountManager accountManager;
    @Inject
    private UnlockedItemsManager unlockedItemsManager;
    @Inject
    private RolledItemsManager rolledItemsManager;
    @Inject
    private RollAnimationManager rollAnimationManager;
    @Inject
    private EventBus eventBus;
    @Inject
    private ItemsFilter itemsFilter;
    @Inject private DropsTabUI dropsTabUI;
    @Inject private DropFetcher dropFetcher;
    
    private ChanceManPanel chanceManPanel;
    private NavigationButton navButton;
    private ExecutorService fileExecutor;
    @Getter private final HashSet<Integer> allTradeableItems = new LinkedHashSet<>();
    private static final int GE_SEARCH_BUILD_SCRIPT = 751;
    private boolean tradeableItemsInitialized = false;
    private boolean featuresActive = false;

    @Provides
    ChanceManConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(ChanceManConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        eventBus.register(this);
        if (isNormalWorld()) enableFeatures();
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (featuresActive) disableFeatures();
        eventBus.unregister(this);
    }

    private void enableFeatures()
    {
        if (featuresActive) return;
        featuresActive = true;

        getInjector().getInstance(ActionHandler.class).startUp();
        eventBus.register(accountManager);
        overlayManager.add(chanceManOverlay);
        fileExecutor = Executors.newSingleThreadExecutor();
        unlockedItemsManager.setExecutor(fileExecutor);
        rolledItemsManager.setExecutor(fileExecutor);
        rollAnimationManager.startUp();
        dropsTabUI.startUp();

        chanceManPanel = new ChanceManPanel(
                unlockedItemsManager,
                rolledItemsManager,
                itemManager,
                allTradeableItems,
                clientThread,
                rollAnimationManager
        );
        rollAnimationManager.setChanceManPanel(chanceManPanel);

        BufferedImage icon = ImageUtil.loadImageResource(
                getClass(), "/net/runelite/client/plugins/chanceman/icon.png"
        );
        navButton = NavigationButton.builder()
                .tooltip("ChanceMan")
                .icon(icon)
                .priority(5)
                .panel(chanceManPanel)
                .build();
        clientToolbar.addNavigation(navButton);

        accountManager.init();
    }

    private void disableFeatures()
    {
        if (!featuresActive) return;
        featuresActive = false;
        dropsTabUI.shutDown();
        eventBus.unregister(accountManager);
        getInjector().getInstance(ActionHandler.class).shutDown();

        if (clientToolbar != null && navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
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
            fileExecutor = null;
        }
        dropFetcher.shutdown();

        // reset panel/tradeable state
        chanceManPanel = null;
        allTradeableItems.clear();
        tradeableItemsInitialized = false;
        accountManager.reset();
    }

    @Subscribe
    public void onWorldChanged(WorldChanged event)
    {
        if (isNormalWorld()) enableFeatures();
        else disableFeatures();
    }

    /**
     * Refreshes the list of tradeable item IDs based on the current configuration.
     */
    public void refreshTradeableItems() {
        clientThread.invokeLater(() -> {
            allTradeableItems.clear();
            for (int i = 0; i < 40000; i++) {
                ItemComposition comp = itemManager.getItemComposition(i);
                if (comp != null && comp.isTradeable() && !isNotTracked(i)
                        && !ItemsFilter.isBlocked(i, config)) {
                    if (config.freeToPlay() && comp.isMembers()) {
                        continue;
                    }
                    if (!ItemsFilter.isPoisonEligible(i, config.requireWeaponPoison(),
                            unlockedItemsManager.getUnlockedItems())) {
                        continue;
                    }
                    allTradeableItems.add(i);
                }
            }
            rollAnimationManager.setAllTradeableItems(allTradeableItems);
            if (chanceManPanel != null) {
                SwingUtilities.invokeLater(() -> chanceManPanel.updatePanel());
            }
        });
    }


    @Subscribe
    public void onConfigChanged(net.runelite.client.events.ConfigChanged event)
    {
        if (!featuresActive) return;
        if (!event.getGroup().equals("chanceman")) return;
        switch (event.getKey())
        {
            case "freeToPlay":
            case "includeF2PTradeOnlyItems":
            case "enableFlatpacks":
            case "enableItemSets":
            case "requireWeaponPoison":
                refreshTradeableItems();
                break;
        }
    }

    @Subscribe
    private void onAccountChanged(AccountChanged event)
    {
        if (!featuresActive) return;
        unlockedItemsManager.loadUnlockedItems();
        rolledItemsManager.loadRolledItems();
        if (chanceManPanel != null)
        {
            SwingUtilities.invokeLater(() -> chanceManPanel.updatePanel());
        }
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        if (!featuresActive) return;
        if (!tradeableItemsInitialized && client.getGameState() == GameState.LOGGED_IN)
        {
            refreshTradeableItems();
            tradeableItemsInitialized = true;
        }

        rollAnimationManager.process();
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event)
    {
        if (!featuresActive) return;
        if (event.getScriptId() == GE_SEARCH_BUILD_SCRIPT) { killSearchResults(); }
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
        if (!featuresActive) return;
        if (!accountManager.ready()) return;

        TileItem tileItem = (TileItem) event.getItem();
        int itemId = tileItem.getId();
        ItemComposition comp = itemManager.getItemComposition(itemId);
        String name = (comp != null && comp.getName() != null) ? comp.getName() : tileItem.toString();
        if (name.toLowerCase().contains("ensouled")) {
            int mappedId = ItemsFilter.getEnsouledHeadId(name);
            if (mappedId != EnsouledHeadMapping.DEFAULT_ENSOULED_HEAD_ID) { itemId = mappedId; }
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
            rollAnimationManager.enqueueRoll(canonicalItemId);
            rolledItemsManager.markRolled(canonicalItemId);
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!featuresActive) return;
        if (!accountManager.ready()) return;

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
                    rollAnimationManager.enqueueRoll(canonicalId);
                    rolledItemsManager.markRolled(canonicalId);
                    processed.add(canonicalId);
                }
            }
        }
    }

    public boolean isNormalWorld()
    {
        EnumSet<WorldType> worldTypes = client.getWorldType();
        return !(worldTypes.contains(WorldType.DEADMAN)
                || worldTypes.contains(WorldType.SEASONAL)
                || worldTypes.contains(WorldType.BETA_WORLD)
                || worldTypes.contains(WorldType.PVP_ARENA)
                || worldTypes.contains(WorldType.QUEST_SPEEDRUNNING)
                || worldTypes.contains(WorldType.TOURNAMENT_WORLD));
    }

    public boolean isTradeable(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp != null && comp.isTradeable();
    }

    public boolean isNotTracked(int itemId)
    {
        return itemId == 995 || itemId == 13191 || itemId == 13190 ||
                itemId == 7587 || itemId == 7588 || itemId == 7589 || itemId == 7590 || itemId == 7591;
    }

    public boolean isInPlay(int itemId)
    {
        return allTradeableItems.contains(itemId);
    }

    public ItemManager getItemManager() { return itemManager; }

}
