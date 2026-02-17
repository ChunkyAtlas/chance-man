package com.chanceman.drops;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/**
 * Retrieves NPC drop information from the wiki and
 * resolves item and NPC IDs.
 */
@Slf4j
@Singleton
public class DropFetcher
{
    private static final String USER_AGENT = "RuneLite-ChanceMan/3.0.4";
    private final OkHttpClient httpClient;
    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private ExecutorService fetchExecutor;

    @Inject
    public DropFetcher(OkHttpClient httpClient, ItemManager itemManager, ClientThread clientThread)
    {
        this.httpClient = httpClient;
        this.itemManager  = itemManager;
        this.clientThread = clientThread;
    }

    /**
     * Asynchronously fetch an NPC's drop table from the wiki.
     * 1) Download + parse document (BG thread)
     * 2) Resolve item IDs on client thread using ItemManager.search (canonicalized)
     */
    public CompletableFuture<NpcDropData> fetch(int npcId, String name, int level)
    {
        return CompletableFuture.supplyAsync(() -> {
            String url = buildWikiUrl(npcId, name);
            String html = fetchHtml(url);
            Document doc = Jsoup.parse(html);

            String actualName = name;
            Element heading = doc.selectFirst("h1#firstHeading");
            if (heading != null) {
                actualName = heading.text();
            }

            int resolvedLevel = level > 0 ? level : parseCombatLevel(doc);
            int actualId = resolveNpcId(doc);
            List<DropTableSection> sections = parseSections(doc);
            if (sections.isEmpty()) {
                return null; // skip NPCs without drop tables
            }
            return new NpcDropData(actualId, actualName, resolvedLevel, sections);
        }, fetchExecutor).thenCompose(data -> {
            if (data == null) {
                return CompletableFuture.completedFuture(null);
            }

            CompletableFuture<NpcDropData> resolved = new CompletableFuture<>();
            clientThread.invoke(() -> {
                for (DropTableSection sec : data.getDropTableSections()) {
                    List<DropItem> items = sec.getItems();
                    for (int i = 0; i < items.size(); i++) {
                        DropItem d = items.get(i);
                        d.setItemId(resolveItemId(d.getName()));
                    }
                }
                resolved.complete(data);
            });
            return resolved;
        });
    }

    /** Resolve an item name to an ID using ItemManager.search only (canonicalized). */
    private int resolveItemId(String itemName)
    {
        if (itemName == null || itemName.isEmpty()) {
            return 0;
        }
        String lower = itemName.trim().toLowerCase(Locale.ROOT);
        if ("nothing".equals(lower) || "unknown".equals(lower)) {
            return 0;
        }

        try {
            List<ItemPrice> results = itemManager.search(itemName);
            for (int j = 0; j < results.size(); j++) {
                int id = results.get(j).getId();
                ItemComposition comp = itemManager.getItemComposition(id);
                if (comp != null && comp.getName() != null && comp.getName().equalsIgnoreCase(itemName)) {
                    return itemManager.canonicalize(id);
                }
            }
        } catch (Exception ex) {
            // ignore; fall through
        }

        return 0;
    }

    /** Extract drop table sections (skips Nothing rows). */
    private List<DropTableSection> parseSections(Document doc)
    {
        Elements tables = doc.select("table.item-drops");
        List<DropTableSection> sections = new ArrayList<>();

        for (Element table : tables)
        {
            Map<String, Integer> col = buildColumnIndexMap(table);

            Integer itemCol = col.get("item");
            Integer rarityCol = col.get("rarity");
            if (itemCol == null || rarityCol == null)
            {
                continue; // table not understood
            }

            String header = findSectionHeader(table);

            List<DropItem> items = new ArrayList<>();
            Elements rows = table.select("tbody > tr");

            for (Element row : rows)
            {
                // Skip header-like rows inside tbody
                if (!row.select("th").isEmpty())
                {
                    continue;
                }

                Elements tds = row.select("td");
                if (itemCol >= tds.size())
                {
                    continue;
                }

                Element itemTd = tds.get(itemCol);

                String name = extractItemName(itemTd);
                if (name.isEmpty() || name.equalsIgnoreCase("nothing"))
                {
                    continue;
                }

                String rarity = "";
                if (rarityCol < tds.size())
                {
                    rarity = extractRarity(tds.get(rarityCol));
                }
                else
                {
                    // Fallback: locate a cell containing the new rarity spans
                    Element rarityTd = row.selectFirst("td:has(span[data-drop-fraction]), td:has(span[data-drop-oneover])");
                    if (rarityTd != null)
                    {
                        rarity = extractRarity(rarityTd);
                    }
                }

                items.add(new DropItem(0, name, rarity));
            }

            if (!items.isEmpty())
            {
                sections.add(new DropTableSection(header, items));
            }
        }

        return sections;
    }

