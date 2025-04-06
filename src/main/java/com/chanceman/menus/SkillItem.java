package com.chanceman.menus;

import lombok.Getter;

import java.util.HashSet;

public enum SkillItem
{

	BRONZE_AXE(1351, SkillOp.CHOP_DOWN),
	IRON_AXE(1349, SkillOp.CHOP_DOWN),
	STEEL_AXE(1353, SkillOp.CHOP_DOWN),
	BLACK_AXE(1361, SkillOp.CHOP_DOWN),
	MITHRIL_AXE(1355, SkillOp.CHOP_DOWN),
	ADAMANT_AXE(1357, SkillOp.CHOP_DOWN),
	RUNE_AXE(1359, SkillOp.CHOP_DOWN),
	DRAGON_AXE(6739, SkillOp.CHOP_DOWN),
	THIRD_AGE_AXE(20011, SkillOp.CHOP_DOWN),

	BRONZE_PICKAXE(1265, SkillOp.MINE),
	IRON_PICKAXE(1267, SkillOp.MINE),
	STEEL_PICKAXE(1269, SkillOp.MINE),
	BLACK_PICKAXE(12297, SkillOp.MINE),
	MITHRIL_PICKAXE(1273, SkillOp.MINE),
	RUNE_PICKAXE(1275, SkillOp.MINE),

	SMALL_FISHING_NET(303, SkillOp.SMALL_NET),
	BIG_FISHING_NET(305, SkillOp.BIG_NET),
	LOBSTER_POT(301, SkillOp.CAGE),
	FISHING_BAIT(313, SkillOp.BAIT),
	FLY_FISHING_ROD(309, SkillOp.LURE),
	RAKE(5341, SkillOp.RAKE),

	BRONZE_BAR(2349, SkillOp.SMITH),
	IRON_BAR(2351, SkillOp.SMITH),
	STEEL_BAR(2353, SkillOp.SMITH),
	MITHRIL_BAR(2359, SkillOp.SMITH),
	ADAMANTITE_BAR(2361, SkillOp.SMITH),
	RUNITE_BAR(2363, SkillOp.SMITH),

	TIN_ORE(438, SkillOp.SMELT),
	COPPER_ORE(436, SkillOp.SMELT),
	IRON_ORE(440, SkillOp.SMELT),
	COAL(453, SkillOp.SMELT),
	MITHRIL_ORE(447, SkillOp.SMELT),
	RUNITE_ORE(451, SkillOp.SMELT),
	SILVER_ORE(442, SkillOp.SMELT),
	GOLD_ORE(444, SkillOp.SMELT),

	GRIMY_GUAM_LEAF(199, SkillOp.CLEAN),
	GRIMY_MARRENTILL(201, SkillOp.CLEAN),
	GRIMY_TARROMIN(203, SkillOp.CLEAN),
	GRIMY_HARRALANDER(205, SkillOp.CLEAN),
	GRIMY_RANARR_WEED(207, SkillOp.CLEAN),
	GRIMY_IRIT_LEAF(209, SkillOp.CLEAN),
	GRIMY_AVANTOE(211, SkillOp.CLEAN),
	GRIMY_KWUARM(213, SkillOp.CLEAN),
	GRIMY_CADANTINE(215, SkillOp.CLEAN),
	GRIMY_DWARF_WEED(217, SkillOp.CLEAN),
	GRIMY_TORSTOL(219, SkillOp.CLEAN),
	GRIMY_LANTADYME(2485, SkillOp.CLEAN),
	GRIMY_TOADFLAX(3049, SkillOp.CLEAN),
	GRIMY_SNAPDRAGON(3051, SkillOp.CLEAN),

	RUNE_ESSENCE(1436, SkillOp.CRAFT_RUNE),
	PURE_ESSENCE(7936, SkillOp.CRAFT_RUNE);

	@Getter private final int id;
	@Getter private final SkillOp option;

	SkillItem(int id, SkillOp option)
	{
		this.id = id;
		this.option = option;
	}

	public SkillOp getSkillOp()
	{
		return option;
	}

	private static final HashSet<Integer> ALL_SKILL_ITEMS = new HashSet<>();

	static
	{
		for (SkillItem skillItem : SkillItem.values())
		{
			ALL_SKILL_ITEMS.add(skillItem.getId());
		}
	}

	public static boolean isSkillItem(int id)
	{
		return ALL_SKILL_ITEMS.contains(id);
	}
}
