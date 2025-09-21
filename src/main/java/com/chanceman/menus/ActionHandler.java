package com.chanceman.menus;

import com.chanceman.ChanceManConfig;
import com.chanceman.ChanceManPlugin;
import com.chanceman.filters.EnsouledHeadMapping;
import com.chanceman.managers.UnlockedItemsManager;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.HashSet;
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

	private final HashSet<Integer> enabledUIs = new HashSet<>() {{
		add(EnabledUI.BANK.getId());
		add(EnabledUI.DEPOSIT_BOX.getId());
	}};

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
	private UnlockedItemsManager unlockedItemsManager;
	@Getter
	@Setter
	private int enabledUIOpen = -1;

	// A no-op click handler that marks a menu entry as disabled.
	private final Consumer<MenuEntry> DISABLED = e -> { };

	public void startUp() {
		eventBus.register(this);
		eventBus.register(restrictions);
	}

	public void shutDown() {
		eventBus.unregister(this);
		eventBus.unregister(restrictions);
	}

	private boolean enabledUiOpen() {
		return enabledUIOpen != -1;
	}

	private boolean inactive() {
		if (!unlockedItemsManager.ready()) return true;
		return client.getGameState().getState() < GameState.LOADING.getState();
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event) {
		if (enabledUIs.contains(event.getGroupId()))
			enabledUIOpen = -1;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event) {
		if (enabledUIs.contains(event.getGroupId()))
			enabledUIOpen = event.getGroupId();
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event) {
		if (inactive()) return;

		MenuEntry entry = event.getMenuEntry();
		MenuAction action = entry.getType();
		int id = getItemId(event, entry);
		boolean enabled;
		// Check if the entry looks like it's for a ground item.
		if (isGroundItem(entry)) {
			enabled = !isLockedGroundItem(id);
		} else {
			enabled = isEnabled(id, entry, action);
		}
		// If not enabled, grey out the text and set the click handler to DISABLED.
		if (!enabled) {
			String option = Text.removeTags(entry.getOption());
			String target = Text.removeTags(entry.getTarget());
			entry.setOption("<col=808080>" + option);
			entry.setTarget("<col=808080>" + target);
			entry.onClick(DISABLED);
            if (config.deprioritizeLockedOptions()) {
                entry.setDeprioritized(true);
            }
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
		handleGroundItems(plugin.getItemManager(), unlockedItemsManager, event, plugin);
	}

	/**
	 * Returns true if the entry appears to be for a ground item.
	 */
	private boolean isGroundItem(MenuEntry entry)
	{
		return GROUND_ACTIONS.contains(entry.getType());
	}

	/**
	 * @param itemId canonicalized item ID of a ground item
	 * @return true if it’s tradeable, tracked, and still locked
	 */
	private boolean isLockedGroundItem(int itemId)
	{
		return plugin.isTradeable(itemId)
				&& !plugin.isNotTracked(itemId)
				&& !unlockedItemsManager.isUnlocked(itemId);
	}

	/**
	 * This method handles non-ground items (or any other cases) by checking if the item is enabled.
	 * It returns true if the action should be allowed.
	 */
	private boolean isEnabled(int id, MenuEntry entry, MenuAction action) {
		String option = Text.removeTags(entry.getOption());
		String target = Text.removeTags(entry.getTarget());

		// Always allow "Drop"
		if (option.equalsIgnoreCase("drop") || option.equalsIgnoreCase("check"))
			return true;
		if (option.equalsIgnoreCase("clean") || option.equalsIgnoreCase("rub"))
		{
			if (!plugin.isInPlay(id)) { return true; }
			return unlockedItemsManager.isUnlocked(id);
		}
		if (SkillOp.isSkillOp(option))
			return restrictions.isSkillOpEnabled(option);
		if (Spell.isSpell(option))
			return restrictions.isSpellOpEnabled(option);
		if (Spell.isSpell(target))
			return restrictions.isSpellOpEnabled(target);

		boolean enabled;
		if (enabledUiOpen()) {
			enabled = option.startsWith("Deposit") || option.startsWith("Examine") || option.startsWith("Withdraw")
					|| option.startsWith("Release") || option.startsWith("Destroy");
		} else {
			enabled = !disabledActions.contains(action);
		}
		if (enabled)
			return true;
		if (id == 0 || id == -1 || !plugin.isInPlay(id))
			return true;
		return unlockedItemsManager.isUnlocked(id);
	}

	/**
	 * A static helper to further safeguard ground item actions.
	 * If a ground item is locked, this method consumes the event.
	 */
	public static void handleGroundItems(ItemManager itemManager, UnlockedItemsManager unlockedItemsManager,
										 MenuOptionClicked event, ChanceManPlugin plugin) {
		if (event.getMenuAction() != null && GROUND_ACTIONS.contains(event.getMenuAction())) {
			int rawItemId = event.getId() != -1
					? event.getId()
					: event.getMenuEntry().getItemId();
			int mapped = EnsouledHeadMapping.toTradeableId(rawItemId);
			int canonicalGroundId = itemManager.canonicalize(mapped);
			if (plugin.isTradeable(canonicalGroundId)
					&& !plugin.isNotTracked(canonicalGroundId)
					&& unlockedItemsManager != null
					&& !unlockedItemsManager.isUnlocked(canonicalGroundId)) {
				event.consume();
			}
		}
	}
}
