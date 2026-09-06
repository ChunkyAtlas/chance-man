package com.chanceman.menus;

import lombok.Getter;

import java.util.HashSet;

@Getter
public enum Spell
{
	// STANDARD
	WIND_STRIKE("Wind Strike"),
	WATER_STRIKE("Water Strike"),
	EARTH_STRIKE("Earth Strike"),
	FIRE_STRIKE("Fire Strike"),
	WIND_BOLT("Wind Bolt"),
	WATER_BOLT("Water Bolt"),
	EARTH_BOLT("Earth Bolt"),
	FIRE_BOLT("Fire Bolt"),
	WIND_BLAST("Wind Blast"),
	WATER_BLAST("Water Blast"),
	EARTH_BLAST("Earth Blast"),
	FIRE_BLAST("Fire Blast"),
	WIND_WAVE("Wind Wave"),
	WATER_WAVE("Water Wave"),
	EARTH_WAVE("Earth Wave"),
	FIRE_WAVE("Fire Wave"),
	WIND_SURGE("Wind Surge"),
	WATER_SURGE("Water Surge"),
	EARTH_SURGE("Earth Surge"),
	FIRE_SURGE("Fire Surge"),
	SARADOMIN_STRIKE("Saradomin Strike"),
	CLAWS_OF_GUTHIX("Claws of Guthix"),
	FLAMES_OF_ZAMORAK("Flames of Zamorak"),
	CHARGE("Charge"),
	TELEBLOCK("Tele Block"),
	WEAKEN("Weaken"),
	CONFUSE("Confuse"),
	CURSE("Curse"),
	STUN("Stun"),
	VULNERABILITY("Vulnerability"),
	ENFEEBLE("Enfeeble"),
	BIND("Bind"),
	SNARE("Snare"),
	ENTANGLE("Entangle"),
	LOW_LEVEL_ALCHEMY("Low Level Alchemy"),
	HIGH_LEVEL_ALCHEMY("High Level Alchemy"),
	TELEKINETIC_GRAB("Telekinetic Grab"),
	VARROCK_TELEPORT("Varrock Teleport"),
	LUMBRIDGE_TELEPORT("Lumbridge Teleport"),
	FALADOR_TELEPORT("Falador Teleport"),
	CAMELOT_TELEPORT("Camelot Teleport"),
	ARDOUGNE_TELEPORT("Ardougne Teleport"),
	CIVITAS_ILLA_FORTIS_TELEPORT("Civitas illa Fortis Teleport"),
	TROLLHEIM_TELEPORT("Trollheim Teleport"),
	WATCHTOWER_TELEPORT("Watchtower Teleport"),
	TELEPORT_TO_HOUSE("Teleport to House"),
	APE_ATOLL_TELEPORT("Ape Atoll Teleport"),
	KOUREND_CASTLE_TELEPORT("Kourend Castle Teleport"),
	LVL_1_ENCHANT("Lvl-1 Enchant"),
	LVL_2_ENCHANT("Lvl-2 Enchant"),
	LVL_3_ENCHANT("Lvl-3 Enchant"),
	LVL_4_ENCHANT("Lvl-4 Enchant"),
	LVL_5_ENCHANT("Lvl-5 Enchant"),
	LVL_6_ENCHANT("Lvl-6 Enchant"),
	LVL_7_ENCHANT("Lvl-7 Enchant"),
	CRUMBLE_UNDEAD("Crumble Undead"),
	MAGIC_DART("Magic Dart"),
	IBAN_BLAST("Iban Blast"),
	BONES_TO_BANANAS("Bones to Bananas"),
	BONES_TO_PEACHES("Bones to Peaches"),
	SUPERHEAT_ITEM("Superheat Item"),
	TELEOTHER_LUMBRIDGE("Teleother Lumbridge"),
	TELEOTHER_VARROCK("Teleother Varrock"),
	TELEOTHER_FALADOR("Teleother Falador"),
	TELEOTHER_CAMELOT("Teleother Camelot"),
	CHARGE_WATER_ORB("Charge Water Orb"),
	CHARGE_EARTH_ORB("Charge Earth Orb"),
	CHARGE_FIRE_ORB("Charge Fire Orb"),
	CHARGE_AIR_ORB("Charge Air Orb"),
	MONSTER_INSPECT("Monster Inspect"),

