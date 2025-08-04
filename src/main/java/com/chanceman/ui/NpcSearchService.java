package com.chanceman.ui;

import com.chanceman.drops.DropCache;
import com.chanceman.drops.NpcDropData;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * Provides fuzzy search over available NPC drop data. The cache is consulted
 * first and any misses fall back to a wiki lookup. Results without drop tables
 * are discarded and lookups for multiple candidates are performed in parallel
 * to keep searches snappy.
 */
@Singleton
public class NpcSearchService
{
    private static final Pattern ID_LEVEL_PATTERN = Pattern.compile("^(\\d+)\\s+(?:lvl|level)?\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_LVL_PATTERN = Pattern.compile("^(.*)\\s+(?:lvl|level)\\s*(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LVL_NAME_PATTERN = Pattern.compile("^(?:lvl|level)\\s*(\\d+)\\s+(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME_NUM_PATTERN = Pattern.compile("^(.*\\D)\\s+(\\d+)$");
    private static final Pattern NUM_NAME_PATTERN = Pattern.compile("^(\\d+)\\s+(\\D.*)$");

    private final DropCache dropCache;

    @Inject
    public NpcSearchService(DropCache dropCache)
    {
        this.dropCache = dropCache;
    }

    private static class ParsedQuery
    {
        Integer npcId;
        Integer level;
        String  name;
    }

    private static ParsedQuery parse(String q)
    {
        if (q == null) return null;
        String lower = q.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) return null;

        ParsedQuery pq = new ParsedQuery();
        Matcher m;

        // pure ID
        if (lower.matches("\\d+"))
        {
            pq.npcId = Integer.valueOf(lower);
            return pq;
        }
        // ID + level
        if ((m = ID_LEVEL_PATTERN.matcher(lower)).matches())
        {
            pq.npcId = Integer.valueOf(m.group(1));
            pq.level = Integer.valueOf(m.group(2));
            return pq;
        }
        // name + level
        if ((m = NAME_LVL_PATTERN.matcher(lower)).matches())
        {
            pq.name  = m.group(1).trim();
            pq.level = Integer.valueOf(m.group(2));
            return pq;
        }
        // level + name
        if ((m = LVL_NAME_PATTERN.matcher(lower)).matches())
        {
            pq.level = Integer.valueOf(m.group(1));
            pq.name  = m.group(2).trim();
            return pq;
        }
        // trailing number = level
        if ((m = NAME_NUM_PATTERN.matcher(lower)).matches())
        {
            pq.name  = m.group(1).trim();
            pq.level = Integer.valueOf(m.group(2));
            return pq;
        }
        // leading number = level
        if ((m = NUM_NAME_PATTERN.matcher(lower)).matches())
        {
            pq.level = Integer.valueOf(m.group(1));
            pq.name  = m.group(2).trim();
            return pq;
        }

        // fallback to pure name
        pq.name = lower;
        return pq;
    }

    /**
     * Search by partial name, level, or ID. Results are limited and ordered by
     * Levenshtein distance when appropriate.
     */
    public List<NpcDropData> search(String query)
    {
        ParsedQuery pq = parse(query);
        if (pq == null)
        {
            return Collections.emptyList();
        }

        // 1) name only → fetch all candidates by name
        if (pq.npcId == null && pq.level == null && pq.name != null)
        {
            List<String> names = dropCache.searchNpcNames(pq.name).join();
            List<NpcDropData> fetched = fetchAll(names.stream().limit(10).collect(Collectors.toList()), 0);
            return fetched.stream()
                    .sorted(Comparator.comparingInt(d ->
                            levenshtein(d.getName().toLowerCase(Locale.ROOT), pq.name)))
                    .collect(Collectors.toList());
        }

        // 2) ID only → fetch by ID
        if (pq.npcId != null && pq.name == null)
        {
            int lvl = (pq.level != null ? pq.level : 0);
            NpcDropData d = dropCache.get(pq.npcId, "", lvl).join();
            if (d == null || d.getDropTableSections().isEmpty())
            {
                return Collections.emptyList();
            }
            return Collections.singletonList(d);
        }

        // 3) mixed or partial → fuzzy search
        String nameFilter = (pq.name != null ? pq.name : "");
        int lvlFilter = (pq.level != null ? pq.level : -1);

        List<String> candidates = dropCache.searchNpcNames(nameFilter).join();
        List<NpcDropData> all = fetchAll(candidates.stream().limit(10).collect(Collectors.toList()),
                lvlFilter > -1 ? lvlFilter : 0);

        // if ID also provided, filter it
        if (pq.npcId != null)
        {
            all = all.stream()
                    .filter(d -> d.getNpcId() == pq.npcId)
                    .collect(Collectors.toList());
        }

        // filter by level + sort by name distance
        final int lvl = lvlFilter;
        return all.stream()
                .filter(d -> lvl < 0 || d.getLevel() == lvl)
                .sorted(Comparator.comparingInt(d ->
                        levenshtein(d.getName().toLowerCase(Locale.ROOT), nameFilter)))
                .collect(Collectors.toList());
    }

    /**
     * Fetch drop data for a list of names concurrently.
     */
    private List<NpcDropData> fetchAll(List<String> names, int level)
    {
        List<CompletableFuture<NpcDropData>> futures = names.stream()
                .map(n -> dropCache.get(0, n, level))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .filter(d -> !d.getDropTableSections().isEmpty())
                .collect(Collectors.toList());
    }

    // simple DP Levenshtein
    private static int levenshtein(String a, String b)
    {
        int[][] dp = new int[a.length()+1][b.length()+1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = Math.min(
                        Math.min(dp[i-1][j] + 1, dp[i][j-1] + 1),
                        dp[i-1][j-1] + (a.charAt(i-1)==b.charAt(j-1) ? 0 : 1)
                );
        return dp[a.length()][b.length()];
    }
}