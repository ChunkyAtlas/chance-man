package com.chanceman.ui;

import com.chanceman.drops.DropCache;
import com.chanceman.drops.NpcDropData;
import lombok.RequiredArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Provides fuzzy search over available NPC drop data. Level-qualified searches
 * preserve exact Wiki variants instead of collapsing NPCs that share a name and
 * combat level.
 */
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class NpcSearchService {
    private static final Pattern ID_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern ID_LEVEL_PATTERN = Pattern.compile("^(\\d+)\\s+(?:lvl|level)?\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_LVL_PATTERN = Pattern.compile("^(.*)\\s+(?:lvl|level)\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LVL_NAME_PATTERN = Pattern.compile("^(?:lvl|level)\\s*(\\d+)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_NUM_PATTERN = Pattern.compile("^(.*\\D)\\s+(\\d+)$");
    private static final Pattern NUM_NAME_PATTERN = Pattern.compile("^(\\d+)\\s+(\\D.*)$");

    private final DropCache dropCache;

    private static ParsedQuery parse(String q) {
        if (q == null) {
            return null;
        }

        String lower = q.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return null;
        }

        ParsedQuery pq = new ParsedQuery();
        Matcher m;

        if (ID_PATTERN.matcher(lower).matches()) {
            pq.npcId = Integer.valueOf(lower);
            return pq;
        }

        if ((m = ID_LEVEL_PATTERN.matcher(lower)).matches()) {
            pq.npcId = Integer.valueOf(m.group(1));
            pq.level = Integer.valueOf(m.group(2));
            return pq;
        }

        if ((m = NAME_LVL_PATTERN.matcher(lower)).matches()) {
            pq.name = m.group(1).trim();
            pq.level = Integer.valueOf(m.group(2));
            return pq;
        }

        if ((m = LVL_NAME_PATTERN.matcher(lower)).matches()) {
            pq.level = Integer.valueOf(m.group(1));
            pq.name = m.group(2).trim();
            return pq;
        }

        if ((m = NAME_NUM_PATTERN.matcher(lower)).matches()) {
            pq.name = m.group(1).trim();
            pq.level = Integer.valueOf(m.group(2));
            return pq;
        }

        if ((m = NUM_NAME_PATTERN.matcher(lower)).matches()) {
            pq.level = Integer.valueOf(m.group(1));
            pq.name = m.group(2).trim();
            return pq;
        }

        pq.name = lower;
        return pq;
    }

    private static List<NpcDropData> deduplicateExactResults(List<NpcDropData> results) {
        Map<String, NpcDropData> unique = new LinkedHashMap<>();
        for (NpcDropData data : results) {
            if (data == null || data.getDropTableSections() == null || data.getDropTableSections().isEmpty()) {
                continue;
            }

            String key = data.getNpcId() > 0
                    ? "id:" + data.getNpcId()
                    : "name:" + Objects.toString(data.getName(), "").toLowerCase(Locale.ROOT)
                    + "|level:" + data.getLevel();
            unique.putIfAbsent(key, data);
        }
        return new ArrayList<>(unique.values());
    }

    private static int levenshtein(String a, String b) {
        int[] cost = new int[b.length() + 1];
        for (int j = 0; j < cost.length; j++) {
            cost[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int diagonal = cost[0];
            cost[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int previous = cost[j];
                cost[j] = Math.min(
                        Math.min(cost[j] + 1, cost[j - 1] + 1),
                        diagonal + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1)
                );
                diagonal = previous;
            }
        }
        return cost[b.length()];
    }

    /**
     * Search by partial name, level, or ID. Same-name/same-level Wiki variants
     * are returned as separate NpcDropData results with their own NPC IDs.
     */
    public List<NpcDropData> search(String query) {
        ParsedQuery pq = parse(query);
        if (pq == null) {
            return Collections.emptyList();
        }

        if (pq.npcId != null && pq.name == null) {
            int lvl = pq.level != null ? pq.level : 0;
            NpcDropData data = dropCache.get(pq.npcId, "", lvl).join();
            if (data == null || data.getDropTableSections().isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.singletonList(data);
        }

        String nameFilter = pq.name;
        int levelFilter = pq.level == null ? 0 : pq.level;

        List<String> candidateNames = dropCache.searchNpcNames(nameFilter).join();
        List<NpcDropData> results = fetchAllVariants(
                candidateNames.stream().limit(10).collect(Collectors.toList()),
                levelFilter
        );

        return deduplicateExactResults(results).stream()
                .filter(data -> levelFilter <= 0 || data.getLevel() == levelFilter)
                .sorted(Comparator
                        .comparingInt((NpcDropData data) -> levenshtein(
                                data.getName().toLowerCase(Locale.ROOT),
                                nameFilter
                        ))
                        .thenComparingInt(NpcDropData::getLevel)
                        .thenComparingInt(NpcDropData::getNpcId))
                .collect(Collectors.toList());
    }

    /**
     * Persist the one search result the user actually chose. This deliberately
     * happens after selection so merely searching never populates the disk cache.
     */
    public CompletableFuture<Void> cacheSelected(NpcDropData selected) {
        return dropCache.cacheSelected(selected);
    }

    /**
     * Fetch all concrete variants for each candidate page concurrently.
     */
    private List<NpcDropData> fetchAllVariants(List<String> names, int level) {
        List<CompletableFuture<List<NpcDropData>>> futures = names.stream()
                .map(name -> dropCache.searchNpcVariants(name, level))
                .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return futures.stream()
                .flatMap(future -> future.join().stream())
                .collect(Collectors.toList());
    }

    private static class ParsedQuery {
        Integer npcId;
        Integer level;
        String name;
    }
}