	// ANCIENT
	SMOKE_RUSH("Smoke Rush"),
	SHADOW_RUSH("Shadow Rush"),
	BLOOD_RUSH("Blood Rush"),
	ICE_RUSH("Ice Rush"),
	SMOKE_BURST("Smoke Burst"),
	SHADOW_BURST("Shadow Burst"),
	BLOOD_BURST("Blood Burst"),
	ICE_BURST("Ice Burst"),
	SMOKE_BLITZ("Smoke Blitz"),
	SHADOW_BLITZ("Shadow Blitz"),
	BLOOD_BLITZ("Blood Blitz"),
	ICE_BLITZ("Ice Blitz"),
	SMOKE_BARRAGE("Smoke Barrage"),
	SHADOW_BARRAGE("Shadow Barrage"),
	BLOOD_BARRAGE("Blood Barrage"),
	ICE_BARRAGE("Ice Barrage"),
	PADDEWWA_TELEPORT("Paddewwa Teleport"),
	SENNTISTEN_TELEPORT("Senntisten Teleport"),
	KHARYRLL_TELEPORT("Kharyrll Teleport"),
	LASSAR_TELEPORT("Lassar Teleport"),
	DAREEYAK_TELEPORT("Dareeyak Teleport"),
	CARRALLANGER_TELEPORT("Carrallanger Teleport"),
	TELEPORT_TO_TARGET("Teleport to Target"),
	ANNAKARL_TELEPORT("Annakarl Teleport"),
	GHORROCK_TELEPORT("Ghorrock Teleport"),

	// ARCEUUS
	ARCEUUS_LIBRARY_TELEPORT("Arceuus Library Teleport"),
	DRAYNOR_MANOR_TELEPORT("Draynor Manor Teleport"),
	BATTLEFRONT_TELEPORT("Battlefront Teleport"),
	MIND_ALTAR_TELEPORT("Mind Altar Teleport"),
	RESPAWN_TELEPORT("Respawn Teleport"),
	SALVE_GRAVEYARD_TELEPORT("Salve Graveyard Teleport"),
	FENKENSTRAINS_CASTLE_TELEPORT("Fenkenstrain's Castle Teleport"),
	WEST_ARDOUGNE_TELEPORT("West Ardougne Teleport"),
	HARMONY_ISLAND_TELEPORT("Harmony Island Teleport"),
	CEMETERY_TELEPORT("Cemetery Teleport"),
	BARROWS_TELEPORT("Barrows Teleport"),
	TELEPORT_TO_TARGET_ARC("Teleport to Target"),
	TELEPORT_TO_BOAT("Teleport to Boat"),
	GHOSTLY_GRASP("Ghostly Grasp"),
	SKELETAL_GRASP("Skeletal Grasp"),
	UNDEAD_GRASP("Undead Grasp"),
	INFERIOR_DEMONBANE("Inferior Demonbane"),
	SUPERIOR_DEMONBANE("Superior Demonbane"),
	DARK_DEMONBANE("Dark Demonbane"),
	LESSER_CORRUPTION("Lesser Corruption"),
	GREATER_CORRUPTION("Greater Corruption"),
	RESURRECT_LESSER_GHOST("Resurrect Lesser Ghost"),
	RESURRECT_LESSER_SKELETON("Resurrect Lesser Skeleton"),
	RESURRECT_LESSER_ZOMBIE("Resurrect Lesser Zombie"),
	RESURRECT_SUPERIOR_GHOST("Resurrect Superior Ghost"),
	RESURRECT_SUPERIOR_SKELETON("Resurrect Superior Skeleton"),
	RESURRECT_SUPERIOR_ZOMBIE("Resurrect Superior Zombie"),
	RESURRECT_GREATER_GHOST("Resurrect Greater Ghost"),
	RESURRECT_GREATER_SKELETON("Resurrect Greater Skeleton"),
	RESURRECT_GREATER_ZOMBIE("Resurrect Greater Zombie"),
	DARK_LURE("Dark Lure"),
	MARK_OF_DARKNESS("Mark of Darkness"),
	WARD_OF_ARCEUUS("Ward of Arceuus"),
	BASIC_REANIMATION("Basic Reanimation"),
	ADEPT_REANIMATION("Adept Reanimation"),
	EXPERT_REANIMATION("Expert Reanimation"),
	MASTER_REANIMATION("Master Reanimation"),
	DEMONIC_OFFERING("Demonic Offering"),
	SINISTER_OFFERING("Sinister Offering"),
	SHADOW_VEIL("Shadow Veil"),
	VILE_VIGOUR("Vile Vigour"),
	DEGRIME("Degrime"),
	RESURRECT_CROPS("Resurrect Crops"),
	DEATH_CHARGE("Death Charge");

	private final String spellName;

	Spell(String spellName)
	{
		this.spellName = spellName;
	}

	private static final HashSet<String> ALL_SPELLS = new HashSet<>();

	static
	{
		for (Spell spell : Spell.values())
		{
			ALL_SPELLS.add(spell.getSpellName());
		}
	}

	public static boolean isSpell(String target)
	{
		return ALL_SPELLS.contains(target);
	}
}
