package com.chanceman.menus;

import com.chanceman.ChanceManPlugin;
import com.chanceman.managers.RolledItemsManager;
import com.chanceman.account.AccountManager;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;

@Singleton
public class Restrictions
{
	private static final int[] RUNE_POUCH_TYPE_VARBITS = {
			29,    // RUNE_POUCH_RUNE1
			1622,  // RUNE_POUCH_RUNE2
			1623,  // RUNE_POUCH_RUNE3
			14285, // RUNE_POUCH_RUNE4
			15373, // RUNE_POUCH_RUNE5
			15374  // RUNE_POUCH_RUNE6
	};

	private static final int[] RUNE_POUCH_AMOUNT_VARBITS = {
			1624,  // RUNE_POUCH_AMOUNT1
			1625,  // RUNE_POUCH_AMOUNT2
			1626,  // RUNE_POUCH_AMOUNT3
			14286, // RUNE_POUCH_AMOUNT4
			15375, // RUNE_POUCH_AMOUNT5
			15376  // RUNE_POUCH_AMOUNT6
	};

	private static final WorldArea FOUNTAIN_OF_RUNE_AREA =
			new WorldArea(3367, 3890, 13, 9, 0);

	private boolean isInFountainArea()
	{
		Player player = client.getLocalPlayer();
		if (player == null) return false;
		WorldPoint lp = player.getWorldLocation();
		return FOUNTAIN_OF_RUNE_AREA.contains(lp);
	}

	private boolean isInLMS()
	{
		EnumSet<WorldType> worldTypes = client.getWorldType();
		return (worldTypes.contains(WorldType.LAST_MAN_STANDING));

	}

	public static final int SPELL_REQUIREMENT_OVERLAY_NORMAL = InterfaceID.MagicSpellbook.TOOLTIP;
	public static final int AUTOCAST_REQUIREMENT_OVERLAY_NORMAL = InterfaceID.Autocast.INFO;

