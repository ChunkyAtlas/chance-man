package com.chanceman.menus;

import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

@Getter
public enum EnabledUI
{
	BANK(InterfaceID.BANKMAIN, true, true),
	DEPOSIT_BOX(InterfaceID.BANK_DEPOSITBOX, true, true),
	CUSTOM_FUR_STORE(InterfaceID.HuntingCustomfurs.CONTENT, false, true),
	TTREK_REWARDS(InterfaceID.TREK_REWARDS,false, true),
	RUNEDOKU(InterfaceID.ROGUETRADER_SUDOKU, false, true);

	private final int id; // group id
	private final boolean greyLockedItems; // should grey/disable locked items in this UI
	private final boolean allowAllActions; // should bypass action restrictions in this UI

	EnabledUI(int id, boolean greyLockedItems, boolean allowAllActions)
	{
		this.id = (id > 0xFFFF) ? (id >>> 16) : id;
		this.greyLockedItems = greyLockedItems;
		this.allowAllActions = allowAllActions;
	}

	public static EnabledUI fromGroupId(int groupId)
	{
		for (EnabledUI ui : values())
		{
			if (ui.id == groupId) return ui;
		}
		return null;
	}
}
