package com.chanceman.drops;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts Bucket:dropsline API results into Chance Man drop-table models.
 */
final class WikiDropBucketParser {
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final String NORMAL_SECTION = "Drops";
    private static final String RARE_SECTION = "Rare drop table";
    private static final String GEM_SECTION = "Gem drop table";

    private static final String COMBINED_SECTION = "Rare and Gem drop table";

    private WikiDropBucketParser() {
    }

    static List<BucketDrop> parseResponse(String responseBody) {
        JsonElement parsed = new JsonParser().parse(responseBody);
        if (!parsed.isJsonObject()) {
            return Collections.emptyList();
        }

        JsonObject root = parsed.getAsJsonObject();
        String error = stringValue(root.get("error"));
        if (!error.isEmpty()) {
            throw new IllegalArgumentException("Wiki Bucket query failed: " + error);
        }

        JsonElement bucket = root.get("bucket");
        if (bucket == null || !bucket.isJsonArray()) {
            return Collections.emptyList();
        }

        List<BucketDrop> drops = new ArrayList<>();
        for (JsonElement element : bucket.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject row = element.getAsJsonObject();
            addRowDrops(
                    row.get("drop_json"),
                    row.get("rare_drop_table"),
                    stringValue(row.get("page_name")),
                    stringValue(row.get("page_name_sub")),
                    drops
            );
        }
        return drops;
    }

    private static void addRowDrops(
            JsonElement dropJson,
            JsonElement rareDropTable,
            String pageName,
            String pageSub,
            List<BucketDrop> output) {
        if (dropJson == null || dropJson.isJsonNull()) {
            return;
        }

        if (dropJson.isJsonArray()) {
            JsonArray drops = dropJson.getAsJsonArray();
            for (int i = 0; i < drops.size(); i++) {
                addEmbeddedDrops(
                        drops.get(i),
                        booleanValue(elementAt(rareDropTable, i)),
                        pageName,
                        pageSub,
                        output
                );
            }
            return;
        }

        addEmbeddedDrops(dropJson, booleanValue(rareDropTable), pageName, pageSub, output);
    }

    private static void addEmbeddedDrops(
            JsonElement dropJson,
            boolean rareDropTable,
            String pageName,
            String pageSub,
            List<BucketDrop> output) {
        if (dropJson == null || dropJson.isJsonNull()) {
            return;
        }
        if (dropJson.isJsonArray()) {
            for (JsonElement element : dropJson.getAsJsonArray()) {
                addEmbeddedDrops(element, rareDropTable, pageName, pageSub, output);
            }
            return;
        }

        JsonObject data = parseEmbeddedObject(dropJson);
        if (data == null) {
            return;
        }

        String itemName = stringValue(data.get("Dropped item")).trim();
        if (itemName.isEmpty()) {
            itemName = stringValue(data.get("Dropped item from RDT")).trim();
        }
        if (itemName.isEmpty()
                || itemName.equalsIgnoreCase("nothing")
                || itemName.equalsIgnoreCase("coin")
                || itemName.equalsIgnoreCase("coins")) {
            return;
        }

        String sourceVersion = extractSourceVersion(stringValue(data.get("Dropped from")));
        if (sourceVersion.isEmpty()) {
            sourceVersion = extractPageSub(pageName, pageSub);
        }

        output.add(new BucketDrop(
                itemName,
                buildRarity(data),
                sourceVersion,
                parseLevels(data.get("Drop level")),
                rareDropTable
        ));
    }

    private static JsonObject parseEmbeddedObject(JsonElement element) {
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        if (!element.isJsonPrimitive()) {
            return null;
        }

        String raw = element.getAsString();
        JsonObject direct = tryParseObject(raw);
        return direct == null ? tryParseObject(unescapeJsonEntities(raw)) : direct;
    }