	@Inject private ChanceManPlugin plugin;
	@Inject private Client client;
	@Inject private RolledItemsManager rolledItemsManager;
	@Inject private AccountManager accountManager;
	private final Set<SkillOp> enabledSkillOps = EnumSet.noneOf(SkillOp.class);
	private final HashSet<Integer> availableRunes = new HashSet<>();

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!accountManager.ready()) return;
		enabledSkillOps.clear();
		availableRunes.clear();

		ItemContainer equippedItems = client.getItemContainer(InventoryID.WORN);
		ItemContainer inventoryItems = client.getItemContainer(InventoryID.INV);

		if (equippedItems != null)
		{
			Arrays.stream(equippedItems.getItems()).forEach(item -> {
				int id = item.getId();
				SkillItem skillItem = SkillItem.fromId(id);
				if (skillItem != null && !skillItem.isRequiresUnlock())
				{
					enabledSkillOps.add(skillItem.getSkillOp());
					return;
				}

				if (shouldSkipItem(id)) return;
				if (RuneProvider.isEquippedProvider(id))
					availableRunes.addAll(RuneProvider.getProvidedRunes(id));
				if (skillItem != null) enabledSkillOps.add(skillItem.getSkillOp());
			});
		}

		if (inventoryItems != null)
		{
			Arrays.stream(inventoryItems.getItems()).forEach(item -> {
				int id = item.getId();
				SkillItem skillItem = SkillItem.fromId(id);
				if (skillItem != null && !skillItem.isRequiresUnlock())
				{
					enabledSkillOps.add(skillItem.getSkillOp());
					return;
				}

				if (shouldSkipItem(id)) return;
				if (RuneProvider.isInvProvider(id))
					availableRunes.addAll(RuneProvider.getProvidedRunes(id));
				if (skillItem != null) enabledSkillOps.add(skillItem.getSkillOp());
			});
		}

		EnumComposition pouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		for (int i = 0; i < 6; i++)
		{
			int qty     = client.getVarbitValue(RUNE_POUCH_AMOUNT_VARBITS[i]);
			int typeIdx = client.getVarbitValue(RUNE_POUCH_TYPE_VARBITS[i]);
			if (qty <= 0)
			{
				continue;
			}

			int runeId = pouchEnum.getIntValue(typeIdx);
			if (shouldSkipItem(runeId))
			{
				continue;
			}

			if (RuneProvider.isInvProvider(runeId))
			{
				availableRunes.addAll(RuneProvider.getProvidedRunes(runeId));
			}
		}
	}

	public boolean isSkillOpEnabled(String option)
	{
		SkillOp op = SkillOp.fromString(option);
		return enabledSkillOps.contains(op);
	}

	public boolean isBlackjackEnabled()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		Item weapon = equipment == null
				? null
				: equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		SkillItem blackjack = weapon == null ? null : SkillItem.fromId(weapon.getId());
		if (blackjack == null) return false;

		switch (blackjack)
		{
			case OAK_BLACKJACK:
			case OAK_BLACKJACK_O:
			case OAK_BLACKJACK_D:
			case WILLOW_BLACKJACK:
			case WILLOW_BLACKJACK_O:
			case WILLOW_BLACKJACK_D:
			case MAPLE_BLACKJACK:
			case MAPLE_BLACKJACK_O:
			case MAPLE_BLACKJACK_D:
				return !blackjack.isRequiresUnlock() || !shouldSkipItem(weapon.getId());
			default:
				return false;
		}
	}

	public boolean isFlyFishingEnabled()
	{
		return isInventoryItemEnabled(ItemID.FLY_FISHING_ROD)
				|| hasInventoryItem(ItemID.FISHINGROD_PEARL_FLY)
				|| hasEquippedItem(ItemID.FISHINGROD_PEARL_FLY, EquipmentInventorySlot.WEAPON);
	}

	public boolean isBaitFishingEnabled()
	{
		return isBaitFishingRodEnabled() && isInventoryItemEnabled(ItemID.FISHING_BAIT);
	}

	public boolean isAnglerfishFishingEnabled()
	{
		int bait = hasInventoryItem(ItemID.DIABOLIC_WORMS)
				? ItemID.DIABOLIC_WORMS
				: ItemID.PISCARILIUS_SANDWORMS;
		return isBaitFishingRodEnabled() && isInventoryItemEnabled(bait);
	}

	public boolean isBarroniteSmithingEnabled()
	{
		return isInventoryItemEnabled(ItemID.HAMMER)
				|| hasInventoryItem(ItemID.IMCANDO_HAMMER)
				|| hasInventoryItem(ItemID.IMCANDO_HAMMER_OFFHAND)
				|| hasEquippedItem(ItemID.IMCANDO_HAMMER, EquipmentInventorySlot.WEAPON)
				|| hasEquippedItem(ItemID.IMCANDO_HAMMER_OFFHAND, EquipmentInventorySlot.SHIELD);
	}

	private boolean isBaitFishingRodEnabled()
	{
		return isInventoryItemEnabled(ItemID.FISHING_ROD)
				|| hasInventoryItem(ItemID.OILY_FISHING_ROD)
				|| hasInventoryItem(ItemID.FISHINGROD_PEARL)
				|| hasInventoryItem(ItemID.FISHINGROD_PEARL_OILY)
				|| hasEquippedItem(ItemID.FISHINGROD_PEARL, EquipmentInventorySlot.WEAPON)
				|| hasEquippedItem(ItemID.FISHINGROD_PEARL_OILY, EquipmentInventorySlot.WEAPON);
	}

	private boolean isInventoryItemEnabled(int itemId)
	{
		return hasInventoryItem(itemId) && !shouldSkipItem(itemId);
	}

	private boolean hasInventoryItem(int itemId)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		return inventory != null && inventory.contains(itemId);
	}

	private boolean hasEquippedItem(int itemId, EquipmentInventorySlot slot)
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		Item item = equipment == null ? null : equipment.getItem(slot.getSlotIdx());
		return item != null && item.getId() == itemId;
	}

	public boolean isAnyNetEnabled()
	{
		return enabledSkillOps.contains(SkillOp.SMALL_NET)
				|| enabledSkillOps.contains(SkillOp.BIG_NET);
	}

	public boolean isNetTrapEnabled()
	{
		return isInventoryItemEnabled(ItemID.NET_TRAP);
	}

	private boolean shouldSkipItem(int id)
	{
		if (!plugin.isInPlay(id))
		{
			return !(RuneProvider.isEquippedProvider(id)
					|| RuneProvider.isInvProvider(id));
		}
		return !rolledItemsManager.isRolled(id);
	}

	public boolean isSpellOpEnabled(String spellName)
	{
		if (isInFountainArea() || isInLMS()) { return true; }
		BlightedSack sack = BlightedSack.fromSpell(spellName);
		if (sack != null)
		{
			int sackId = sack.getSackItemId();
			ItemContainer inv = client.getItemContainer(InventoryID.INV);
			if (inv != null	&& (sackId == ItemID.BLIGHTED_SACK_SURGE || rolledItemsManager.isRolled(sackId)))
			{
				for (Item item : inv.getItems())
				{
					if (item.getId() == sackId)
					{
						return true;
					}
				}
			}
		}

		Widget autocastOverlay = client.getWidget(AUTOCAST_REQUIREMENT_OVERLAY_NORMAL);
		if (autocastOverlay != null) return processChildren(autocastOverlay);

		Widget spellOverlay = client.getWidget(SPELL_REQUIREMENT_OVERLAY_NORMAL);
		if (spellOverlay != null) return processChildren(spellOverlay);
		return false;
	}

	public boolean processChildren(Widget widget)
	{
		Widget[] children = widget.getDynamicChildren();
		if (children == null) return true;

		for (Widget child : children)
		{
			if (child == null) continue;
			int id = child.getItemId();
			if (id == -1) continue;

			if (plugin.isInPlay(id) && !availableRunes.contains(id))
				return false;
		}
		return true;
	}
}