package com.chanceman.menus;

import lombok.Getter;

import java.util.HashMap;
import java.util.HashSet;

@Getter
public enum SkillOp
{
	CHOP_DOWN("Chop down"),
	MINE("Mine"),
	NET("Net"),
	CAGE("Cage"),
	BAIT("Bait"),
	LURE("Lure"),
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

	private static final HashSet<String> ALL_SKILL_OPS = new HashSet<>();
	private static final HashMap<String, SkillOp> STRING_TO_OP = new HashMap<>();

	static
	{
		for (SkillOp skillOp : SkillOp.values())
		{
			ALL_SKILL_OPS.add(skillOp.option);
			STRING_TO_OP.put(skillOp.option, skillOp);
		}
	}

	public static boolean isSkillOp(String option)
	{
		return ALL_SKILL_OPS.contains(option);
	}

	public static SkillOp fromString(String option)
	{
		if (!isSkillOp(option)) return null;
		return STRING_TO_OP.get(option);
	}
}