    private static JsonObject tryParseObject(String value) {
        try {
            JsonElement parsed = new JsonParser().parse(value);
            return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String unescapeJsonEntities(String value) {
        return value
                .replace("&#123;", "{")
                .replace("&#125;", "}")
                .replace("&#91;", "[")
                .replace("&#93;", "]")
                .replace("&#34;", "\"")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private static String buildRarity(JsonObject data) {
        String primary = cleanRarity(stringValue(data.get("Rarity")));
        String alternate = cleanRarity(stringValue(data.get("Alt Rarity")));
        boolean approximate = booleanValue(data.get("Approx"));
        int rolls = intValue(data.get("Rolls"), 1);

        if (approximate) {
            primary = approximate(primary);
            alternate = approximate(alternate);
        }
        if (rolls > 1 && !primary.isEmpty()) {
            primary = rolls + " x " + primary;
        }
        if (primary.isEmpty() || alternate.isEmpty()) {
            return primary.isEmpty() ? alternate : primary;
        }
        return primary + (hasContent(data.get("Alt Rarity Dash")) ? "–" : "; ") + alternate;
    }

    private static String approximate(String value) {
        return value.isEmpty() || value.startsWith("~") ? value : "~" + value;
    }

    private static String cleanRarity(String value) {
        return value.replace(",", "").trim();
    }

    static List<DropTableSection> selectSections(
            List<BucketDrop> allDrops,
            List<String> targetDropVersions,
            int combatLevel,
            WikiDropTableClassifier.Classification classification) {
        if (allDrops == null || allDrops.isEmpty()) {
            return Collections.emptyList();
        }

        WikiDropTableClassifier.Classification tables = classification == null
                ? WikiDropTableClassifier.Classification.empty()
                : classification;
        Map<String, DropItem> normal = new LinkedHashMap<>();
        Map<String, DropItem> rare = new LinkedHashMap<>();
        Map<String, DropItem> gem = new LinkedHashMap<>();
        Map<String, DropItem> combined = new LinkedHashMap<>();

        for (BucketDrop drop : selectRows(allDrops, targetDropVersions, combatLevel)) {
            DropItem item = new DropItem(0, drop.getItemName(), drop.getRarity());
            if (!drop.isRareDropTable()) {
                mergeMostCommon(normal, item);
                continue;
            }

            routeSpecialTableDrop(
                    item,
                    tables.resolve(drop.getSourceVersion(), targetDropVersions),
                    rare,
                    gem,
                    combined
            );
        }

        List<DropTableSection> sections = new ArrayList<>();
        addSection(sections, NORMAL_SECTION, normal);
        addSection(sections, RARE_SECTION, rare);
        addSection(sections, GEM_SECTION, gem);
        addSection(sections, COMBINED_SECTION, combined);
        return sections;
    }

    private static void routeSpecialTableDrop(
            DropItem item,
            WikiDropTableClassifier.Access access,
            Map<String, DropItem> rare,
            Map<String, DropItem> gem,
            Map<String, DropItem> combined) {
        if (access.isGem() && !access.isRare()) {
            mergeMostCommon(gem, item);
            return;
        }
        if (access.isRare() && !access.isGem()) {
            mergeMostCommon(rare, item);
            return;
        }

        mergeMostCommon(combined, item);
    }

    static boolean isSpecialSection(String header) {
        return RARE_SECTION.equals(header)
                || GEM_SECTION.equals(header)
                || COMBINED_SECTION.equals(header);
    }

    private static void addSection(
            List<DropTableSection> sections,
            String header,
            Map<String, DropItem> items) {
        if (!items.isEmpty()) {
            sections.add(new DropTableSection(header, new ArrayList<>(items.values())));
        }
    }

    private static List<BucketDrop> selectRows(
            List<BucketDrop> allDrops,
            List<String> targetDropVersions,
            int combatLevel) {
        Set<String> targets = normalizeVersions(targetDropVersions);
        List<BucketDrop> matches = new ArrayList<>();

        for (BucketDrop drop : allDrops) {
            Set<String> versions = normalizeVersions(WikiMonsterMetadataParser.splitValues(drop.getSourceVersion(), "[,¦]"));
            if (!targets.isEmpty()
                    && !versions.isEmpty()
                    && Collections.disjoint(versions, targets)) {
                continue;
            }

            if (combatLevel > 0
                    && !drop.getLevels().isEmpty()
                    && !drop.getLevels().contains(combatLevel)) {
                continue;
            }

            matches.add(drop);
        }
        return matches;
    }

    private static void mergeMostCommon(Map<String, DropItem> items, DropItem candidate) {
        String key = normalizeItemName(candidate.getName());
        DropItem existing = items.get(key);
        if (existing == null || candidate.getRarityValue() < existing.getRarityValue()) {
            items.put(key, candidate);
        }
    }

    private static String normalizeItemName(String name) {
        return name == null
                ? ""
                : name.trim().replace('_', ' ').replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeVersions(List<String> versions) {
        Set<String> normalized = new LinkedHashSet<>();
        if (versions != null) {
            for (String version : versions) {
                String value = normalizeDropVersion(version);
                if (!value.isEmpty() && !"default".equals(value)) {
                    normalized.add(value);
                }
            }
        }
        return normalized;
    }

    private static String normalizeDropVersion(String value) {
        String normalized = WikiMonsterMetadataParser.normalizeForComparison(value);

        if (normalized.startsWith("drops (") && normalized.endsWith(")")) {
            normalized = normalized.substring(7, normalized.length() - 1).trim();
        }

        return normalized;
    }


    private static String extractSourceVersion(String source) {
        int hash = source == null ? -1 : source.indexOf('#');
        return hash < 0 ? "" : source.substring(hash + 1).trim();
    }

    static String extractPageSub(String pageName, String pageSub) {
        if (pageSub == null || pageSub.trim().isEmpty()) {
            return "";
        }

        String value = pageSub.trim();
        int hash = value.indexOf('#');
        if (hash >= 0) {
            return value.substring(hash + 1).trim();
        }

        return WikiMonsterMetadataParser.normalizeForComparison(value)
                .equals(WikiMonsterMetadataParser.normalizeForComparison(pageName))
                ? ""
                : value;
    }

    private static Set<Integer> parseLevels(JsonElement value) {
        return WikiMonsterMetadataParser.parseIntegers(stringValue(value));
    }

    static String stringValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }
        if (value.isJsonArray()) {
            List<String> values = new ArrayList<>();
            for (JsonElement child : value.getAsJsonArray()) {
                String childValue = stringValue(child);
                if (!childValue.isEmpty()) {
                    values.add(childValue);
                }
            }
            return String.join(",", values);
        }
        return value.isJsonObject() ? value.toString() : value.getAsString();
    }

    private static boolean booleanValue(JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return false;
        }
        if (value.isJsonArray()) {
            for (JsonElement child : value.getAsJsonArray()) {
                if (booleanValue(child)) {
                    return true;
                }
            }
            return false;
        }
        if (value.isJsonPrimitive()) {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            }
            if (primitive.isNumber()) {
                return primitive.getAsDouble() != 0d;
            }
        }

        String text = stringValue(value).trim().toLowerCase(Locale.ROOT);
        return "true".equals(text) || "yes".equals(text) || "1".equals(text);
    }

