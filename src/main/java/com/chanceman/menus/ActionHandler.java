package com.chanceman.menus;

import com.chanceman.ChanceManConfig;
import com.chanceman.ChanceManPlugin;
import com.chanceman.filters.EnsouledHeadMapping;
import com.chanceman.filters.BlockedItems;
import com.chanceman.managers.RolledItemsManager;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@Singleton
public class ActionHandler {

    private static final Set<MenuAction> disabledActions = EnumSet.of(
            MenuAction.CC_OP,               // inventory “Use” on locked
            MenuAction.WIDGET_TARGET,       // “Use” on widgets
            MenuAction.WIDGET_TARGET_ON_WIDGET  // “Use” on widget -> widget
    );

    private static final Set<MenuAction> GROUND_ACTIONS = EnumSet.of(
            MenuAction.GROUND_ITEM_FIRST_OPTION,
            MenuAction.GROUND_ITEM_SECOND_OPTION,
            MenuAction.GROUND_ITEM_THIRD_OPTION,
            MenuAction.GROUND_ITEM_FOURTH_OPTION,
            MenuAction.GROUND_ITEM_FIFTH_OPTION
    );

    private static final Set<Integer> ALWAYS_ALLOW_OBJECT_IDS = Set.of(ObjectID.CATABOW);

    private static final EnumSet<MenuAction> GAME_OBJECT_ACTIONS = EnumSet.of(
            MenuAction.GAME_OBJECT_FIRST_OPTION,
            MenuAction.GAME_OBJECT_SECOND_OPTION,
            MenuAction.GAME_OBJECT_THIRD_OPTION,
            MenuAction.GAME_OBJECT_FOURTH_OPTION,
            MenuAction.GAME_OBJECT_FIFTH_OPTION
    );

    private static final int ORBS_GROUP = (InterfaceID.Orbs.UNIVERSE >>> 16);
    // A no-op click handler that marks a menu entry as disabled.
    private final Consumer<MenuEntry> DISABLED = e -> {
    };
    @Inject
    private Client client;
    @Inject
    private EventBus eventBus;
    @Inject
    private ChanceManConfig config;
    @Inject
    private ChanceManPlugin plugin;
    @Inject
    private Restrictions restrictions;
    @Inject
    private RolledItemsManager rolledItemsManager;
    @Getter
    @Setter
    private int enabledUIOpen = -1;

    /**
     * A static helper to further safeguard ground item actions.
     * If a ground item is locked, this method consumes the event.
     */
    public static void handleGroundItems(
            ItemManager itemManager,
            RolledItemsManager rolledItemsManager,
            MenuOptionClicked event,
            ChanceManPlugin plugin) {
        if (event.getMenuAction() != null && GROUND_ACTIONS.contains(event.getMenuAction())) {
            int rawItemId = event.getId() != -1
                    ? event.getId()
                    : event.getMenuEntry().getItemId();
            int mapped = EnsouledHeadMapping.toTradeableId(rawItemId);
            int canonicalGroundId = itemManager.canonicalize(mapped);

            if (plugin.isGeTradeable(canonicalGroundId)
                    && !plugin.isNotTracked(canonicalGroundId)
                    && !BlockedItems.getBLOCKED_ITEMS().contains(canonicalGroundId)
                    && rolledItemsManager != null
                    && !rolledItemsManager.isRolled(canonicalGroundId))
                event.consume();
        }
    }

    /**
     * Normalize a MenuEntryAdded into the base item ID.
     */
    private int getItemId(MenuEntryAdded event, MenuEntry entry) {
        MenuAction type = entry.getType();
        boolean hasItemId = entry.getItemId() > 0 || event.getItemId() > 0;
        if (!GROUND_ACTIONS.contains(type) && !hasItemId) {
            return -1;
        }

        int raw = GROUND_ACTIONS.contains(type)
                ? event.getIdentifier()
                : Math.max(event.getItemId(), entry.getItemId());
        int mapped = EnsouledHeadMapping.toTradeableId(raw);
        return plugin.getItemManager().canonicalize(mapped);
    }

    public void startUp() {
        eventBus.register(this);
        eventBus.register(restrictions);
    }

    public void shutDown() {
        eventBus.unregister(this);
        eventBus.unregister(restrictions);
    }

