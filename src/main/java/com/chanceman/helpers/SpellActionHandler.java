package com.chanceman.helpers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.game.ItemManager;
import com.chanceman.UnlockedItemsManager;

public class SpellActionHandler {

    // Mapping from required rune to a list of staff names that provide unlimited runes.
    private static final Map<String, List<String>> STAFF_MAPPING = Map.ofEntries(
            Map.entry("air rune", List.of("Staff of air", "Air battlestaff", "Mystic air staff", "Dust battlestaff",
                    "Mystic dust staff", "Mist battlestaff", "Mystic mist staff", "Smoke battlestaff",
                    "Mystic smoke staff", "Blighted Wave Sack", "Blighted Surge Sack", "Blighted ancient ice sack",
                    "Blighted entangle sack", "Blighted teleport spell sack", "Blighted vengeance sack")),
            Map.entry("water rune", List.of("Staff of water", "Water battlestaff", "Mystic water staff",
                    "Mist battlestaff", "Mystic mist staff", "Mud battlestaff", "Mystic mud staff", "Steam battlestaff",
                    "Mystic steam staff", "Twinflame staff", "Kodai Wand", "Tome of Water", "Blighted Wave Sack",
                    "Blighted Surge Sack", "Blighted ancient ice sack", "Blighted entangle sack",
                    "Blighted teleport spell sack", "Blighted vengeance sack", "Trident of the Seas",
                    "Trident of the Swamp")),
            Map.entry("earth rune", List.of("Staff of earth", "Earth battlestaff", "Mystic earth staff",
                    "Dust battlestaff", "Mystic dust staff", "Mud battlestaff", "Mystic mud staff", "Lava battlestaff",
                    "Mystic lava staff", "Blighted Wave Sack", "Blighted Surge Sack", "Blighted ancient ice sack",
                    "Blighted entangle sack", "Blighted teleport spell sack", "Blighted vengeance sack",
                    "Tome of Earth")),
            Map.entry("fire rune", List.of("Staff of fire", "Fire battlestaff", "Mystic fire staff",
                    "Lava battlestaff", "Mystic lava staff", "Smoke battlestaff", "Mystic smoke staff",
                    "Steam battlestaff", "Mystic steam staff", "Twinflame staff", "Blighted Wave Sack",
                    "Blighted Surge Sack", "Blighted ancient ice sack", "Blighted entangle sack",
                    "Blighted teleport spell sack", "Blighted vengeance sack", "Tome of Fire"))
    );

    /**
     * Checks if the player's inventory or equipment contains an unlocked staff that substitutes for the given rune.
     */
    private static boolean hasStaffForRune(ItemManager itemManager, UnlockedItemsManager unlockedItemsManager,
                                           Client client, String runeKeyword) {
        List<String> staffList = STAFF_MAPPING.get(runeKeyword);
        if (staffList == null) {
            return false;
        }

        // Check player's inventory.
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory != null) {
            for (net.runelite.api.Item item : inventory.getItems()) {
                int canonicalId = itemManager.canonicalize(item.getId());
                ItemComposition comp = itemManager.getItemComposition(canonicalId);
                if (comp != null) {
                    String name = comp.getName().toLowerCase();
                    for (String staffName : staffList) {
                        if (name.contains(staffName.toLowerCase())) {
                            if (unlockedItemsManager.isUnlocked(canonicalId)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // Check player's equipment.
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment != null) {
            for (net.runelite.api.Item item : equipment.getItems()) {
                int canonicalId = itemManager.canonicalize(item.getId());
                ItemComposition comp = itemManager.getItemComposition(canonicalId);
                if (comp != null) {
                    String name = comp.getName().toLowerCase();
                    for (String staffName : staffList) {
                        if (name.contains(staffName.toLowerCase())) {
                            if (unlockedItemsManager.isUnlocked(canonicalId)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Helper method to strip HTML-like tags from a string.
     */
    private static String stripHtml(String input) {
        return input == null ? "" : input.replaceAll("<[^>]+>", "").trim();
    }

    /**
     * Removes any trailing parenthesized text from the spell name.
     */
    private static String cleanSpellName(String input) {
        return input == null ? "" : input.replaceAll("\\s*\\(.*\\)$", "").trim();
    }

    /**
     * Checks if the event corresponds to a spell cast and verifies that the required unlocked runes
     * (or staff substitutions) are present in the player's inventory. If any required rune is not
     * sufficiently available, the event is consumed.
     * Returns true if the spell cast is allowed; false otherwise.
     */
    public static boolean handleSpellAction(Client client, ItemManager itemManager,
                                            UnlockedItemsManager unlockedItemsManager,
                                            ChanceManSpellHelper spellHelper, MenuOptionClicked event) {
        String option = event.getMenuEntry().getOption().toLowerCase().trim();
        if (option.contains("cast")) {
            String rawSpellTarget = event.getMenuEntry().getTarget();
            String stripped = stripHtml(rawSpellTarget);
            String spellName = cleanSpellName(stripped).toLowerCase();

            Optional<Spells> spellOpt = spellHelper.getSpellByName(spellName);
            if (spellOpt.isPresent()) {
                Spells spells = spellOpt.get();
                Map<String, Integer> requiredRunes = spells.getRunes();
                if (requiredRunes.isEmpty()) {
                    return true;
                }
                boolean canCast = true;
                ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
                if (inventory != null) {
                    for (Map.Entry<String, Integer> runeEntry : requiredRunes.entrySet()) {
                        String runeKeyword = runeEntry.getKey().toLowerCase().trim();
                        int requiredQty = runeEntry.getValue();
                        int unlockedQty = 0;
                        // Check for staff substitution first.
                        if (hasStaffForRune(itemManager, unlockedItemsManager, client, runeKeyword)) {
                            continue;
                        }
                        for (net.runelite.api.Item item : inventory.getItems()) {
                            int canonicalId = itemManager.canonicalize(item.getId());
                            ItemComposition comp = itemManager.getItemComposition(canonicalId);
                            if (comp != null && comp.getName().toLowerCase().contains(runeKeyword)) {
                                if (unlockedItemsManager.isUnlocked(canonicalId)) {
                                    unlockedQty += item.getQuantity();
                                }
                            }
                        }
                        if (unlockedQty < requiredQty) {
                            canCast = false;
                            break;
                        }
                    }
                }
                if (!canCast) {
                    event.consume();
                    return false;
                }
            }
        }
        return true;
    }
}