    private static JsonElement elementAt(JsonElement value, int index) {
        if (value == null || value.isJsonNull() || !value.isJsonArray()) {
            return value;
        }
        JsonArray array = value.getAsJsonArray();
        return index >= 0 && index < array.size() ? array.get(index) : null;
    }

    private static boolean hasContent(JsonElement value) {
        return !stringValue(value).trim().isEmpty();
    }

    private static int intValue(JsonElement value, int fallback) {
        if (value == null || value.isJsonNull()) {
            return fallback;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException ignored) {
            Matcher matcher = INTEGER.matcher(value.getAsString());
            if (!matcher.find()) {
                return fallback;
            }
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException ignoredAgain) {
                return fallback;
            }
        }
    }

    @Value
    static class BucketDrop {
        String itemName;
        String rarity;
        String sourceVersion;
        Set<Integer> levels;
        boolean rareDropTable;

        BucketDrop(
                String itemName,
                String rarity,
                String sourceVersion,
                Set<Integer> levels,
                boolean rareDropTable) {
            this.itemName = itemName;
            this.rarity = rarity;
            this.sourceVersion = sourceVersion == null ? "" : sourceVersion;
            this.levels = Collections.unmodifiableSet(new LinkedHashSet<>(levels));
            this.rareDropTable = rareDropTable;
        }
    }
}