package com.chanceman.menus;

import lombok.Getter;

import java.util.HashMap;

@Getter
public enum SkillOp
{
	CHOP_DOWN("Chop down"),
	MINE("Mine"),
	SMALL_NET("Small Net"),
	BIG_NET("Big Net"),
	CAGE("Cage"),
	BAIT("Bait"),
	LURE("Lure"),
	HARPOON("Harpoon"),
	RAKE("Rake"),
	PRUNE("Prune"),
	CURE("Cure"),
	GRIND("Grind"),
	SMITH("Smith"),
	SMELT("Smelt"),
	SHEAR("Shear"),
	CLEAN("Clean"),
	FIRE("Fire"),
	CRAFT_RUNE("Craft-rune");

	private final String option;

	SkillOp(String option)
	{
		this.option = option;
	}

	private static final HashMap<String, SkillOp> STRING_TO_OP = new HashMap<>();

	static
	{
		for (SkillOp skillOp : SkillOp.values())
		{
			STRING_TO_OP.put(skillOp.option, skillOp);
		}
	}

	public static boolean isSkillOp(String option)
	{
		return STRING_TO_OP.containsKey(option);
	}

	public static SkillOp fromString(String option)
	{
		return STRING_TO_OP.get(option);
	}
}