    /** Find the nearest section header preceding the table (supports mw-heading wrappers). */
    private String findSectionHeader(Element table)
    {
        Element prev = table.previousElementSibling();
        while (prev != null)
        {
            if (prev.is("h2,h3,h4"))
            {
                String txt = prev.text().trim();
                return txt.isEmpty() ? "Drops" : txt;
            }

            if (prev.hasClass("mw-heading"))
            {
                Element h = prev.selectFirst("h2,h3,h4");
                if (h != null)
                {
                    String txt = h.text().trim();
                    return txt.isEmpty() ? "Drops" : txt;
                }
            }

            prev = prev.previousElementSibling();
        }
        return "Drops";
    }

    /** Build a normalized map of column name -> index from the table header row. */
    private Map<String, Integer> buildColumnIndexMap(Element table)
    {
        Map<String, Integer> map = new HashMap<>();

        Element headerRow = table.selectFirst("tr:has(th)");
        if (headerRow == null)
        {
            return map;
        }

        Elements ths = headerRow.select("th");
        for (int i = 0; i < ths.size(); i++)
        {
            Element th = ths.get(i);

            // OSRS wiki uses class "item-col" on the Item column header
            if (th.hasClass("item-col"))
            {
                map.put("item", i);
            }

            String key = normalizeHeader(th.text());
            if (!key.isEmpty())
            {
                map.put(key, i);
            }
        }

        return map;
    }

    private String normalizeHeader(String s)
    {
        if (s == null) return "";
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return "";

        if (t.contains("item")) return "item";
        if (t.contains("rarity")) return "rarity";
        return "";
    }

    /** Extract item name from the item cell. */
    private String extractItemName(Element itemTd)
    {
        if (itemTd == null) return "";

        Element a = itemTd.selectFirst("a.itemlink[title], a[title]");
        if (a != null)
        {
            String title = a.attr("title");
            if (title != null && !title.trim().isEmpty())
            {
                return title.trim();
            }
        }

        return itemTd.text().replace("(m)", "").trim();
    }

    /** Extract rarity from data-drop-* spans. */
    private String extractRarity(Element rarityTd)
    {
        if (rarityTd == null) return "";

        Elements spans = rarityTd.select("span[data-drop-fraction], span[data-drop-oneover]");
        if (!spans.isEmpty())
        {
            List<String> parts = new ArrayList<>();
            for (Element sp : spans)
            {
                String v = sp.hasAttr("data-drop-fraction") ? sp.attr("data-drop-fraction") : "";
                if (v == null || v.isEmpty())
                {
                    v = sp.hasAttr("data-drop-oneover") ? sp.attr("data-drop-oneover") : "";
                }

                String txt = (v != null && !v.isEmpty()) ? v : sp.text();
                txt = txt.replace(",", "").trim();
                if (!txt.isEmpty())
                {
                    parts.add(txt);
                }
            }

            if (parts.isEmpty())
            {
                return "";
            }
            if (parts.size() == 1)
            {
                return parts.get(0);
            }
            if (parts.size() == 2)
            {
                return parts.get(0) + "–" + parts.get(1);
            }
            return String.join("; ", parts);
        }

        // Fallback
        String own = rarityTd.ownText();
        if (own != null && !own.trim().isEmpty())
        {
            return own.trim();
        }
        return rarityTd.text().trim();
    }

