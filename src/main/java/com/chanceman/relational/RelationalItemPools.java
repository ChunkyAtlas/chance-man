package com.chanceman.relational;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Loads the item-ID pool definitions and filters them to active items. */
@Singleton
public final class RelationalItemPools
{
    private static final String RESOURCE = "/com/chanceman/relational-items.json";

    private final Set<Integer> toolItemIds;
    private final Set<Integer> questItemIds;

    @Inject
    public RelationalItemPools(Gson gson)
    {
        Set<Integer> tools = new LinkedHashSet<>();
        Set<Integer> quests = new LinkedHashSet<>();

        try (InputStream stream = RelationalItemPools.class.getResourceAsStream(RESOURCE))
        {
            if (stream == null) throw new IllegalStateException("Missing " + RESOURCE);
            JsonObject root = new JsonParser().parse(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            readIds(root.get("tools"), tools);
            readIds(root.get("quest"), quests);
        }
        catch (Exception ignored)
        {
            // An empty special-pool definition is safer than preventing the plugin from starting.
        }

        toolItemIds = Collections.unmodifiableSet(tools);
        questItemIds = Collections.unmodifiableSet(quests);
    }

    private static void readIds(JsonElement element, Set<Integer> target)
    {
        if (element == null || !element.isJsonArray()) return;
        element.getAsJsonArray().forEach(value -> {
            try { target.add(value.getAsInt()); }
            catch (RuntimeException ignored) { }
        });
    }

    public boolean isToolItem(int itemId)
    {
        return toolItemIds.contains(itemId);
    }

    public boolean isQuestItem(int itemId)
    {
        return questItemIds.contains(itemId);
    }

    public Set<Integer> filterTools(Set<Integer> eligibleItems)
    {
        return intersection(toolItemIds, eligibleItems);
    }

    public Set<Integer> filterQuestItems(Set<Integer> eligibleItems)
    {
        return intersection(questItemIds, eligibleItems);
    }

    public Set<Integer> filterNormalItems(Set<Integer> eligibleItems)
    {
        Set<Integer> result = new LinkedHashSet<>(eligibleItems);
        result.removeAll(toolItemIds);
        result.removeAll(questItemIds);
        return result;
    }

    private static Set<Integer> intersection(Set<Integer> source, Set<Integer> eligibleItems)
    {
        Set<Integer> result = new LinkedHashSet<>(source);
        if (eligibleItems == null) return Collections.emptySet();
        result.retainAll(eligibleItems);
        return result;
    }
}
