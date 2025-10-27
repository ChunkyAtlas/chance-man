package com.chanceman.filters;

import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.gameval.ItemID;


public final class BlockedItems {
    @Getter private static final Set<Integer> BLOCKED_ITEMS;

    static {
        Set<Integer> blocked = new HashSet<>();
        //Dead Man Mode
        blocked.add(ItemID.DEADMAN_BLIGHTED_AGS); // Corrupted Armadyl Godsword
        blocked.add(ItemID.DEADMAN_BLIGHTED_DARK_BOW); // Corrupted Dark Bow
        blocked.add(ItemID.DEADMAN_BLIGHTED_DRAGON_CLAWS); // Corrupted Dragon Claws
        blocked.add(ItemID.DEADMAN_BLIGHTED_TWISTED_BOW); // Corrupted Twisted Bow
        blocked.add(ItemID.DEADMAN_BLIGHTED_VOIDWAKER); // Corrupted Voidwaker
        blocked.add(ItemID.DEADMAN_BLIGHTED_VOLATILE_STAFF); // Corrupted Volatile Nightmare Staff
        blocked.add(ItemID.DEADMAN_BLIGHTED_TUMEKENS_SHADOW_UNCHARGED); // Corrupted Tumeken's Shadow (uncharged)
        blocked.add(ItemID.DEADMAN_BLIGHTED_SCYTHE_OF_VITUR_UNCHARGED);// Corrupted Scythe of Vitur (uncharged)
        blocked.add(ItemID.DEADMAN_NIMBLENESS_CHARM); // Nimbleness Charm
        blocked.add(ItemID.DEADMAN_STOCKPILING_CHARM); // Stockpiling Charm
        blocked.add(ItemID.DEADMAN_ACCUMULATION_CHARM); // Accumulation Charm
        blocked.add(ItemID.DEADMAN_VULNERABILITY_CHARM); // Vulnerability Charm
        blocked.add(ItemID.DEADMAN_2024_HOME_TELEPORT_SCROLL); // Armageddon Teleport Scroll
        blocked.add(ItemID.DEADMAN_2024_CAPE_ORNAMENT_SCROLL); // Armageddon Cape Fabric
        blocked.add(ItemID.DEADMAN_2024_WEAPON_ORNAMENT_SCROLL); // Armageddon Weapon Scroll
        blocked.add(ItemID.DEADMAN_RUG); // Armageddon Rug
        blocked.add(ItemID.DEADMAN1DOSEOVERLOAD); // Blighted Overload (1)
        blocked.add(ItemID.DEADMAN2DOSEOVERLOAD); // Blighted Overload (2)
        blocked.add(ItemID.DEADMAN3DOSEOVERLOAD); // Blighted Overload (3)
        blocked.add(ItemID.DEADMAN4DOSEOVERLOAD); // Blighted Overload (4)
        blocked.add(ItemID.DEADMAN_OVERLOAD_CHITIN); // Chitin
        blocked.add(ItemID.DEADMAN_STARTER_BOW); // Starter Bow
        blocked.add(ItemID.DEADMAN_STARTER_STAFF); // Starter Staff
        blocked.add(ItemID.DEADMAN_STARTER_SWORD); // Starter Sword
        blocked.add(ItemID.MAGIC_ROCK_OF_WEAPONS); // Trinket of Advanced Weaponry
        blocked.add(ItemID.MAGIC_ROCK_OF_FAIRIES); // Trinket of Fairies
        blocked.add(ItemID.MAGIC_ROCK_OF_UNDEAD); // Trinket of Undead
        blocked.add(ItemID.MAGIC_ROCK_OF_VENGEANCE); // Trinket of Vengeance
        blocked.add(ItemID.MORRIGANS_COIF); // Morrigan's Coif
        blocked.add(ItemID.MORRIGANS_LEATHER_BODY); // Morrigan's Leather Body
        blocked.add(ItemID.MORRIGANS_LEATHER_CHAPS); // Morrigan's Leather Chaps
        blocked.add(ItemID.MORRIGANS_THROWNAXE); // Morrigan's Thrown axe
        blocked.add(ItemID.MORRIGANS_JAVELIN); // Morrigan's Javelin
        blocked.add(ItemID.STATIUS_FULL_HELM); // Statius' Full Helm
        blocked.add(ItemID.STATIUS_PLATEBODY); // Statius' Platebody
        blocked.add(ItemID.STATIUS_PLATELEGS); // Statius' Platelegs
        blocked.add(ItemID.STATIUS_WARHAMMER); // Statius' Warhammer
        blocked.add(ItemID.VESTAS_CHAINBODY); // Vesta's Chainbody
        blocked.add(ItemID.VESTAS_PLATESKIRT); // Vesta's Plateskirt
        blocked.add(ItemID.VESTAS_LONGSWORD); // Vesta's Longsword
        blocked.add(ItemID.VESTAS_SPEAR); // Vesta's Spear
        blocked.add(ItemID.ZURIELS_HOOD); // Zuriel's Hood
        blocked.add(ItemID.ZURIELS_ROBE_TOP); // Zuriel's Robe Top
        blocked.add(ItemID.ZURIELS_ROBE_BOTTOM); // Zuriel's Robe Bottom
        blocked.add(ItemID.ZURIELS_STAFF); // Zuriel's Staff
        blocked.add(ItemID.SIGIL_OF_ADROIT_UNATTUNED); // Sigil Of Adroit
        blocked.add(ItemID.SIGIL_OF_AGGRESSION_UNATTUNED); // Sigil Of Aggression
        blocked.add(ItemID.SIGIL_OF_AGILE_FORTUNE_UNATTUNED); // Sigil Of Agile Fortune
        blocked.add(ItemID.SIGIL_OF_ARCANE_SWIFTNESS_UNATTUNED); // Sigil Of Arcane Swiftness
        blocked.add(ItemID.SIGIL_OF_BARROWS_UNATTUNED); // Sigil Of Barrows
        blocked.add(ItemID.SIGIL_OF_BINDING_UNATTUNED); // Sigil Of Binding
        blocked.add(ItemID.SIGIL_OF_CONSISTENCY_UNATTUNED); // Sigil Of Consistency
        blocked.add(ItemID.SIGIL_OF_DECEPTION_UNATTUNED); // Sigil Of Deception
        blocked.add(ItemID.SIGIL_OF_DEFT_STRIKES_UNATTUNED); // Sigil Of Deft Strikes
        blocked.add(ItemID.SIGIL_OF_DEVOTION_UNATTUNED); // Sigil Of Devotion
        blocked.add(ItemID.SIGIL_OF_ENHANCED_HARVEST_UNATTUNED); // Sigil Of Enhanced Harvest
        blocked.add(ItemID.SIGIL_OF_ESCAPING_UNATTUNED); // Sigil Of Escaping
        blocked.add(ItemID.SIGIL_OF_EXAGGERATION_UNATTUNED); // Sigil Of Exaggeration
        blocked.add(ItemID.SIGIL_OF_FAITH_UNATTUNED); // Sigil Of Faith
        blocked.add(ItemID.SIGIL_OF_FINALITY_UNATTUNED); // Sigil Of Finality
        blocked.add(ItemID.SIGIL_OF_FORTIFICATION_UNATTUNED); // Sigil Of Fortification
        blocked.add(ItemID.SIGIL_OF_FREEDOM_UNATTUNED); // Sigil Of Freedom
        blocked.add(ItemID.SIGIL_OF_GARMENTS_UNATTUNED); // Sigil Of Garments
        blocked.add(ItemID.SIGIL_OF_GUNSLINGER_UNATTUNED); // Sigil Of Gunslinger
        blocked.add(ItemID.SIGIL_OF_HOARDING_UNATTUNED); // Sigil Of Hoarding
        blocked.add(ItemID.SIGIL_OF_LAST_RECALL_UNATTUNED); // Sigil Of Last Recall
        blocked.add(ItemID.SIGIL_OF_LITHE_UNATTUNED); // Sigil Of Lithe
        blocked.add(ItemID.SIGIL_OF_METICULOUSNESS_UNATTUNED); // Sigil Of Meticulousness
        blocked.add(ItemID.SIGIL_OF_MOBILITY_UNATTUNED); // Sigil Of Mobility
        blocked.add(ItemID.SIGIL_OF_NATURE_UNATTUNED); // Sigil Of Nature
        blocked.add(ItemID.SIGIL_OF_ONSLAUGHT_UNATTUNED); // Sigil Of Onslaught
        blocked.add(ItemID.SIGIL_OF_PIOUS_PROTECTION_UNATTUNED); // Sigil Of Pious Protection
        blocked.add(ItemID.SIGIL_OF_PRECISION_UNATTUNED); // Sigil Of Precision
        blocked.add(ItemID.SIGIL_OF_PRESERVATION_UNATTUNED); // Sigil Of Preservation
        blocked.add(ItemID.SIGIL_OF_PROSPERITY_UNATTUNED); // Sigil Of Prosperity
        blocked.add(ItemID.SIGIL_OF_RAMPAGE_UNATTUNED); // Sigil Of Rampage
        blocked.add(ItemID.SIGIL_OF_RAMPART_UNATTUNED); // Sigil Of Rampart
        blocked.add(ItemID.SIGIL_OF_REMOTE_STORAGE_UNATTUNED); // Sigil Of Remote Storage
        blocked.add(ItemID.SIGIL_OF_RESILIENCE_UNATTUNED); // Sigil Of Resilience
        blocked.add(ItemID.SIGIL_OF_RESISTANCE_UNATTUNED); // Sigil Of Resistance
        blocked.add(ItemID.SIGIL_OF_RESTORATION_UNATTUNED); // Sigil Of Restoration
        blocked.add(ItemID.SIGIL_OF_REVOKED_LIMITATION_UNATTUNED); // Sigil Of Revoked Limitations
        blocked.add(ItemID.SIGIL_OF_SLAUGHTER_UNATTUNED); // Sigil Of Slaughter
        blocked.add(ItemID.SIGIL_OF_SPECIALISED_STRIKES_UNATTUNED); // Sigil Of Specialised Strikes
        blocked.add(ItemID.SIGIL_OF_STAMINA_UNATTUNED); // Sigil Of Stamina
        blocked.add(ItemID.SIGIL_OF_STORAGE_UNATTUNED); // Sigil Of Storage
        blocked.add(ItemID.SIGIL_OF_SUPREME_STAMINA_UNATTUNED); // Sigil Of Supreme Stamina
        blocked.add(ItemID.SIGIL_OF_SUSTENANCE_UNATTUNED); // Sigil Of Sustenance
        blocked.add(ItemID.SIGIL_OF_SWASHBUCKLER_UNATTUNED); // Sigil Of Swashbuckler
        blocked.add(ItemID.SIGIL_OF_THE_ABYSS_UNATTUNED); // Sigil Of The Abyss
        blocked.add(ItemID.SIGIL_OF_THE_ALCHEMIST_UNATTUNED); // Sigil Of The Alchemist
        blocked.add(ItemID.SIGIL_OF_THE_ALCHEMANIAC_UNATTUNED); // Sigil Of The Alchemaniac
        blocked.add(ItemID.SIGIL_OF_THE_AUGMENTED_THRALL_UNATTUNED); // Sigil Of The Augmented Thrall
        blocked.add(ItemID.SIGIL_OF_THE_BARBARIANS_UNATTUNED); // Sigil Of The Barbarians
        blocked.add(ItemID.SIGIL_OF_THE_BLOODHOUND_UNATTUNED); // Sigil Of The Bloodhound
        blocked.add(ItemID.SIGIL_OF_THE_CHEF_UNATTUNED); // Sigil Of The Chef
        blocked.add(ItemID.SIGIL_OF_THE_CRAFTER_UNATTUNED); // Sigil Of The Craftsman
        blocked.add(ItemID.SIGIL_OF_THE_DWARVES_UNATTUNED); // Sigil Of The Dwarves
        blocked.add(ItemID.SIGIL_OF_THE_ELVES_UNATTUNED); // Sigil Of The Elves
        blocked.add(ItemID.SIGIL_OF_THE_ETERNAL_JEWELLER_UNATTUNED); // Sigil Of The Eternal Jeweller
        blocked.add(ItemID.SIGIL_OF_THE_FERAL_FIGHTER_UNATTUNED); // Sigil Of The Feral Fighter
        blocked.add(ItemID.SIGIL_OF_THE_FLETCHER_UNATTUNED); // Sigil Of The Fletcher
        blocked.add(ItemID.SIGIL_OF_THE_FOOD_MASTER_UNATTUNED); // Sigil Of The Food Master
        blocked.add(ItemID.SIGIL_OF_THE_FORAGER_UNATTUNED); // Sigil Of The Forger
        blocked.add(ItemID.SIGIL_OF_THE_FORMIDABLE_FIGHTER_UNATTUNED); // Sigil Of The Formidable Fighter
        blocked.add(ItemID.SIGIL_OF_THE_FORTUNE_FARMER_UNATTUNED); // Sigil Of The Fortune Farmer
        blocked.add(ItemID.SIGIL_OF_THE_GNOMES_UNATTUNED); // Sigil Of The Gnomes
        blocked.add(ItemID.SIGIL_OF_THE_GUARDIAN_ANGEL_UNATTUNED); // Sigil Of The Guardian Angel
        blocked.add(ItemID.SIGIL_OF_THE_HUNTER_UNATTUNED); // Sigil Of The Hunter
        blocked.add(ItemID.SIGIL_OF_THE_INFERNAL_CHEF_UNATTUNED); // Sigil Of The Infernal Chef
        blocked.add(ItemID.SIGIL_OF_THE_INFERNAL_SMITH_UNATTUNED); // Sigil Of The Infernal Smith
        blocked.add(ItemID.SIGIL_OF_THE_LIGHTBEARER_UNATTUNED); // Sigil Of The Lighterbearer
        blocked.add(ItemID.SIGIL_OF_THE_MENACING_MAGE_UNATTUNED); // Sigil Of The Menacing Mage
        blocked.add(ItemID.SIGIL_OF_THE_METICULOUS_MAGE_UNATTUNED); // Sigil Of The Meticulous Mage
        blocked.add(ItemID.SIGIL_OF_THE_NINJA_UNATTUNED); // Sigil Of The Ninja
        blocked.add(ItemID.SIGIL_OF_THE_PORCUPINE_UNATTUNED); // Sigil Of The Porcupine
        blocked.add(ItemID.SIGIL_OF_THE_POTION_MASTER_UNATTUNED); // Sigil Of The Potion Master
        blocked.add(ItemID.SIGIL_OF_THE_RIGOROUS_RANGER_UNATTUNED); // Sigil Of The Rigorous Ranger
        blocked.add(ItemID.SIGIL_OF_THE_RUTHLESS_RANGER_UNATTUNED); // Sigil Of The Ruthless Ranger
        blocked.add(ItemID.SIGIL_OF_THE_SERPENT_UNATTUNED); // Sigil Of The Serpent
        blocked.add(ItemID.SIGIL_OF_THE_SKILLER_UNATTUNED); // Sigil Of The Skiller
        blocked.add(ItemID.SIGIL_OF_THE_SMITH_UNATTUNED); // Sigil Of The Smith
        blocked.add(ItemID.SIGIL_OF_THE_TREASURE_HUNTER_UNATTUNED); // Sigil Of The Treasure Hunter
        blocked.add(ItemID.SIGIL_OF_THE_WELL_FED_UNATTUNED); // Sigil Of The Well-Fed
        blocked.add(ItemID.SIGIL_OF_TITANIUM_UNATTUNED); // Sigil Of Titanium
        blocked.add(ItemID.SIGIL_OF_VERSATILITY_UNATTUNED); // Sigil Of Versatility
        blocked.add(ItemID.SIGIL_OF_WOODCRAFT_UNATTUNED); // Sigil Of Woodcraft

        // Twisted Leagues Rewards
        blocked.add(ItemID.TWISTED_BANNER); // Twisted Banner
        blocked.add(ItemID.TWISTED_BLUEPRINTS); // Twisted Blueprints
        blocked.add(ItemID.TWISTED_CANE); // Twisted Cane
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_HAT_T1); // Twisted Hat (t1)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_HAT_T2); // Twisted Hat (t2)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_HAT_T3); // Twisted Hat (t3)
        blocked.add(ItemID.TWISTED_HORNS); // Twisted Horns
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_TOP_T1); // Twisted Coat (t1)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_TOP_T2); // Twisted Coat (t2)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_TOP_T3); // Twisted Coat (t3)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_LEGS_T1); // Twisted Trouser (t1)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_LEGS_T2); // Twisted Trouser (t2)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_LEGS_T3); // Twisted Trouser (t3)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_BOOTS_T1); // Twisted Boots (t1)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_BOOTS_T2); // Twisted Boots (t2)
        blocked.add(ItemID.TWISTED_RELIC_HUNTER_BOOTS_T3); // Twisted Boots (t3)
        blocked.add(ItemID.SET_TWISTED_RELICHUNTER_T1); // Twisted Relic Hunter (t1) Armour Set
        blocked.add(ItemID.SET_TWISTED_RELICHUNTER_T2); // Twisted Relic Hunter (t2) Armour Set
        blocked.add(ItemID.SET_TWISTED_RELICHUNTER_T3); // Twisted Relic Hunter (t3) Armour Set
        blocked.add(ItemID.TWISTED_HOME_TELEPORT_SCROLL); // Twisted Teleport Scroll

        // Trailblazer Leagues Rewards
        blocked.add(ItemID.TRAILBLAZER_BANNER); // Trailblazer Banner
        blocked.add(ItemID.TRAILBLAZER_HOME_TELEPORT_SCROLL); // Trailblazer Teleport Scroll
        blocked.add(ItemID.TRAILBLAZER_TOOL_ORNAMENTKIT); // Trailblazer Tool Ornament Kit
        blocked.add(ItemID.TRAILBLAZER_GRACEFUL_KIT); // Trailblazer Graceful Ornament Kit
        blocked.add(ItemID.TRAILBLAZER_STATUE); // Trailblazer Globe
        blocked.add(ItemID.TRAILBLAZER_RUG); // Trailblazer Rug
        blocked.add(ItemID.TRAILBLAZER_CANE); // Trailblazer Cane
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_HOOD_T1); // Trailblazer Hood (t1)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_HOOD_T2); // Trailblazer Hood (t2)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_HOOD_T3); // Trailblazer Hood (t3)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_TOP_T1); // Trailblazer Top (t1)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_TOP_T2); // Trailblazer Top (t2)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_TOP_T3); // Trailblazer Top (t3)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_LEGS_T1); // Trailblazer Trousers (t1)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_LEGS_T2); // Trailblazer Trousers (t2)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_LEGS_T3); // Trailblazer Trouser (t3)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_BOOTS_T1); // Trailblazer Boots (t1)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_BOOTS_T2); // Trailblazer Boots (t2)
        blocked.add(ItemID.TRAILBLAZER_RELIC_HUNTER_BOOTS_T3); // Trailblazer Boots (t3)
        blocked.add(ItemID.SET_TRAILBLAZER_RELICHUNTER_T1); // Trailblazer Relic Hunter (t1) Armour Set
        blocked.add(ItemID.SET_TRAILBLAZER_RELICHUNTER_T2); // Trailblazer Relic Hunter (t2) Armour Set
        blocked.add(ItemID.SET_TRAILBLAZER_RELICHUNTER_T3); // Trailblazer Relic Hunter (t3) Armour Set

        // Shattered Leagues Rewards
        blocked.add(ItemID.LEAGUE_3_BANNER); // Shattered Banner
        blocked.add(ItemID.LEAGUE_3_HOME_TELEPORT_SCROLL); // Shattered Teleport Scroll
        blocked.add(ItemID.LEAGUE_3_CANE); // Shattered Cane
        blocked.add(ItemID.LEAGUE_3_WEAPON_VARIETY_PACK); // Shattered Relics Variety Ornament Kit
        blocked.add(ItemID.LEAGUE_3_VOID_KIT); // Shattered Relics Void Ornament Kit
        blocked.add(ItemID.LEAGUE_3_MYSTIC_ORNAMENT_PACK); // Shattered Relics Mystic Ornament Kit
        blocked.add(ItemID.LEAGUE_3_MULTICANNON_PACK); // Shattered Cannon Ornament Kit
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_HOOD_T1); // Shattered Hood (t1)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_HOOD_T2); // Shattered Hood (t2)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_HOOD_T3); // Shattered Hood (t3)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_TOP_T1); // Shattered Top (t1)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_TOP_T2); // Shattered Top (t2)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_TOP_T3); // Shattered Top (t3)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_LEGS_T1); // Shattered Trousers (t1)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_LEGS_T2); // Shattered Trousers (t2)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_LEGS_T3); // Shattered Trousers (t3)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_BOOTS_T1); // Shattered Boots (t1)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_BOOTS_T2); // Shattered Boots (t2)
        blocked.add(ItemID.LEAGUE_3_RELIC_HUNTER_BOOTS_T3); // Shattered Boots (t3)
        blocked.add(ItemID.SET_LEAGUE_3_RELICHUNTER_T1); // Shattered Relic Hunter (t1) Armour Set
        blocked.add(ItemID.SET_LEAGUE_3_RELICHUNTER_T2); // Shattered Relic Hunter (t2) Armour Set
        blocked.add(ItemID.SET_LEAGUE_3_RELICHUNTER_T3); // Shattered Relic Hunter (t3) Armour Set

        // Trailblazer Reloaded Leagues Rewards
        blocked.add(ItemID.LEAGUE_4_BANNER); // Trailblazer Reloaded Banner
        blocked.add(ItemID.LEAGUE_4_HOME_TELEPORT_SCROLL); // Trailblazer Reloaded Home Teleport Scroll
        blocked.add(ItemID.LEAGUE_4_DEATH_SCROLL); // Trailblazer Reloaded Death Scroll
        blocked.add(ItemID.LEAGUE_4_ALCHEMY_SCROLL); // Trailblazer Reloaded Alchemy Scroll
        blocked.add(ItemID.LEAGUE_4_VENGEANCE_SCROLL); // Trailblazer Reloaded Vengeance Scroll
        blocked.add(ItemID.LEAGUE_4_REJUVINATION_POOL_SCROLL); // Trailblazer Reloaded Rejuvenation Pool Scroll
        blocked.add(ItemID.LEAGUE_4_TORCH); // Trailblazer Reloaded Torch
        blocked.add(ItemID.TOXIC_BLOWPIPE_ORNAMENT_KIT); // Trailblazer Reloaded Blowpipe Ornament Kit
        blocked.add(ItemID.DINHS_BULWARK_ORNAMENT_KIT); // Trailblazer Reloaded Banner Bulwark Ornament Kit
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_HAT_T1); // Trailblazer Reloaded Headband (t1)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_HAT_T2); // Trailblazer Reloaded Headband (t2)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_HAT_T3); // Trailblazer Reloaded Headband (t3)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_TOP_T1); // Trailblazer Reloaded Top (t1)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_TOP_T2); // Trailblazer Reloaded Top (t2)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_TOP_T3); // Trailblazer Reloaded Top (t3)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_LEGS_T1); // Trailblazer Reloaded Trousers (t1)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_LEGS_T2); // Trailblazer Reloaded Trousers (t2)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_LEGS_T3); // Trailblazer Reloaded Trousers (t3)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_BOOTS_T1); // Trailblazer Reloaded Boots (t1)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_BOOTS_T2); // Trailblazer Reloaded Boots (t2)
        blocked.add(ItemID.LEAGUE_4_RELIC_HUNTER_BOOTS_T3); // Trailblazer Reloaded Boots (t3)
        blocked.add(ItemID.SET_LEAGUE_4_RELICHUNTER_T1); // Trailblazer Reloaded Relic Hunter (t1) Armour Set
        blocked.add(ItemID.SET_LEAGUE_4_RELICHUNTER_T2); // Trailblazer Reloaded Relic Hunter (t2) Armour Set
        blocked.add(ItemID.SET_LEAGUE_4_RELICHUNTER_T3); // Trailblazer Reloaded Relic Hunter (t3) Armour Set

        // Raging Echoes Leagues Rewards
        blocked.add(ItemID.LEAGUE_5_BANNER); // Raging Echoes
        blocked.add(ItemID.LEAGUE_5_RUG); // Raging Echoes Rug
        blocked.add(ItemID.LEAGUE_5_CURTAINS); // Raging Echoes Curtains
        blocked.add(ItemID.LEAGUE_5_SPIRIT_TREE_SCROLL); // Raging Echoes Spirit Tree Scroll
        blocked.add(ItemID.LEAGUE_5_NEXUS_SCROLL); // Raging Echoes Portal Nexus Scroll
        blocked.add(ItemID.LEAGUE_5_SCRYING_SCROLL); // Raging Echoes Scrying Pool Scroll
        blocked.add(ItemID.LEAGUE_5_PORTAL_SCROLL); // Raging Echoes Portal Scroll
        blocked.add(ItemID.LEAGUE_5_HOME_TELEPORT_SCROLL); // Raging Echoes Home Teleport
        blocked.add(ItemID.LEAGUE_5_DEATH_SCROLL); // Raging Echoes Death Scroll
        blocked.add(ItemID.LEAGUE_5_CONTACT_SCROLL); // Raging Echoes NPC Contact Scroll
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_CANE); // Raging Echoes Cane
        blocked.add(ItemID.BARROWS_AHRIM_ORNAMENT_KIT); // Echo Ahrim's Robes Ornament Kit
        blocked.add(ItemID.BARROWS_AHRIM_WEAPON); // Echo Ahrim's Staff Ornament Kit
        blocked.add(ItemID.VIRTUS_ORNAMENT_KIT); // Echo Virtus Robes Ornament Kit
        blocked.add(ItemID.VENATOR_BOW_ORNAMENT_KIT); // Echo Venator Bow Ornament Kit
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_HAT_T1); // Raging Echoes Hat (t1)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_HAT_T2); // Raging Echoes Hat (t2)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_HAT_T3); // Raging Echoes Hat (t3)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_TOP_T1); // Raging Echoes Top (t1)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_TOP_T2); // Raging Echoes Top (t2)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_TOP_T3); // Raging Echoes Top (t3)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_LEGS_T1); // Raging Echoes Robeskirt (t1)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_LEGS_T2); // Raging Echoes Robeskirt (t2)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_LEGS_T3); // Raging Echoes Robeskirt (t3)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_BOOTS_T1); // Raging Echoes Boots (t1)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_BOOTS_T2); // Raging Echoes Boots (t2)
        blocked.add(ItemID.LEAGUE5_RELIC_HUNTER_BOOTS_T3); // Raging Echoes Boots (t3)
        blocked.add(ItemID.SET_LEAGUE_5_RELICHUNTER_T1); // Raging Echoes Relic Hunter (t1) Armour Set
        blocked.add(ItemID.SET_LEAGUE_5_RELICHUNTER_T2); // Raging Echoes Relic Hunter (t2) Armour Set
        blocked.add(ItemID.SET_LEAGUE_5_RELICHUNTER_T3); // Raging Echoes Relic Hunter (t3) Armour Set

        //Gridmaster Rewards
        blocked.add(ItemID.BINGO_TABARD); // Grid master tabard
        blocked.add(ItemID.BINGO_TABARD_BLACK); // Grid master tabard (b)
        blocked.add(ItemID.BINGO_TABARD_PINK); // Grid master tabard (p)
        blocked.add(ItemID.BINGO_TABARD_GREEN); // Grid master tabard (g)
        blocked.add(ItemID.BINGO_SWORDS); // Swords and emblem
        blocked.add(ItemID.BINGO_SWORDS_BLACK); // Swords and emblem (b)
        blocked.add(ItemID.BINGO_SWORDS_PINK); // Swords and emblem (p)
        blocked.add(ItemID.BINGO_SWORDS_GREEN); // Swords and emblem (g)
        blocked.add(ItemID.BINGO_ALTAR_ICON_SCROLL); // Grid master altar icon scroll
        blocked.add(ItemID.BINGO_TORCH_SCROLL); // Grid master torch scroll

        // Unobtainable Items
        blocked.add(ItemID.CHOMPY_COOKED); // Cooked Chompy (roasted)
        blocked.add(ItemID.CHICKENQUEST_CORNFLOUR); // Cornflour
        blocked.add(ItemID.POH_FLATPACK_CRYSTALBALL1); // Crystal Ball (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_CRYSTALBALL2); // Elemental Sphere (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_CRYSTALBALL3); // Crystal Of Power (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE1); // Globe (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE2); // Ornamental Globe (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE3); // Lunar Globe (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE4); // Celestial Globe (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE5); // Armillary Globe (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE6); // Small Orrery (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_GLOBE7); // Large Orrery (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_TELESCOPE1); // Oak Telescope (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_TELESCOPE2); // Teak Telescope (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_TELESCOPE3); // Mahogany (FLATPACK)
        blocked.add(ItemID.UNFINISHED_WORM_CRUNCHIES); // Rock-Climbing Boots
        blocked.add(ItemID.OSB9_REPORT); // Osman's Report
        blocked.add(ItemID.FEUD_KARIDIAN_FAKEBEARD_AND_HAT); // Karidian Disguise
        blocked.add(ItemID.POH_FLATPACK_LECTURN1); // Oak Lectern (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_LECTURN2); // Eagle Lectern (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_LECTURN3); // Demon Lectern (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_LECTURN4); // Teak Eagle Lectern (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_LECTURN5); // Teak Demon Lectern (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_LECTURN6); // Mahogany Eagle (FLATPACK)
        blocked.add(ItemID.POH_FLATPACK_LECTURN7); // Mahogany Demon (FLATPACK)
        blocked.add(ItemID.BH_SUPPLY_CRATE); // Bounty Supply Crate
        BLOCKED_ITEMS = Collections.unmodifiableSet(blocked);
    }
}
