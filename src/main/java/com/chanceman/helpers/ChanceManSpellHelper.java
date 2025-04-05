package com.chanceman.helpers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

public class ChanceManSpellHelper {
    private List<Spells> spells;
    private final Gson gson;

    // Constructor now accepts the injected Gson instance.
    public ChanceManSpellHelper(Gson gson) {
        this.gson = gson;
        loadSpells();
    }

    private void loadSpells() {
        try (InputStreamReader reader = new InputStreamReader(
                getClass().getResourceAsStream("/net/runelite/client/plugins/chanceman/spells.json"))) {
            Type listType = new TypeToken<List<Spells>>() {}.getType();
            spells = gson.fromJson(reader, listType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Lookup a spell by its ID.
    public Optional<Spells> getSpellById(int spellId) {
        return spells.stream().filter(spell -> spell.getSpellId() == spellId).findFirst();
    }

    // Lookup a spell by its name (ignoring case).
    public Optional<Spells> getSpellByName(String name) {
        return spells.stream().filter(spell -> spell.getName().equalsIgnoreCase(name)).findFirst();
    }
}
