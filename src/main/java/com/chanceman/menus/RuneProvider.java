package com.chanceman.menus;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.HashMap;
import java.util.HashSet;

@Getter
public enum RuneProvider
{
	// Banana (yeah)
	BANANA(ItemID.BANANA),

	// Runes
	AIR_RUNE(ItemID.AIRRUNE),
	WATER_RUNE(ItemID.WATERRUNE),
	EARTH_RUNE(ItemID.EARTHRUNE),
	FIRE_RUNE(ItemID.FIRERUNE),
	MIND_RUNE(ItemID.MINDRUNE),
	BODY_RUNE(ItemID.BODYRUNE),
	COSMIC_RUNE(ItemID.COSMICRUNE),
	CHAOS_RUNE(ItemID.CHAOSRUNE),
	NATURE_RUNE(ItemID.NATURERUNE),
	LAW_RUNE(ItemID.LAWRUNE),
	DEATH_RUNE(ItemID.DEATHRUNE),
	BLOOD_RUNE(ItemID.BLOODRUNE),
	SOUL_RUNE(ItemID.SOULRUNE),
	WRATH_RUNE(ItemID.WRATHRUNE),
	SUNFIRE_RUNE(false, ItemID.SUNFIRERUNE, FIRE_RUNE),

	// Elemental equipment
	AIR_STAFF(true, ItemID.STAFF_OF_AIR, AIR_RUNE),
	MYSTIC_AIR_STAFF(true, ItemID.MYSTIC_AIR_STAFF, AIR_RUNE),
	WATER_STAFF(true, ItemID.STAFF_OF_WATER, WATER_RUNE),
	MYSTIC_WATER_STAFF(true, ItemID.MYSTIC_WATER_STAFF, WATER_RUNE),
	EARTH_STAFF(true, ItemID.STAFF_OF_EARTH, EARTH_RUNE),
	MYSTIC_EARTH_STAFF(true, ItemID.MYSTIC_EARTH_STAFF, EARTH_RUNE),
	FIRE_STAFF(true, ItemID.STAFF_OF_FIRE, FIRE_RUNE),
	MYSTIC_FIRE_STAFF(true, ItemID.MYSTIC_FIRE_STAFF, FIRE_RUNE),
	AIR_BATTLESTAFF(true, ItemID.AIR_BATTLESTAFF, AIR_RUNE),
	WATER_BATTLESTAFF(true, ItemID.WATER_BATTLESTAFF, WATER_RUNE),
	KODAI_WAND(true, ItemID.KODAI_WAND, WATER_RUNE),
	EARTH_BATTLESTAFF(true, ItemID.EARTH_BATTLESTAFF, EARTH_RUNE),
	FIRE_BATTLESTAFF(true, ItemID.FIRE_BATTLESTAFF, FIRE_RUNE),
	TOME_OF_FIRE(true, ItemID.TOME_OF_FIRE, FIRE_RUNE),
	TOME_OF_WATER(true, ItemID.TOME_OF_WATER, WATER_RUNE),
	TOME_OF_EARTH(true, ItemID.TOME_OF_EARTH, EARTH_RUNE),

	// Combo runes
	AETHER_RUNE(false,ItemID.AETHERRUNE,COSMIC_RUNE, SOUL_RUNE),
	MIST_RUNE(false, ItemID.MISTRUNE, AIR_RUNE, WATER_RUNE),
	DUST_RUNE(false, ItemID.DUSTRUNE, AIR_RUNE, EARTH_RUNE),
	MUD_RUNE(false, ItemID.MUDRUNE, WATER_RUNE, EARTH_RUNE),
	SMOKE_RUNE(false, ItemID.SMOKERUNE, FIRE_RUNE, AIR_RUNE),
	STEAM_RUNE(false, ItemID.STEAMRUNE, WATER_RUNE, FIRE_RUNE),
	LAVA_RUNE(false, ItemID.LAVARUNE, EARTH_RUNE, FIRE_RUNE),

