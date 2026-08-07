package com.chanceman.drops;

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
import java.util.stream.Collectors;

/**
 * Parses NPC IDs, combat levels, variants, and drop versions from Infobox Monster.
 */
final class WikiMonsterMetadataParser {
    private static final Pattern INDEXED_KEY = Pattern.compile(
            "^(version|bucketname|name|combat|id|dropversion)(\\d+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern INTEGER = Pattern.compile("\\d+");
    private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^]\\|]+)(?:\\|([^]]+))?]]");

    private WikiMonsterMetadataParser() {
    }

    static ParsedMonster parse(String wikitext) {
        WikiTemplateParser.Template infobox = WikiTemplateParser.findFirst(wikitext, "Infobox Monster");
        if (infobox == null) {
            return ParsedMonster.empty();
        }

        Map<String, String> parameters = infobox.getNamed();
        int maximumIndex = 0;
        for (String key : parameters.keySet()) {
            Matcher matcher = INDEXED_KEY.matcher(key);
            if (matcher.matches()) {
                maximumIndex = Math.max(maximumIndex, Integer.parseInt(matcher.group(2)));
            }
        }

        int defaultIndex = parseFirstInteger(parameters.get("defver"));
        if (defaultIndex <= 0) {
            defaultIndex = maximumIndex > 0 ? 1 : 0;
        }

        List<Variant> variants = new ArrayList<>();
        if (maximumIndex == 0) {
            variants.add(buildVariant(parameters, 0, true));
        } else {
            for (int index = 1; index <= maximumIndex; index++) {
                Variant variant = buildVariant(parameters, index, index == defaultIndex);
                if (!variant.isEmpty()) {
                    variants.add(variant);
                }
            }
        }
        return new ParsedMonster(variants);
    }

    private static Variant buildVariant(Map<String, String> parameters, int index, boolean defaultVariant) {
        String version = cleanDisplayValue(valueFor(parameters, "version", index));
        String bucketName = cleanDisplayValue(valueFor(parameters, "bucketname", index));
        String name = cleanDisplayValue(valueFor(parameters, "name", index));
        int combatLevel = parseFirstInteger(valueFor(parameters, "combat", index));
        Set<Integer> npcIds = parseIntegers(valueFor(parameters, "id", index));
        List<String> dropVersions = splitValues(
                cleanDisplayValue(valueFor(parameters, "dropversion", index)),
                "[,¦]"
        );

        List<String> subNames = new ArrayList<>();
        subNames.addAll(splitValues(bucketName, "¦"));
        if (!version.isEmpty()) {
            subNames.add(version);
        }

        return new Variant(
                version,
                name,
                combatLevel,
                npcIds,
                deduplicate(dropVersions),
                deduplicate(subNames),
                defaultVariant
        );
    }

    private static String valueFor(Map<String, String> parameters, String key, int index) {
        return index > 0 && parameters.containsKey(key + index)
                ? parameters.get(key + index)
                : parameters.getOrDefault(key, "");
    }

    private static String cleanDisplayValue(String value) {
        if (value == null) {
            return "";
        }

        Matcher matcher = WIKI_LINK.matcher(value.trim());
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(2) == null ? matcher.group(1) : matcher.group(2);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString().trim();
    }

    static int parseFirstInteger(String value) {
        Matcher matcher = INTEGER.matcher(value == null ? "" : value);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static Set<Integer> parseIntegers(String value) {
        Set<Integer> values = new LinkedHashSet<>();
        Matcher matcher = INTEGER.matcher(value == null ? "" : value);
        while (matcher.find()) {
            try {
                values.add(Integer.parseInt(matcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        return values;
    }

    static List<String> splitValues(String value, String delimiterRegex) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (String part : value.split(delimiterRegex)) {
            String cleaned = part.trim();
            if (!cleaned.isEmpty()) {
                values.add(cleaned);
            }
        }
        return values;
    }

    private static List<String> deduplicate(List<String> values) {
        Map<String, String> unique = new LinkedHashMap<>();
        for (String value : values) {
            unique.putIfAbsent(normalizeForComparison(value), value);
        }
        return new ArrayList<>(unique.values());
    }

    static String normalizeForComparison(String value) {
        return value == null
                ? ""
                : value.trim().replace('_', ' ').replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    @Value
    static class ParsedMonster {
        List<Variant> variants;

        ParsedMonster(List<Variant> variants) {
            this.variants = Collections.unmodifiableList(new ArrayList<>(variants));
        }

        static ParsedMonster empty() {
            return new ParsedMonster(Collections.emptyList());
        }

        Variant selectVariant(int npcId, int combatLevel, String pageSub) {
            if (npcId > 0) {
                for (Variant variant : variants) {
                    if (variant.getNpcIds().contains(npcId)) {
                        return variant;
                    }
                }
            }

            String normalizedSub = normalizeForComparison(pageSub);
            if (!normalizedSub.isEmpty() && !"default".equals(normalizedSub)) {
                for (Variant variant : variants) {
                    if (variant.matchesSubName(normalizedSub)) {
                        return variant;
                    }
                }
            }

            if (combatLevel > 0) {
                List<Variant> matchingLevel = selectVariants(combatLevel);
                if (matchingLevel.size() == 1) {
                    return matchingLevel.get(0);
                }

                if (matchingLevel.size() > 1) {
                    return null;
                }
            }

            for (Variant variant : variants) {
                if (variant.isDefaultVariant()) {
                    return variant;
                }
            }
            return variants.isEmpty() ? null : variants.get(0);
        }

        /**
         * Return every distinct Wiki variant matching an optional combat level.
         * A level-qualified search is strict: unknown or different combat levels
         * are not treated as matches.
         */
        List<Variant> selectVariants(int combatLevel) {
            if (combatLevel <= 0) {
                return new ArrayList<>(variants);
            }
            return variants.stream()
                    .filter(variant -> variant.getCombatLevel() == combatLevel)
                    .collect(Collectors.toList());
        }
    }

    @Value
    static class Variant {
        String version;
        String name;
        int combatLevel;
        Set<Integer> npcIds;
        List<String> dropVersions;
        List<String> subNames;
        boolean defaultVariant;

        Variant(
                String version,
                String name,
                int combatLevel,
                Set<Integer> npcIds,
                List<String> dropVersions,
                List<String> subNames,
                boolean defaultVariant) {
            this.version = version;
            this.name = name;
            this.combatLevel = combatLevel;
            this.npcIds = Collections.unmodifiableSet(new LinkedHashSet<>(npcIds));
            this.dropVersions = Collections.unmodifiableList(new ArrayList<>(dropVersions));
            this.subNames = Collections.unmodifiableList(new ArrayList<>(subNames));
            this.defaultVariant = defaultVariant;
        }

        private boolean isEmpty() {
            return version.isEmpty()
                    && name.isEmpty()
                    && combatLevel == 0
                    && npcIds.isEmpty()
                    && dropVersions.isEmpty()
                    && subNames.isEmpty();
        }

        private boolean matchesSubName(String normalizedSub) {
            return normalizeForComparison(version).equals(normalizedSub)
                    || subNames.stream().anyMatch(name -> normalizeForComparison(name).equals(normalizedSub));
        }
    }
}