    private boolean inactive() {
        return !rolledItemsManager.ready()
                || client.getGameState().getState() < GameState.LOADING.getState();
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == enabledUIOpen) enabledUIOpen = -1;
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (EnabledUI.fromGroupId(event.getGroupId()) != null)
            enabledUIOpen = event.getGroupId();
    }

    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event) {
        if (inactive()) return;

        EnabledUI ui = enabledUIOpen == -1 ? null : EnabledUI.fromGroupId(enabledUIOpen);
        if (ui != null && !ui.isGreyLockedItems()) return;

        MenuEntry entry = event.getMenuEntry();
        MenuAction action = entry.getType();
        int id = getItemId(event, entry);
        boolean enabled = GROUND_ACTIONS.contains(action)
                ? !isLockedGroundItem(id)
                : isEnabled(id, entry, action, ui);

        // If not enabled, grey out the text and set the click handler to DISABLED.
        if (!enabled) {
            String option = Text.removeTags(entry.getOption());
            String target = Text.removeTags(entry.getTarget());
            entry.setOption("<col=808080>" + option);
            entry.setTarget("<col=808080>" + target);
            entry.onClick(DISABLED);
            if (config.deprioritizeLockedOptions())
                entry.setDeprioritized(true);
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        // If the entry is disabled, consume the event.
        if (event.getMenuEntry().onClick() == DISABLED) {
            event.consume();
            return;
        }

        // Extra safeguard for ground items.
        handleGroundItems(plugin.getItemManager(), rolledItemsManager, event, plugin);
    }

    /**
     * @param itemId canonicalized item ID of a ground item
     * @return true if it’s tradeable, tracked, and still locked
     */
    private boolean isLockedGroundItem(int itemId) {
        return plugin.isGeTradeable(itemId)
                && !plugin.isNotTracked(itemId)
                && !BlockedItems.getBLOCKED_ITEMS().contains(itemId)
                && !rolledItemsManager.isRolled(itemId);
    }

    private boolean isHealthOrbCure(MenuEntry entry) {
        if (entry.getType() != MenuAction.CC_OP
                || !"cure".equalsIgnoreCase(Text.removeTags(entry.getOption())))
            return false;

        int w1 = entry.getParam1();
        int w0 = entry.getParam0();
        return (w1 >>> 16) == ORBS_GROUP || (w0 >>> 16) == ORBS_GROUP;
    }

    private boolean hasLockedShantayPass() {
        if (rolledItemsManager.isRolled(ItemID.SHANTAY_PASS)) return false;

        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory != null) {
            for (Item item : inventory.getItems())
                if (item.getId() == ItemID.SHANTAY_PASS) return true;
        }

        return false;
    }

    private boolean isShantayPassGateEntry(MenuEntry entry, String option) {
        if (!option.equalsIgnoreCase("go-through") || client.getLocalPlayer() == null)
            return false;

        int objectId = entry.getIdentifier();
        if (objectId != ObjectID.SHANTAY_PASS_HENGE_DOORWAY
                && objectId != ObjectID.TEMPOROSS_LOBBY_PASS)
            return false;

        WorldView worldView = client.getWorldView(entry.getWorldViewId());
        if (worldView == null) return false;

        WorldPoint player = client.getLocalPlayer().getWorldLocation();
        WorldPoint gate = WorldPoint.fromScene(
                worldView,
                entry.getParam0(),
                entry.getParam1(),
                worldView.getPlane()
        );

        if (objectId == ObjectID.SHANTAY_PASS_HENGE_DOORWAY)
            return player.getY() > gate.getY();

        return gate.getY() > 2830
                ? player.getX() <= 3193
                : player.getY() >= 2819;
    }

    /**
     * This method handles non-ground items (or any other cases) by checking if the item is enabled.
     * It returns true if the action should be allowed.
     */
    private boolean isEnabled(int id, MenuEntry entry, MenuAction action, EnabledUI ui) {
        if (isHealthOrbCure(entry)) return true;

        String option = Text.removeTags(entry.getOption());
        String target = Text.removeTags(entry.getTarget());

        if (ui != null && ui.isAllowAllActions()) return true;

        // Allowlisted world objects bypass all restrictions
        if (GAME_OBJECT_ACTIONS.contains(action)
                && ALWAYS_ALLOW_OBJECT_IDS.contains(entry.getIdentifier()))
            return true;

        if ((isShantayPassGateEntry(entry, option)
                || (target.equalsIgnoreCase("Shantay Guard")
                && (option.equalsIgnoreCase("talk-to") || option.equalsIgnoreCase("pass"))))
                && hasLockedShantayPass())
            return false;

        // Always allow "Drop" / "Check"
        if (option.equalsIgnoreCase("drop") || option.equalsIgnoreCase("check"))
            return true;

        if (option.equalsIgnoreCase("clean") || option.equalsIgnoreCase("rub"))
            return !plugin.isInPlay(id) || rolledItemsManager.isRolled(id);

        if ("harpoon".equalsIgnoreCase(option)
                && !hasAnyHarpoonInInvOrWorn()) {
            String t = target.toLowerCase();
            if (t.contains("fishing spot") || t.contains("spirit pool"))
                return true;
        }

        if (target.equalsIgnoreCase("Reward pool")
                && (option.equalsIgnoreCase("net") || option.equalsIgnoreCase("big-search")))
            return restrictions.isAnyNetEnabled();

        if (option.equalsIgnoreCase("net")
                && target.toLowerCase().contains("fishing spot"))
            return isNetFishingEnabled(entry);

        if (SkillOp.isSkillOp(option))
            return restrictions.isSkillOpEnabled(option);

        if (Spell.isSpell(option))
            return restrictions.isSpellOpEnabled(option);

        if (Spell.isSpell(target))
            return restrictions.isSpellOpEnabled(target);

        if (!disabledActions.contains(action)
                || id == 0
                || id == -1
                || !plugin.isInPlay(id))
            return true;

        return rolledItemsManager.isRolled(id);
    }

    private boolean isNetFishingEnabled(MenuEntry entry) {
        NPC npc = entry.getNpc();
        if (npc != null) {
            NPCComposition composition = npc.getTransformedComposition();
            if (composition == null) composition = npc.getComposition();

            String[] actions = composition.getActions();
            if (actions != null) {
                for (String npcAction : actions)
                    if ("Harpoon".equalsIgnoreCase(npcAction))
                        return restrictions.isSkillOpEnabled(SkillOp.BIG_NET.getOption());
            }
        }

        return restrictions.isSkillOpEnabled(SkillOp.SMALL_NET.getOption());
    }

    /**
     * Checks for harpoon in inventory and worn items for
     * barbarian fishing.
     **/
    private boolean hasAnyHarpoonInInvOrWorn() {
        return hasHarpoon(client.getItemContainer(InventoryID.WORN))
                || hasHarpoon(client.getItemContainer(InventoryID.INV));
    }

    private boolean hasHarpoon(ItemContainer container) {
        if (container != null) {
            for (Item item : container.getItems()) {
                SkillItem skillItem = SkillItem.fromId(item.getId());
                if (skillItem != null && skillItem.getSkillOp() == SkillOp.HARPOON)
                    return true;
            }
        }

        return false;
    }
}