    /** Attempt to parse the combat level from the NPC infobox. */
    private int parseCombatLevel(Document doc)
    {
        Element infobox = doc.selectFirst("table.infobox");
        if (infobox == null)
        {
            return 0;
        }
        Elements rows = infobox.select("tr");
        for (Element row : rows)
        {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");
            if (th != null && td != null) {
                String thText = th.text();
                if (thText != null && thText.toLowerCase(Locale.ROOT).contains("combat level")) {
                    String txt = td.text();
                    String[] parts = txt.split("[^0-9]+");
                    for (String part : parts) {
                        if (part != null && part.length() > 0) {
                            try {
                                return Integer.parseInt(part);
                            } catch (NumberFormatException nfe) {
                                log.warn("Failed to parse combat level: {}", txt);
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    /** Resolve the canonical wiki page ID for the provided document. */
    private int resolveNpcId(Document doc)
    {
        Element link = doc.selectFirst("link[rel=canonical]");
        if (link == null)
        {
            return 0;
        }

        String href = link.attr("href");
        if (href == null || href.isEmpty())
        {
            return 0;
        }

        // Strip query / fragment just in case the canonical ever includes them.
        int q = href.indexOf('?');
        if (q >= 0) href = href.substring(0, q);
        int h = href.indexOf('#');
        if (h >= 0) href = href.substring(0, h);

        String title = href.substring(href.lastIndexOf('/') + 1);
        title = URLDecoder.decode(title, StandardCharsets.UTF_8);
        title = title.replace(' ', '_');
        String apiUrl = "https://oldschool.runescape.wiki/api.php?action=query&format=json&prop=info&titles="
                + URLEncoder.encode(title, StandardCharsets.UTF_8);

        Request req = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                log.warn("Failed to resolve NPC ID for {}: HTTP {}", title, res.code());
                return 0;
            }

            String body = res.body().string();
            JsonElement root = new JsonParser().parse(body);
            JsonElement pages = root.getAsJsonObject()
                    .getAsJsonObject("query")
                    .getAsJsonObject("pages");

            for (Map.Entry<String, JsonElement> entry : pages.getAsJsonObject().entrySet())
            {
                JsonElement page = entry.getValue();
                if (page.getAsJsonObject().has("pageid"))
                {
                    return page.getAsJsonObject().get("pageid").getAsInt();
                }
            }

            log.warn("No page ID found for title {}", title);
        }
        catch (IOException ex)
        {
            log.warn("Error resolving NPC ID for {}", title, ex);
        }
        return 0;
    }

    /** Query the wiki's search API for NPC names matching the provided text. */
    public List<String> searchNpcNames(String query)
    {
        String url = "https://oldschool.runescape.wiki/api.php?action=opensearch&format=json&limit=20&namespace=0&search="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                throw new IOException("HTTP " + res.code());
            }
            String body = res.body().string();
            JsonArray arr = new JsonParser().parse(body).getAsJsonArray();
            JsonArray titles = arr.get(1).getAsJsonArray();
            List<String> names = new ArrayList<>();
            for (int i = 0; i < titles.size(); i++) {
                names.add(titles.get(i).getAsString());
            }
            return names;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    private String buildWikiUrl(int npcId, String name)
    {
        String fallback = URLEncoder.encode(name.replace(' ', '_'), StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc");

        if (npcId > 0)
        {
            url.append("&id=").append(npcId);
        }

        if (!fallback.isEmpty())
        {
            url.append("&name=").append(fallback);
        }

        url.append("#Drops");
        return url.toString();
    }

    private String fetchHtml(String url)
    {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build();
        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful()) throw new IOException("HTTP " + res.code());
            return res.body().string();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /** Creates the fetch executor if it is missing or has been shut down. */
    public void startUp()
    {
        if (fetchExecutor == null || fetchExecutor.isShutdown() || fetchExecutor.isTerminated())
        {
            fetchExecutor = Executors.newFixedThreadPool(
                    4,
                    new ThreadFactoryBuilder().setNameFormat("dropfetch-%d").build()
            );
        }
    }

    /** Shut down the executor service. */
    public void shutdown()
    {
        if (fetchExecutor != null)
        {
            fetchExecutor.shutdownNow();
            fetchExecutor = null;
        }
    }
}