	// Combo staves
	MIST_STAFF(true, ItemID.MIST_BATTLESTAFF, AIR_RUNE, WATER_RUNE),
	MYSTIC_MIST_STAFF(true, ItemID.MYSTIC_MIST_BATTLESTAFF, AIR_RUNE, WATER_RUNE),
	DUST_STAFF(true, ItemID.DUST_BATTLESTAFF, AIR_RUNE, EARTH_RUNE),
	MYSTIC_DUST_STAFF(true, ItemID.MYSTIC_DUST_BATTLESTAFF, AIR_RUNE, EARTH_RUNE),
	MUD_STAFF(true, ItemID.MUD_BATTLESTAFF, WATER_RUNE, EARTH_RUNE),
	MYSTIC_MUD_STAFF(true, ItemID.MYSTIC_MUD_STAFF, WATER_RUNE, EARTH_RUNE),
	SMOKE_STAFF(true, ItemID.SMOKE_BATTLESTAFF, FIRE_RUNE, AIR_RUNE),
	MYSTIC_SMOKE_STAFF(true, ItemID.MYSTIC_SMOKE_BATTLESTAFF, FIRE_RUNE, AIR_RUNE),
	STEAM_STAFF(true, ItemID.STEAM_BATTLESTAFF, WATER_RUNE, FIRE_RUNE),
	STEAM_STAFF_OR(true, ItemID.STEAM_BATTLESTAFF_PRETTY, WATER_RUNE, FIRE_RUNE),
	MYSTIC_STEAM_STAFF(true, ItemID.MYSTIC_STEAM_BATTLESTAFF, WATER_RUNE, FIRE_RUNE),
	MYSTIC_STEAM_STAFF_OR(true, ItemID.MYSTIC_STEAM_BATTLESTAFF_PRETTY, WATER_RUNE, FIRE_RUNE),
	LAVA_STAFF(true, ItemID.LAVA_BATTLESTAFF, EARTH_RUNE, FIRE_RUNE),
	LAVA_STAFF_OR(true, ItemID.LAVA_BATTLESTAFF_PRETTY, EARTH_RUNE, FIRE_RUNE),
	MYSTIC_LAVA_STAFF(true, ItemID.MYSTIC_LAVA_STAFF, EARTH_RUNE, FIRE_RUNE),
	MYSTIC_LAVA_STAFF_OR(true, ItemID.MYSTIC_LAVA_STAFF_PRETTY, EARTH_RUNE, FIRE_RUNE),
	TWINFLAME_STAFF(true, ItemID.TWINFLAME_STAFF, WATER_RUNE, FIRE_RUNE),

	// Other
	BRYOPHYTAS_STAFF_CHARGED(true, ItemID.NATURE_STAFF_CHARGED, NATURE_RUNE);

	private final boolean requiresEquipped;
	private final int id;
	private final HashSet<Integer> provides = new HashSet<>();

	RuneProvider(int id)
	{
		this.requiresEquipped = false;
		this.id = id;
		this.provides.add(id);
	}

	RuneProvider(boolean requiresEquipped, int id, RuneProvider... provides)
	{
		this.requiresEquipped = requiresEquipped;
		this.id = id;
		for (RuneProvider runeProvider : provides) this.provides.addAll(runeProvider.getProvides());
	}

	private static final HashSet<Integer> EQUIPPED_PROVIDERS = new HashSet<>();
	private static final HashSet<Integer> INV_PROVIDERS = new HashSet<>();
	private static final HashMap<Integer, HashSet<Integer>> PROVIDER_TO_PROVIDED = new HashMap<>();

	static
	{
		for (RuneProvider runeProvider : RuneProvider.values())
		{
			PROVIDER_TO_PROVIDED.put(runeProvider.getId(), runeProvider.getProvides());
			if (runeProvider.isRequiresEquipped())
			{
				EQUIPPED_PROVIDERS.add(runeProvider.getId());
			} else {
				INV_PROVIDERS.add(runeProvider.getId());
			}
		}
	}

	public static boolean isEquippedProvider(int id) { return EQUIPPED_PROVIDERS.contains(id); }
	public static boolean isInvProvider(int id) { return INV_PROVIDERS.contains(id); }
	public static HashSet<Integer> getProvidedRunes(int id) { return PROVIDER_TO_PROVIDED.get(id); }
}
